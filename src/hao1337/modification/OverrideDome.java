package hao1337.modification;

import arc.Core;
import mindustry.Vars;
import mindustry.world.blocks.defense.OverdriveProjector;

public class OverrideDome {
    public static void load() {
        if (Core.settings.getBool("hao1337.gameplay.better-override-dome")) {
            OverdriveProjector overridedome = (OverdriveProjector)Vars.content.block("overdrive-dome");
            overridedome.range = 300f;
            overridedome.speedBoost = 3f;
        }
    }
}
