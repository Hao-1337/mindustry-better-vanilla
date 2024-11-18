package hao1337.modification;

import mindustry.content.Blocks;
import mindustry.world.meta.BuildVisibility;

public class SlagCentrifuge {
    public static void apply(boolean enable) {
        Blocks.slagCentrifuge.buildVisibility = enable ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
