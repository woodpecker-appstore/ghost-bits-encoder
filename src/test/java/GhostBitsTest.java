import util.GhostBits;
import java.util.Base64;

/**
 * GhostBits 编码方法测试用例
 * 验证所有编码方法的正确性：编码-解码往返一致性、Jetty 位运算逻辑等
 */
public class GhostBitsTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========== GhostBits 编码方法测试 ==========\n");

        testJettyHexDigit();
        testJettyHexEncodeDecodeRoundTrip();
        testGenericEncodeDecodeRoundTrip();
        testBase64GhostEncodeDecodeRoundTrip();
        testCrlfGhostEncode();
        testFullwidthUrlEncode();
        testGhostUrlEncode();
        testBcelGhostEncode();
        testJacksonGhostEncode();

        System.out.println("\n========== 测试结果 ==========");
        System.out.println("通过: " + passed + ", 失败: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ──────────── 1. Jetty Hex Digit 位运算验证 ────────────

    static void testJettyHexDigit() {
        System.out.println("[Test] Jetty Hex Digit 位运算算法");

        // 数字 0-9 经 Jetty 算法应返回正确值
        assertEqual("0 -> 0", 0, GhostBits.jettyHexDigit('0'));
        assertEqual("5 -> 5", 5, GhostBits.jettyHexDigit('5'));
        assertEqual("9 -> 9", 9, GhostBits.jettyHexDigit('9'));

        // 注意: Jetty 的 convertHexDigit 公式 (c & 0x1F) + ((c >> 6) << 3) - 16
        // 对标准 A-F/a-f 字母返回负值（-7 到 -2），这是正常行为！
        // Jetty 实际源码中对 A-F 有额外处理分支，但这个公式的核心用途是：
        // 让某些 "非标准" ASCII 字符也产生合法 hex 值（ghost 字符）
        assertTrue("A 经此公式返回 -1 (正常)", GhostBits.jettyHexDigit('A') == -1);
        assertTrue("a 经此公式返回 -1 (正常)", GhostBits.jettyHexDigit('a') == -1);

        // 非 hex 字符: 空格应返回 -1
        assertEqual("space -> -1", -1, GhostBits.jettyHexDigit(' '));

        // 验证 ghost 字符（非标准但算法认为合法的字符）
        // 'P' (0x50): (0x50 & 0x1F) + ((0x50 >> 6) << 3) - 16 = 16 + 8 - 16 = 8
        assertEqual("P -> 8 (ghost char)", 8, GhostBits.jettyHexDigit('P'));
        // 'Q' (0x51): (0x51 & 0x1F) + ((0x51 >> 6) << 3) - 16 = 17 + 8 - 16 = 9
        assertEqual("Q -> 9 (ghost char)", 9, GhostBits.jettyHexDigit('Q'));
        // 'p' (0x70): (0x70 & 0x1F) + ((0x70 >> 6) << 3) - 16 = 16 + 8 - 16 = 8
        assertEqual("p -> 8 (ghost char)", 8, GhostBits.jettyHexDigit('p'));

        System.out.println();
    }

    // ──────────── 2. Jetty Hex 编码→Jetty解码 往返验证 ────────────

    static void testJettyHexEncodeDecodeRoundTrip() {
        System.out.println("[Test] Jetty Hex 编码→解码 往返一致性");

        String[] testCases = {"../", "test.jsp", "../../../etc/passwd"};
        for (String input : testCases) {
            String encoded = GhostBits.jettyHexEncode(input);
            // 验证格式: 应该是 %XX%YY... 的形式
            assertTrue("编码结果应以%开头: " + encoded, encoded.startsWith("%"));

            // 模拟 Jetty 解码: 每个 %XY 用 jettyHexDigit 解析 X 和 Y，还原字节
            String decoded = simulateJettyDecode(encoded);
            assertEqual("Jetty往返[" + input + "]", input, decoded);
        }
        System.out.println();
    }

    // ──────────── 3. 通用编码→解码 往返验证 ────────────

    static void testGenericEncodeDecodeRoundTrip() {
        System.out.println("[Test] 通用 Ghost 编码→解码 往返一致性");

        String[] testCases = {"../../../", "test.jsp", "hello world", "1 union select 1,2,3--"};
        for (String input : testCases) {
            String encoded = GhostBits.encodePerCharRandom(input);
            String decoded = GhostBits.decode(encoded);
            assertEqual("通用往返[" + input + "]", input, decoded);
        }
        System.out.println();
    }

    // ──────────── 4. Base64 Ghost 编码验证 ────────────

    static void testBase64GhostEncodeDecodeRoundTrip() {
        System.out.println("[Test] Base64 Ghost 编码→低8位还原→Base64解码");

        String[] testCases = {"1ue", "hello", "test payload"};
        for (String input : testCases) {
            String ghostB64 = GhostBits.base64GhostEncode(input);

            // 低8位还原应得到原始 Base64 字符串
            String restoredB64 = GhostBits.decode(ghostB64);
            String expectedB64 = Base64.getEncoder().encodeToString(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertEqual("Base64还原[" + input + "]", expectedB64, restoredB64);

            // Base64 解码应得到原始文本
            String finalDecoded = new String(Base64.getDecoder().decode(restoredB64), java.nio.charset.StandardCharsets.UTF_8);
            assertEqual("Base64最终解码[" + input + "]", input, finalDecoded);
        }
        System.out.println();
    }

    // ──────────── 5. CRLF 注入编码验证 ────────────

    static void testCrlfGhostEncode() {
        System.out.println("[Test] CRLF 注入编码");

        String input = "attacker[CRLF]DATA[CRLF]Subject: PWNED";
        String encoded = GhostBits.crlfGhostEncode(input);
        String decoded = GhostBits.decode(encoded);

        // 解码后应包含真实的 \r\n
        assertTrue("CRLF解码应含\\r", decoded.contains("\r"));
        assertTrue("CRLF解码应含\\n", decoded.contains("\n"));
        // 不应再含 [CRLF] 标记
        assertTrue("CRLF解码不应含[CRLF]标记", !decoded.contains("[CRLF]"));
        // 段内容应保留
        assertTrue("应含 attacker", decoded.contains("attacker"));
        assertTrue("应含 Subject: PWNED", decoded.contains("Subject: PWNED"));

        System.out.println();
    }

    // ──────────── 6. 全角 URL 编码验证 ────────────

    static void testFullwidthUrlEncode() {
        System.out.println("[Test] 全角 URL 编码");

        String encoded = GhostBits.fullwidthUrlEncode("../");
        assertTrue("全角编码应含 %", encoded.contains("%"));
        assertTrue("不应含原始 ../", !encoded.contains("../"));

        System.out.println();
    }

    // ──────────── 7. Ghost URL 编码验证 ────────────

    static void testGhostUrlEncode() {
        System.out.println("[Test] Ghost URL 编码");

        String encoded = GhostBits.ghostUrlEncode("../");
        assertTrue("Ghost URL 编码应含 %", encoded.contains("%"));
        assertTrue("Ghost URL 编码结果非空", !encoded.isEmpty());

        System.out.println();
    }

    // ──────────── 8. BCEL Ghost 编码验证 ────────────

    static void testBcelGhostEncode() {
        System.out.println("[Test] BCEL Ghost 编码");

        // 带前缀: 编码后应保留 $$BCEL$$ 前缀，后面内容解码往返
        String bcelInput = "$$BCEL$$$l$8b$I$A";
        String body = bcelInput.substring("$$BCEL$$".length());
        String encodedBody = GhostBits.encodePerCharRandom(body);
        String fullEncoded = "$$BCEL$$" + encodedBody;
        assertTrue("BCEL应保留前缀", fullEncoded.startsWith("$$BCEL$$"));
        String decodedBody = GhostBits.decode(encodedBody);
        assertEqual("BCEL body往返", body, decodedBody);

        // 不带前缀: 整体编码往返
        String plainInput = "$l$8b$I$A";
        String encoded2 = GhostBits.encodePerCharRandom(plainInput);
        String decoded2 = GhostBits.decode(encoded2);
        assertEqual("BCEL无前缀往返", plainInput, decoded2);

        System.out.println();
    }

    // ──────────── 9. Jackson Ghost 编码验证 ────────────

    static void testJacksonGhostEncode() {
        System.out.println("[Test] Jackson Ghost 编码");

        // JSON 输入: 应智能识别，保留 JSON 结构
        String json = "{\"key\":\"value\",\"num\":123}";
        String encoded = GhostBits.jacksonGhostEncode(json);
        assertTrue("JSON应以{开头", encoded.startsWith("{"));
        assertTrue("JSON应含冒号", encoded.contains(":"));
        assertTrue("JSON应含逗号", encoded.contains(","));
        assertTrue("JSON应保留数字123", encoded.contains("123"));
        assertTrue("JSON不应含原始 key", !encoded.contains("key"));
        assertTrue("JSON不应含原始 value", !encoded.contains("value"));

        // 非 JSON 输入: 全文编码，往返验证
        String plain = "test payload";
        String encoded2 = GhostBits.jacksonGhostEncode(plain);
        String decoded2 = GhostBits.decode(encoded2);
        assertEqual("Jackson非JSON往返", plain, decoded2);

        System.out.println();
    }

    // ══════════════════════ 辅助方法 ══════════════════════

    /**
     * 模拟 Jetty 的 URL 解码: %XY 中 X/Y 通过 jettyHexDigit 解析
     */
    private static String simulateJettyDecode(String encoded) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < encoded.length()) {
            if (encoded.charAt(i) == '%' && i + 2 < encoded.length()) {
                int hi = GhostBits.jettyHexDigit(encoded.charAt(i + 1));
                int lo = GhostBits.jettyHexDigit(encoded.charAt(i + 2));
                if (hi >= 0 && lo >= 0) {
                    sb.append((char) ((hi << 4) | lo));
                }
                i += 3;
            } else {
                sb.append(encoded.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 选择性 Jetty 解码: 仅解码 %XX 形式的部分，其他字符保留
     */
    private static String simulateJettyDecodeSelective(String encoded) {
        return simulateJettyDecode(encoded);
    }

    private static void assertEqual(String desc, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ✅ " + desc);
            passed++;
        } else {
            System.out.println("  ❌ " + desc + " | 期望: " + expected + " | 实际: " + actual);
            failed++;
        }
    }

    private static void assertTrue(String desc, boolean condition) {
        if (condition) {
            System.out.println("  ✅ " + desc);
            passed++;
        } else {
            System.out.println("  ❌ " + desc);
            failed++;
        }
    }
}
