package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;
import mindustry.world.blocks.defense.OverdriveProjector;

public class OverrideDome {
    private static float range;
    private static float speed;
    private static boolean ik;

    public static float newRange = 300f;
    public static float newSpeed = 2.5f;

    public static void load() {
        range = ((OverdriveProjector)Blocks.overdriveDome).range;
        speed = ((OverdriveProjector)Blocks.overdriveDome).speedBoost;
        
        ik = Core.settings.getBool("hao1337.gameplay.serpulo.better-override-dome");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            ((OverdriveProjector)Blocks.overdriveDome).range = newRange;
            ((OverdriveProjector)Blocks.overdriveDome).speedBoost = newSpeed;
            return;
        }
        ((OverdriveProjector)Blocks.overdriveDome).range = range;
        ((OverdriveProjector)Blocks.overdriveDome).speedBoost = speed;
    }
}
