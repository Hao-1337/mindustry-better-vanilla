package hao1337.modification;

import mindustry.content.Blocks;

public class ForceProjector {
    private static float shieldHealth;

    public static float newHealth = 1500f;

    public static void load() {
        shieldHealth = ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth;
    }

    public static void apply(boolean is) {
        if (is) {
            ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth = newHealth;
            return;
        }
        ((mindustry.world.blocks.defense.ForceProjector)Blocks.forceProjector).shieldHealth = shieldHealth;
    }
}
