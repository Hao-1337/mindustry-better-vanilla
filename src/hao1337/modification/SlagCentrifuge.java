package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;
import mindustry.world.meta.BuildVisibility;

public class SlagCentrifuge {
    public static void load() {
        Blocks.slagCentrifuge.buildVisibility = Core.settings.getBool("hao1337.gameplay.slag-centrifuge") ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
