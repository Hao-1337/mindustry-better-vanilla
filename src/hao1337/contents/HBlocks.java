package hao1337.contents;

import mindustry.Vars;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.Reconstructor;

public class HBlocks {
    // Storage blocks - serpulo
    public static Block box, silo, ultraVault, valveUnloader, giganticDome, noConnectContainer,
    // Liquid - serpulo
    liquidStorageCore, liquidStorageRouter, liquidStorageUnloader;
    // Conveyor - serpulo
    public static Conveyor thoriumConveyor, armoredThoriumConveyor, surgeConveyor, armoredSurgeConveyor;
    // Constructor - serpulo
    public static Reconstructor leviathanReconstructor;
    // Defense - serpulo
    public static Turret m1014, dropper;

    // Crafting - erekir
    public static GenericCrafter uraniumCentrifuge;

    static Block block(String name) { return Vars.content.block("hao1337-mod-" + name); }
    static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;

        // Use json for bypass content loader
        noConnectContainer = block("no-connect-container");
        box = block("box");
        silo = block("silo");
        ultraVault = block("ultra-vault");
        valveUnloader = block("valve-unloader");
        thoriumConveyor = (Conveyor) block("thorium-conveyor");
        armoredThoriumConveyor = (Conveyor) block("armored-thorium-conveyor");
        surgeConveyor = (Conveyor) block("serpulo-surge-conveyor");
        armoredSurgeConveyor = (Conveyor) block("armored-surge-conveyor");
        leviathanReconstructor = (Reconstructor) block("leviathan-reconstructor");
        giganticDome = block("gigantic-dome");
        m1014 = (Turret)block("m1014");
        dropper = (Turret)block("dropper");
        uraniumCentrifuge = (GenericCrafter)block("uranium-centrifuge");
        liquidStorageCore = block("multi-liquid-storage");
        liquidStorageRouter = block("multi-liquid-router");
        liquidStorageUnloader = block("multi-liquid-unloader");
    }
}
