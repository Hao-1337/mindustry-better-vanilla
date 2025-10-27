package hao1337.content.blocks;

import mindustry.Vars;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.GenericCrafter;

public class HaoBlocks {
    // Storage blocks - serpulo
    public static Block box, silo, ultraVault, valveUnloader, giganticDome, noConnectContainer;
    // Conveyor - serpulo
    public static Block thoriumConveyor, armoredThoriumConveyor, surgeConveyor, armoredSurgeConveyor;
    // Constructor - serpulo
    public static Block leviathanReconstructor;
    // Defense - serpulo
    public static Turret m1014, dropper;

    // Crafting - erekir
    public static GenericCrafter uraniumCentrifuge;

    public static void load() {
        // Use json for bypass content loader
        noConnectContainer = Vars.content.block("hao1337-mod-no-connect-container");
        box = Vars.content.block("hao1337-mod-box");
        silo = Vars.content.block("hao1337-mod-silo");
        ultraVault = Vars.content.block("hao1337-mod-ultra-vault");
        valveUnloader = Vars.content.block("hao1337-mod-valve-unloader");
        thoriumConveyor = Vars.content.block("hao1337-mod-thorium-conveyor");
        armoredThoriumConveyor = Vars.content.block("hao1337-mod-armored-thorium-conveyor");
        surgeConveyor = Vars.content.block("hao1337-mod-serpulo-surge-conveyor");
        armoredSurgeConveyor = Vars.content.block("hao1337-mod-armored-surge-conveyor");
        leviathanReconstructor = Vars.content.block("hao1337-mod-leviathan-reconstructor");
        giganticDome = Vars.content.block("hao1337-mod-gigantic-dome");
        m1014 = (Turret)Vars.content.block("hao1337-mod-m1014");
        dropper = (Turret)Vars.content.block("hao1337-mod-dropper");
        uraniumCentrifuge = (GenericCrafter)Vars.content.block("hao1337-mod-uranium-centrifuge");
    }
}
