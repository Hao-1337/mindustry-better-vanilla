package hao1337.content.blocks;

import mindustry.Vars;
import mindustry.world.Block;

public class HaoBlocks {
    // Storage blocks - serpulo
    public static Block box, silo, ultraVault, valveUnloader;
    // Conveyor
    public static Block thoriumConveyor, armoredThoriumConveyor, surgeConveyor, armoredSurgeConveyor;
    // Constructor
    public static Block leviathanReconstructor;

    public static void load() {
        // Use json for bypass content loader
        box = Vars.content.block("hao1337-mod-box");
        silo = Vars.content.block("hao1337-mod-silo");
        ultraVault = Vars.content.block("hao1337-mod-ultra-vault");
        valveUnloader = Vars.content.block("hao1337-mod-valve-unloader");
        thoriumConveyor = Vars.content.block("hao1337-mod-thorium-conveyor");
        armoredThoriumConveyor = Vars.content.block("hao1337-mod-armored-thorium-conveyor");
        surgeConveyor = Vars.content.block("hao1337-mod-serpulo-surge-conveyor");
        armoredSurgeConveyor = Vars.content.block("hao1337-mod-armored-surge-conveyor");
        leviathanReconstructor = Vars.content.block("hao1337-mod-leviathan-reconstructor");
    }
}
