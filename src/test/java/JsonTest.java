import util.Json;

/**
 * Json 编码方法测试用例
 * 验证 FastJSON Ghost Unicode 编码的正确性
 */
public class JsonTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========== Json 编码方法测试 ==========\n");

        testFastjsonGhostEncodeWithJson();
        testFastjsonGhostEncodeWithPlainText();
        testJsonStructurePreserved();
        testEncodeStringValuesToUnicode();

        System.out.println("\n========== 测试结果 ==========");
        System.out.println("通过: " + passed + ", 失败: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ──────────── 1. JSON 输入: 字符串字面量应被编码，结构保留 ────────────

    static void testFastjsonGhostEncodeWithJson() {
        System.out.println("[Test] FastJSON Ghost 编码 - JSON 输入");

        String input = "{\"@type\":\"java.awt.Rectangle\"}";
        String encoded = Json.fastjsonGhostEncode(input);

        // JSON 结构字符应保留
        assertTrue("应以 { 开头", encoded.startsWith("{"));
        assertTrue("应以 } 结尾", encoded.endsWith("}"));
        assertTrue("应含双引号", encoded.contains("\""));
        assertTrue("应含冒号", encoded.contains(":"));

        // 字符串内容应被 unicode 转义编码
        assertTrue("应含 \\u 编码", encoded.contains("\\u"));

        // 不应含原始字符串 @type （已被编码）
        assertTrue("不应含原始 @type", !encoded.contains("@type"));

        System.out.println("  编码结果: " + encoded);
        System.out.println();
    }

    // ──────────── 2. 非 JSON 输入: 整体编码 ────────────

    static void testFastjsonGhostEncodeWithPlainText() {
        System.out.println("[Test] FastJSON Ghost 编码 - 普通文本输入");

        String input = "@type";
        String encoded = Json.fastjsonGhostEncode(input);

        // 应该全部是 unicode 转义格式
        assertTrue("应含 \\u 编码", encoded.contains("\\u"));
        // 不应含原始字符
        assertTrue("不应含原始 @", !encoded.contains("@"));

        System.out.println("  编码结果: " + encoded);
        System.out.println();
    }

    // ──────────── 3. JSON 结构完整性验证 ────────────

    static void testJsonStructurePreserved() {
        System.out.println("[Test] JSON 结构完整性");

        String input = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";
        String encoded = Json.fastjsonGhostEncode(input);

        // 数字和布尔值不应被编码（它们不在双引号内）
        assertTrue("应含数字 123", encoded.contains("123"));
        assertTrue("应含布尔 true", encoded.contains("true"));
        // 逗号应保留
        assertTrue("应含逗号", encoded.contains(","));

        System.out.println("  编码结果: " + encoded);
        System.out.println();
    }

    // ──────────── 4. 标准 Unicode 编码验证 ────────────

    static void testEncodeStringValuesToUnicode() {
        System.out.println("[Test] 标准 Unicode 编码 (encodeStringValuesToUnicode)");

        String input = "{\"a\":\"b\"}";
        String encoded = Json.encodeStringValuesToUnicode(input);

        // 应包含 unicode 转义编码
        assertTrue("应含 \\u 编码", encoded.contains("\\u"));
        // 结构应保留
        assertTrue("应以 { 开头", encoded.startsWith("{"));
        assertTrue("应含冒号", encoded.contains(":"));

        System.out.println("  编码结果: " + encoded);
        System.out.println();
    }

    // ══════════════════════ 辅助方法 ══════════════════════

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
