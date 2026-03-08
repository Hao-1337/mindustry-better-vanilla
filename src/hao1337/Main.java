package hao1337;

import arc.Core;
import arc.util.Log;
import mindustry.mod.Mod;
import static hao1337.HVars.*;

/**
 * HERE WE GO!! Yippeeeeeeeeeeeeeeeeeee :)))
 * @author Hao-1337
 */
public class Main extends Mod {
    @Override
    public void init() {
        Log.info("[Hao1337: Better Vanilla] is launching.");
        AutoUpdate.load(gitapi, name, repoName, unzipName, version);
        // RegionPart

        new HVars();
        if (Core.settings.getBool("hao1337.toggle.autoupdate")) AutoUpdate.check();
    }

}
