package me.gv7.woodpecker.plugin.helpers;

import me.gv7.woodpecker.plugin.*;
import java.util.*;
import static me.gv7.woodpecker.plugin.WoodpeckerPluginManager.pluginHelper;

/**
 * 帮助文档 Tab
 */
public class HelpHelper implements IHelper {
    @Override
    public String getHelperTabCaption() { return "使用帮助"; }

    @Override
    public IArgsUsageBinder getHelperCutomArgs() {
        IArgsUsageBinder binder = pluginHelper.createArgsUsageBinder();
        List<IArg> args = new ArrayList<>();
        IArg arg = pluginHelper.createArg();
        arg.setName("all");
        arg.setDefaultValue("点击执行按钮查看帮助文档");
        arg.setDescription("无需输入");
        arg.setRequired(false);
        args.add(arg);
        binder.setArgsList(args);
        return binder;
    }

    @Override
    public void doHelp(Map<String, Object> customArgs, IResultOutput resultOutput) throws Throwable {
        resultOutput.infoPrintln(buildHelpText());
    }

    private String buildHelpText() {
        StringBuilder sb = new StringBuilder();

        sb.append("================================================================\n");
        sb.append("  Ghost Bits WAF 绕过编解码工具集 v2.0\n");
        sb.append("  基于 BlackHat Asia 2026 Ghost Bits 攻击技术\n");
        sb.append("================================================================\n\n");

        sb.append("【核心原理】\n");
        sb.append("Java 中 char 是 16 位，强转 (byte)ch 时高 8 位被静默丢弃。\n");
        sb.append("攻击者将恶意字节放到 Unicode 高位字符中：\n");
        sb.append("  WAF 看到的 -> 无害的 Unicode 字符 (如汉字、盲文)\n");
        sb.append("  Java 截断后 -> 还原出真实的恶意载荷 (如 ../ 或 .jsp)\n\n");

        sb.append("【使用方式】\n");
        sb.append("  1. 输入框直接填写完整内容\n");
        sb.append("  2. 每次执行随机生成不同编码，多点几次得到不同变体\n");
        sb.append("  3. 用「解码」Tab 可验证编码结果是否正确\n\n");

        sb.append("================================================================\n");
        sb.append("  基础功能 (2个)\n");
        sb.append("================================================================\n\n");

        sb.append("【编码】\n");
        sb.append("  做什么: 通用 Ghost Bits 编码，输出 5 个不同语言区块的变体\n");
        sb.append("  怎么用: 输入任意文本，如 ../../../ 或 class 或 1.jsp\n");
        sb.append("  用在哪: 所有支持 UTF-8 的 Java 中间件，最通用的绕过方式\n");
        sb.append("  适用场景: Tomcat上传绕过/Spring4Shell/路径穿越/通用WAF绕过\n\n");

        sb.append("【解码】\n");
        sb.append("  做什么: 对每个字符取低 8 位还原原始文本\n");
        sb.append("  怎么用: 粘贴 Ghost 编码后的文本\n");
        sb.append("  用在哪: 验证任何编码结果是否正确\n\n");

        sb.append("================================================================\n");
        sb.append("  按「谁来解码」分类的攻击模式\n");
        sb.append("================================================================\n\n");

        sb.append("---- Jetty 中间件 (convertHexDigit 位运算漏洞) ----\n\n");

        sb.append("【Jetty Hex编码】\n");
        sb.append("  做什么: 把文本转为 %XX 编码，但 XX 中用非标准字符替代 hex\n");
        sb.append("  原理: Jetty 用位运算 (c&0x1F)+((c>>6)<<3)-16 解析 hex，\n");
        sb.append("        某些 ASCII 字符 (如 P=8, Q=9, p=8) 经此公式产生合法值，\n");
        sb.append("        WAF 不认识这些字符所以忽略，Jetty 正常解析\n");
        sb.append("  怎么用: 输入任意文本，如 ../../../etc/passwd\n");
        sb.append("  用在哪: Jetty 作为中间件的 URL 参数注入、路径穿越\n");
        sb.append("  优势: 纯 ASCII 逻辑层绕过，不依赖 UTF-8，绝对免疫编码坍塌\n\n");

        sb.append("【Ghost URL编码】\n");
        sb.append("  做什么: 和 Jetty Hex 类似，但交替混合两种编码方式\n");
        sb.append("  原理: 偶数位字节用 Jetty Hex (%XX ghost)，奇数位用 CJK 高位包装\n");
        sb.append("  怎么用: 输入任意文本\n");
        sb.append("  用在哪: 同 Jetty Hex，但混合两种技术增加变异性\n");
        sb.append("  vs Jetty Hex: Jetty Hex 纯 ASCII 更安全；Ghost URL 变异性更大\n\n");

        sb.append("【全角URL编码】\n");
        sb.append("  做什么: 把 ASCII 字符映射到全角区 (如 . -> ．)，再 URL 编码\n");
        sb.append("  原理: 全角字符 = 原字符 + 0xFEE0，应用层可能把全角当半角处理\n");
        sb.append("  怎么用: 输入任意文本\n");
        sb.append("  用在哪: 无 NFKC 规范化的老旧 Java 中间件\n");
        sb.append("  警告: 现代中间件的 NFKC 规范化会将全角还原为半角，使绕过失效!\n");
        sb.append("  vs Jetty Hex: Jetty Hex 免疫规范化；全角不免疫，仅限老旧环境\n\n");

        sb.append("---- FastJSON (Character.digit 多文种数字漏洞) ----\n\n");

        sb.append("【FastJSON Ghost】\n");
        sb.append("  做什么: 在 Unicode 转义的 hex 数字中，用多文种等价字符替换\n");
        sb.append("  原理: FastJSON 解析 Unicode 转义时调用 Character.digit(ch,16)\n");
        sb.append("        阿拉伯文/泰文/藏文/全角数字 经此方法返回与 0-9 相同的值\n");
        sb.append("        WAF 看到的是乱码数字，FastJSON 正常解析出合法字符\n");
        sb.append("  怎么用: 输入 JSON 文本或普通文本\n");
        sb.append("        JSON 输入会自动识别，仅编码字符串字面量，保留 JSON 结构\n");
        sb.append("  用在哪: FastJSON 反序列化绕过 WAF (如 @type 类型混淆)\n");
        sb.append("  优势: 绝对免疫编码坍塌和规范化 (合法 Unicode 字符)\n\n");

        sb.append("---- 通用 (byte)ch 截断 ----\n\n");

        sb.append("【Base64 Ghost】\n");
        sb.append("  做什么: 先 Base64 编码，再给每个字符加随机高字节\n");
        sb.append("  原理: JDK Base64 解码器查表时 (byte)ch 截断高位，仍正确解码\n");
        sb.append("  怎么用: 输入原始文本 (如 payload)，不是 Base64 字符串\n");
        sb.append("  用在哪: Java 反序列化 payload 等需要 Base64 传输的场景\n\n");

        sb.append("【CRLF注入编码】\n");
        sb.append("  做什么: 将 [CRLF] 标记替换为 Ghost 化的回车(0x0D)+换行(0x0A)\n");
        sb.append("  原理: 中间件检测换行符来防御 CRLF 注入，Ghost 化后绕过检测\n");
        sb.append("  怎么用: 在想注入换行的位置写 [CRLF]\n");
        sb.append("        SMTP: attacker[CRLF]DATA[CRLF]Subject: PWNED\n");
        sb.append("        HTTP: 1[CRLF]POST /evil HTTP/1.1\n");
        sb.append("  用在哪: SMTP (angus.mail截断) / HTTP (HttpClient截断)\n\n");

        sb.append("【BCEL Ghost】\n");
        sb.append("  做什么: 对 BCEL 编码字符串做 Ghost 高位包装\n");
        sb.append("  原理: BCEL ClassLoader 解析类名时 (byte) 截断还原\n");
        sb.append("  怎么用: 输入完整 BCEL 字符串 (含 $$BCEL$$ 前缀)\n");
        sb.append("        工具自动识别 $$BCEL$$ 前缀，保留前缀不编码，只编码后面 payload\n");
        sb.append("  用在哪: BCEL ClassLoader 漏洞利用绕过 WAF\n\n");

        sb.append("【Jackson Ghost】\n");
        sb.append("  做什么: 对文本每个字符加随机高字节\n");
        sb.append("  原理: Jackson 内部 charToHex 使用 (byte)ch 截断\n");
        sb.append("  怎么用: 输入 JSON 文本或普通文本\n");
        sb.append("        JSON 输入会自动识别，仅编码字符串字面量（key和value），保留 JSON 结构\n");
        sb.append("  用在哪: Jackson 反序列化场景的 WAF 绕过\n");
        sb.append("  vs FastJSON Ghost: FastJSON 用多文种数字替换; Jackson 用高字节包装\n\n");

        sb.append("================================================================\n");
        sb.append("  技术来源: BlackHat Asia 2026 - Ghost Bits\n");
        sb.append("================================================================\n");

        return sb.toString();
    }
}
