package hao1337.modification;

import arc.Core;
import mindustry.Vars;
import mindustry.world.meta.BuildVisibility;

public class ScrapWall {
    private static BuildVisibility _1;
    private static BuildVisibility _2;
    private static BuildVisibility _3;
    private static BuildVisibility _4;
    public static mindustry.world.blocks.defense.Wall scrapWall;
    public static mindustry.world.blocks.defense.Wall scrapWallLarge;
    public static mindustry.world.blocks.defense.Wall scrapWallHuge;
    public static mindustry.world.blocks.defense.Wall scrapWallGigantic;
    private static boolean ik;

    public static float newHealth = 1500f;

    public static void load() {
        scrapWall = (mindustry.world.blocks.defense.Wall) Vars.content.block("scrap-wall");
        scrapWallLarge = (mindustry.world.blocks.defense.Wall) Vars.content.block("scrap-wall-large");
        scrapWallHuge = (mindustry.world.blocks.defense.Wall) Vars.content.block("scrap-wall-huge");
        scrapWallGigantic = (mindustry.world.blocks.defense.Wall) Vars.content.block("scrap-wall-gigantic");

        _1 = scrapWall.buildVisibility;
        _2 = scrapWallLarge.buildVisibility;
        _3 = scrapWallHuge.buildVisibility;
        _4 = scrapWallGigantic.buildVisibility;

        ik = Core.settings.getBool("hao1337.gameplay.scrap-wall");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            scrapWall.buildVisibility = scrapWallLarge.buildVisibility = scrapWallHuge.buildVisibility = scrapWallGigantic.buildVisibility = BuildVisibility.shown;
            return;
        }
        scrapWall.buildVisibility = _1;
        scrapWallLarge.buildVisibility = _2;
        scrapWallHuge.buildVisibility = _3;
        scrapWallGigantic.buildVisibility = _4;
    }
}
