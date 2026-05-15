package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Json {
    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    /**
     * 将 JSON 字符串中所有字符串字段（键名和值）的内容编码为 Unicode 转义序列。
     * JSON 结构字符（引号、冒号、逗号、括号等）保持不变。
     * 示例: @ => {backslash}u0040
     */
    public static String encodeStringValuesToUnicode(String json) {
        if (json == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(json.length() * 2);
        // 逐字符解析，识别 JSON 字符串段并对其内容进行 Unicode 编码
        int i = 0;
        int len = json.length();
        while (i < len) {
            char c = json.charAt(i);
            if (c == '"') {
                // 进入字符串，收集原始内容（处理转义）
                result.append('"');
                i++;
                StringBuilder strContent = new StringBuilder();
                while (i < len) {
                    char sc = json.charAt(i);
                    if (sc == '\\' && i + 1 < len) {
                        // 保留转义序列原样
                        strContent.append(sc);
                        strContent.append(json.charAt(i + 1));
                        i += 2;
                    } else if (sc == '"') {
                        break;
                    } else {
                        strContent.append(sc);
                        i++;
                    }
                }
                // 对字符串内容每个字符进行 Unicode 编码
                for (int j = 0; j < strContent.length(); j++) {
                    char ch = strContent.charAt(j);
                    if (ch == '\\' && j + 1 < strContent.length()) {
                        // 已有转义序列，保留原样
                        result.append(ch);
                        result.append(strContent.charAt(j + 1));
                        j++;
                    } else {
                        result.append(String.format("\\u%04x", (int) ch));
                    }
                }
                result.append('"');
                i++; // 跳过结束引号
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    public static String FjGhostBits(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = UNICODE_PATTERN.matcher(input);

        while (matcher.find()) {
            String hexCode = matcher.group(1);  // 提取 hex 码
            String decoded = "\\u" + GhostBits.unicodeHex(hexCode, GhostBits.hexChars);

            // 对解码结果进行转义，防止破坏正则替换
            String escaped = Matcher.quoteReplacement(decoded);
            matcher.appendReplacement(result, escaped);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    // ====================================================================
    //  FastJSON Ghost Bits (反斜杠u 多文种数字混淆)
    //  原理: FastJSON 解析 反斜杠u+4位hex 时调用 Character.digit(ch, 16)
    //  该方法对多种 Unicode 数字字符返回等价数值
    //  攻击者用这些"等价数字"替换 hex 数字
    //  WAF 看到的是乱码，FastJSON 解析出的是合法字符
    //  抗性: [网关安全: 绝对安全] [规范化免疫: 绝对免疫] [编码链: 绝对免疫]
    // ====================================================================

    /**
     * FastJSON Ghost 编码入口:
     * 1. 若输入看起来像 JSON（以 左花括号 或 左方括号 开头且含双引号），
     *    则基于双引号边界扫描，仅对 字符串字面量 内容做多文种数字混淆，
     *    保留 JSON 结构及 fastjson 接受的非标准语法。
     * 2. 若非 JSON，则对整段字符串编码。
     *
     * @param input 待编码文本（可以是 JSON，也可以是普通文本）
     * @return FastJSON Ghost 编码结果
     */
    public static String fastjsonGhostEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        if (GhostBits.looksLikeJson(input)) {
            return transformJsonStrings(input);
        } else {
            return encodeToFjGhostUnicode(input);
        }
    }

    /**
     * 基于双引号边界扫描 JSON 文本，仅对字符串字面量做编码
     * 保留 JSON 结构字符、非标准语法（不带引号的key、注释、尾随逗号等）
     */
    private static String transformJsonStrings(String text) {
        StringBuilder out = new StringBuilder(text.length() * 2);
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c != '"') {
                out.append(c);
                i++;
                continue;
            }
            // 遇到起始双引号，扫描到匹配的结束双引号
            int j = i + 1;
            while (j < n) {
                char cj = text.charAt(j);
                if (cj == '\\' && j + 1 < n) {
                    j += 2;
                    continue;
                }
                if (cj == '"') {
                    break;
                }
                j++;
            }
            if (j >= n) {
                out.append(text, i, n);
                break;
            }
            // 提取字符串字面量内容（不含外层双引号）
            String innerRaw = text.substring(i + 1, j);
            // 解码 JSON 转义 -> 真实字符串 -> 编码为 Ghost Unicode
            String decoded = decodeJsonStringLiteral(innerRaw);
            String encoded = encodeToFjGhostUnicode(decoded);
            out.append('"').append(encoded).append('"');
            i = j + 1;
        }
        return out.toString();
    }

    /**
     * 将 JSON 字符串字面量内部内容（不含外层双引号）解码为真实字符串
     * 支持标准 JSON 转义
     */
    private static String decodeJsonStringLiteral(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0;
        int n = raw.length();
        while (i < n) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char nxt = raw.charAt(i + 1);
                switch (nxt) {
                    case '"': out.append('"'); i += 2; continue;
                    case '\\': out.append('\\'); i += 2; continue;
                    case '/': out.append('/'); i += 2; continue;
                    case 'b': out.append('\b'); i += 2; continue;
                    case 'f': out.append('\f'); i += 2; continue;
                    case 'n': out.append('\n'); i += 2; continue;
                    case 'r': out.append('\r'); i += 2; continue;
                    case 't': out.append('\t'); i += 2; continue;
                    case '\'': out.append('\''); i += 2; continue;  // fastjson 容忍
                    case 'u':
                        if (i + 5 < n) {
                            try {
                                int cp = Integer.parseInt(raw.substring(i + 2, i + 6), 16);
                                out.append((char) cp);
                                i += 6;
                                continue;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        // fall through
                    default:
                        out.append(nxt);
                        i += 2;
                        continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * 将字符串编码为 FastJSON Ghost Unicode 序列:
     * 每个字符编码为反斜杠u+4位hex，其中 hex 数字可能被替换为多文种等价数字
     */
    private static String encodeToFjGhostUnicode(String text) {
        StringBuilder sb = new StringBuilder(text.length() * 6);
        java.util.Random rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (cp <= 0xFFFF) {
                // BMP 字符
                String hex = String.format("%04X", cp);
                sb.append("\\u");
                for (int h = 0; h < hex.length(); h++) {
                    sb.append(pickGhostHexDigit(hex.charAt(h), rnd));
                }
            } else {
                // 增补平面字符: 代理对
                cp -= 0x10000;
                int highSurr = 0xD800 + (cp >> 10);
                int lowSurr = 0xDC00 + (cp & 0x3FF);
                for (int surr : new int[]{highSurr, lowSurr}) {
                    String hex = String.format("%04X", surr);
                    sb.append("\\u");
                    for (int h = 0; h < hex.length(); h++) {
                        sb.append(pickGhostHexDigit(hex.charAt(h), rnd));
                    }
                }
                i++; // 跳过代理对的第二个 char
            }
        }
        return sb.toString();
    }

    /**
     * 为单个 hex 数字字符选择一个 Unicode 多文种等价数字
     * 如果是 0-9 的数字，从多文种数字池中随机选取
     * 如果是 A-F 的字母，保留原样
     */
    private static String pickGhostHexDigit(char hexChar, java.util.Random rnd) {
        if (hexChar >= '0' && hexChar <= '9') {
            int digitVal = hexChar - '0';
            java.util.List<Integer> pool = UNICODE_DIGIT_MAP.get(digitVal);
            if (pool != null && !pool.isEmpty()) {
                int cp = pool.get(rnd.nextInt(pool.size()));
                return new String(Character.toChars(cp));
            }
        }
        return String.valueOf(hexChar);
    }

    // ---- 多文种数字映射表 ----
    // Character.digit(cp, 16) 对这些字符返回 0-9 的等价数值
    // 注意: 不使用增补平面 (>U+FFFF) 的数学数字，会破坏 fastjson 解析
    private static final java.util.Map<Integer, java.util.List<Integer>> UNICODE_DIGIT_MAP
            = buildUnicodeDigitMap();

    private static java.util.Map<Integer, java.util.List<Integer>> buildUnicodeDigitMap() {
        int[][] ranges = {
                {0x0660, 0x0669},  // 阿拉伯文
                {0x06F0, 0x06F9},  // 扩展阿拉伯文-印度数字
                {0x0A66, 0x0A6F},  // 古木基文
                {0x0E50, 0x0E59},  // 泰文
                {0x0ED0, 0x0ED9},  // 老挝文
                {0x0F20, 0x0F29},  // 藏文
                {0xA620, 0xA629},  // 瓦伊语
                {0xA8D0, 0xA8D9},  // 绍拉什特拉文
                {0xA900, 0xA909},  // 克耶利文
                {0xFF10, 0xFF19},  // 全角数字
        };
        java.util.Map<Integer, java.util.List<Integer>> map = new java.util.HashMap<>();
        for (int d = 0; d < 10; d++) {
            map.put(d, new java.util.ArrayList<>());
        }
        for (int[] range : ranges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                int digitVal = cp - range[0];
                if (digitVal < 10 && cp >= 0x80) {
                    map.get(digitVal).add(cp);
                }
            }
        }
        return map;
    }
}
