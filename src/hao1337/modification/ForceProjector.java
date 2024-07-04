package hao1337.modification;

import arc.Core;
import mindustry.Vars;

public class ForceProjector {
    private static float shieldHealth;
    private static mindustry.world.blocks.defense.ForceProjector forceProjector;
    private static boolean ik;

    public static float newHealth = 1500f;

    public static void load() {
        forceProjector = (mindustry.world.blocks.defense.ForceProjector) Vars.content.block("force-projector");

        shieldHealth = forceProjector.shieldHealth;

        ik = Core.settings.getBool("hao1337.gameplay.better-shield");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            forceProjector.shieldHealth = newHealth;
            return;
        }
        forceProjector.shieldHealth = shieldHealth;
    }
}
