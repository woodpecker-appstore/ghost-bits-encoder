package me.gv7.woodpecker.plugin.helpers;

import me.gv7.woodpecker.plugin.*;
import util.GhostBits;
import java.util.*;
import static me.gv7.woodpecker.plugin.WoodpeckerPluginManager.pluginHelper;

public class JacksonGhostHelper implements IHelper {
    @Override
    public String getHelperTabCaption() { return "Jackson Ghost"; }

    @Override
    public IArgsUsageBinder getHelperCutomArgs() {
        IArgsUsageBinder binder = pluginHelper.createArgsUsageBinder();
        List<IArg> args = new ArrayList<>();
        IArg arg = pluginHelper.createArg();
        arg.setName("all");
        arg.setDefaultValue("{\"@type\":\"com.sun.rowset.JdbcRowSetImpl\"}");
        arg.setDescription("待编码文本");
        arg.setRequired(true);
        args.add(arg);
        binder.setArgsList(args);
        return binder;
    }

    @Override
    public void doHelp(Map<String, Object> customArgs, IResultOutput resultOutput) throws Throwable {
        String text = (String) customArgs.get("all");
        resultOutput.successPrintln(GhostBits.jacksonGhostEncode(text));
    }
}
