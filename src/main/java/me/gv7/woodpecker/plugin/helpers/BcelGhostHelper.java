package me.gv7.woodpecker.plugin.helpers;

import me.gv7.woodpecker.plugin.*;
import util.GhostBits;
import java.util.*;
import static me.gv7.woodpecker.plugin.WoodpeckerPluginManager.pluginHelper;

public class BcelGhostHelper implements IHelper {
    // BCEL ClassLoader 的标识前缀
    private static final String BCEL_PREFIX = "$$BCEL$$";

    @Override
    public String getHelperTabCaption() { return "BCEL Ghost"; }

    @Override
    public IArgsUsageBinder getHelperCutomArgs() {
        IArgsUsageBinder binder = pluginHelper.createArgsUsageBinder();
        List<IArg> args = new ArrayList<>();
        IArg arg = pluginHelper.createArg();
        arg.setName("all");
        arg.setDefaultValue("$$BCEL$$$l$8b$I$A$A$A$A$A$A$A$9c$bc$db$d2$ab$ca$96");
        arg.setDescription("BCEL编码字符串");
        arg.setRequired(true);
        args.add(arg);
        binder.setArgsList(args);
        return binder;
    }

    @Override
    public void doHelp(Map<String, Object> customArgs, IResultOutput resultOutput) throws Throwable {
        String text = (String) customArgs.get("all");
        String payload;
        if (text.startsWith(BCEL_PREFIX)) {
            // 跳过 $$BCEL$$ 前缀，只编码后面的内容，输出时拼回
            String body = text.substring(BCEL_PREFIX.length());
            payload = BCEL_PREFIX + GhostBits.bcelGhostEncode(body);
        } else {
            // 没有前缀，整体编码
            payload = GhostBits.encodePerCharRandom(text);
        }
        resultOutput.successPrintln(payload);
    }
}
