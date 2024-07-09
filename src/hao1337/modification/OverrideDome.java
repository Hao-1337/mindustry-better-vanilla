package hao1337.modification;

import arc.Core;
import mindustry.Vars;
import mindustry.world.blocks.defense.OverdriveProjector;

public class OverrideDome {
    private static float range;
    private static float speed;
    private static OverdriveProjector overridedome;
    private static boolean ik;

    public static float newRange = 300f;
    public static float newSpeed = 2.5f;

    public static void load() {
        overridedome = (OverdriveProjector)Vars.content.block("overdrive-dome");

        range = overridedome.range;
        speed = overridedome.speedBoost;
        
        ik = Core.settings.getBool("hao1337.gameplay.better-override-dome");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            overridedome.range = newRange;
            overridedome.speedBoost = newSpeed;
            return;
        }
        overridedome.range = range;
        overridedome.speedBoost = speed;
    }
}
