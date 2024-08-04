package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;
import mindustry.world.meta.BuildVisibility;

public class ScrapWall {
    private static BuildVisibility _1;
    private static BuildVisibility _2;
    private static BuildVisibility _3;
    private static BuildVisibility _4;
    private static boolean ik = false;

    public static float newHealth = 1500f;

    public static void load() {
        _1 = Blocks.scrapWall.buildVisibility;
        _2 = Blocks.scrapWallLarge.buildVisibility;
        _3 = Blocks.scrapWallHuge.buildVisibility;
        _4 = Blocks.scrapWallGigantic.buildVisibility;

        ik = Core.settings.getBool("hao1337.gameplay.serpulo.scrap-wall");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            Blocks.scrapWall.buildVisibility = Blocks.scrapWallLarge.buildVisibility = Blocks.scrapWallHuge.buildVisibility = Blocks.scrapWallGigantic.buildVisibility = BuildVisibility.shown;
            return;
        }
        Blocks.scrapWall.buildVisibility = _1;
        Blocks.scrapWallLarge.buildVisibility = _2;
        Blocks.scrapWallHuge.buildVisibility = _3;
        Blocks.scrapWallGigantic.buildVisibility = _4;
    }
}
