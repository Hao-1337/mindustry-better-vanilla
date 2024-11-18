package hao1337.modification;

import mindustry.content.Blocks;
import mindustry.world.meta.BuildVisibility;

public class HeatReactor {
    public static void apply(boolean enable) {
        Blocks.heatReactor.buildVisibility = enable ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
