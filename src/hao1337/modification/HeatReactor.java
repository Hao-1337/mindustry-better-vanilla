package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;
import mindustry.world.meta.BuildVisibility;

public class HeatReactor {
    public static void load() {
        Blocks.heatReactor.buildVisibility = Core.settings.getBool("hao1337.gameplay.heat-generator") ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
