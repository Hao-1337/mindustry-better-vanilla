package hao1337.modification;

import arc.Core;
import mindustry.Vars;

public class ForceProjector {
    public static void load() {
        if (Core.settings.getBool("hao1337.gameplay.better-shield")) {
            mindustry.world.blocks.defense.ForceProjector forceProjector = (mindustry.world.blocks.defense.ForceProjector)Vars.content.block("force-projector");
            
            forceProjector.shieldHealth = 1500f;
        }
    }
}
