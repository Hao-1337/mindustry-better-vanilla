package hao1337.content.blocks;

import hao1337.content.items.HaoItems;
import hao1337.modification.TechTreeModification;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.Planets;
import mindustry.content.SectorPresets;
import mindustry.content.TechTree;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.effect.RadialEffect;
import mindustry.game.Objectives;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.type.SectorPreset;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.heat.HeatProducer;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;

public class HaoBlocks {
    // Storage blocks - serpulo
    public static Block box, silo, ultraVault, valveUnloader, giganticDome;
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

    public static void generateContent() {
        TechTreeModification.margeNode(Blocks.phaseHeater, Blocks.heatReactor, ItemStack.with(Items.beryllium, 2000, Items.oxide, 1500, Items.silicon, 3000), Seq.with(new Objectives.SectorComplete(SectorPresets.stronghold)));
        TechTreeModification.margeNodeProduce(Items.thorium, Items.fissileMatter, 1);
        // uraniumCentrifuge.drawer = new DrawMulti(new DrawDefault(), new DrawBlurSpin(), new DrawArcSmelt());
    }
}
