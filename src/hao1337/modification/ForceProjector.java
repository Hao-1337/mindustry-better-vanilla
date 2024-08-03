package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;

public class ForceProjector {
    private static float shieldHealth;
    private static boolean ik;

    public static float newHealth = 1500f;

    public static void load() {
        shieldHealth = ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth;

        ik = Core.settings.getBool("hao1337.gameplay.better-shield");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth = newHealth;
            return;
        }
        ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth = shieldHealth;
    }
}
