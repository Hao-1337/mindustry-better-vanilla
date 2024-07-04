package hao1337.content.blocks;

import mindustry.Vars;
import mindustry.world.Block;

public class HaoBlocks {
    // Storage blocks - serpulo
    public static Block box, storage, cluster;
    // Conveyor
    public static Block thoriumConveyor, armoredThoriumConveyor, surgeConveyor, armoredSurgeConveyor;

    public static void load() {
        // Use json for bypass content loader
        box = Vars.content.block("hao1337-mod-box");
        storage = Vars.content.block("hao1337-mod-storage");
        cluster = Vars.content.block("hao1337-mod-cluster");
        thoriumConveyor = Vars.content.block("hao1337-mod-thorium-conveyor");
        armoredThoriumConveyor = Vars.content.block("hao1337-mod-armored-thorium-conveyor");
        surgeConveyor = Vars.content.block("hao1337-mod-serpulo-surge-conveyor");
        armoredSurgeConveyor = Vars.content.block("hao1337-mod-armored-surge-conveyor");
    }
}
