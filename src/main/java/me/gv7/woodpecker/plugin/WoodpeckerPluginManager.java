package me.gv7.woodpecker.plugin;


import me.gv7.woodpecker.plugin.helpers.*;
import util.GhostBits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WoodpeckerPluginManager implements IPluginManager, IHelperPlugin {
    public static IPluginHelper pluginHelper;

    @Override
    public void registerPluginManagerCallbacks(IPluginManagerCallbacks iPluginManagerCallbacks) {
        iPluginManagerCallbacks.registerHelperPlugin(this);
    }

    @Override
    public void HelperPluginMain(IHelperPluginCallbacks helperPluginCallbacks) {
        helperPluginCallbacks.setHelperPluginAutor("whwlsfb");
        helperPluginCallbacks.setHelperPluginName("GhostBits 编解码器");
        helperPluginCallbacks.setHelperPluginVersion("2.0");
        helperPluginCallbacks.setHelperPluginDescription(
                "Ghost Bits WAF 绕过编解码工具集 — 基于 BlackHat Asia 2026 Ghost Bits 攻击技术");
        pluginHelper = helperPluginCallbacks.getPluginHelper();

        // 注册所有 Helper Tab
        List<IHelper> helpers = new ArrayList<>();

        // ── 原有功能 ──
        helpers.add(new Encoder());           // 通用 Ghost Bits 编码（多语言区块变体）
        helpers.add(new Decoder());           // Ghost Bits 解码

        // ── 攻击模式 ──
        helpers.add(new FastjsonGhostHelper());   // FastJSON 多文种数字混淆
        helpers.add(new JettyHexHelper());        // Jetty convertHexDigit 位运算绕过
        helpers.add(new FullwidthUrlHelper());    // 全角 URL 编码
        helpers.add(new GhostUrlHelper());        // Ghost URL (Jetty Hex + CJK 混合)
        helpers.add(new Base64GhostHelper());     // Base64 Ghost Bits
        helpers.add(new CrlfGhostHelper());       // CRLF 注入编码 (SMTP/HTTP)
        helpers.add(new BcelGhostHelper());       // BCEL ClassLoader Ghost Bits
        helpers.add(new JacksonGhostHelper());    // Jackson charToHex Ghost Bits
        helpers.add(new HelpHelper());            // 使用帮助

        helperPluginCallbacks.registerHelper(helpers);
    }

    public static class Encoder implements IHelper {

        @Override
        public String getHelperTabCaption() {
            return "编码";
        }

        @Override
        public IArgsUsageBinder getHelperCutomArgs() {
            IArgsUsageBinder argsUsageBinder = pluginHelper.createArgsUsageBinder();
            List<IArg> args = new ArrayList<IArg>();
            IArg args1 = pluginHelper.createArg();
            args1.setName("all");
            args1.setDefaultValue("../../../");
            args1.setDescription("write text");
            args1.setRequired(true);
            args.add(args1);
            argsUsageBinder.setArgsList(args);
            return argsUsageBinder;
        }

        @Override
        public void doHelp(Map<String, Object> customArgs, IResultOutput resultOutput) throws Throwable {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("原文: ").append(customArgs.get("all")).append("\n\n");
            GhostBits.generateVariantsByBlock((String) customArgs.get("all"), 5).forEach(variant -> stringBuilder.append(variant).append("\n"));
            resultOutput.successPrintln(stringBuilder.toString());
        }
    }

    public static class Decoder implements IHelper {

        @Override
        public String getHelperTabCaption() {
            return "解码";
        }

        @Override
        public IArgsUsageBinder getHelperCutomArgs() {
            IArgsUsageBinder argsUsageBinder = pluginHelper.createArgsUsageBinder();
            List<IArg> args = new ArrayList<IArg>();
            IArg args1 = pluginHelper.createArg();
            args1.setName("all");
            args1.setDefaultValue("丮丮丯丮丮丯丮丮丯");
            args1.setDescription("write text");
            args1.setRequired(true);
            args.add(args1);
            argsUsageBinder.setArgsList(args);
            return argsUsageBinder;
        }

        @Override
        public void doHelp(Map<String, Object> customArgs, IResultOutput resultOutput) throws Throwable {
            resultOutput.successPrintln(GhostBits.decode((String) customArgs.get("all")));
        }
    }

}
