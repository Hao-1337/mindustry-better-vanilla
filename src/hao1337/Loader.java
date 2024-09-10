package hao1337;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import hao1337.content.blocks.HaoBlocks;
import hao1337.content.items.HaoItems;
import hao1337.modification.ForceProjector;
import hao1337.modification.HeatReactor;
import hao1337.modification.Liquids;
import hao1337.modification.OverrideDome;
import hao1337.modification.ScrapWall;
import hao1337.modification.SlagCentrifuge;
import hao1337.modification.TechTreeModification;
import hao1337.modification.Vault;
import hao1337.ui.TimeControl;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.SectorPresets;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;

public class Loader {
    public void load() {
        Log.info("[Mod]Load vanilla modification");
        ForceProjector.load();
        OverrideDome.load();
        Vault.load();
        hao1337.modification.Items.load();
        Liquids.load();
        ScrapWall.load();
        HeatReactor.load();
        SlagCentrifuge.load();
    }

    public void updateState(boolean state) {
        Log.info("Mod Current state: @", state);

        ForceProjector.apply(state);
        OverrideDome.apply(state);
        Vault.apply(state);
        ScrapWall.apply(state);
        
        TimeControl.enableSpeedUp = state;
        contentSwitcher(state);
    }

    public void contentSwitcher(boolean enable) {
        HaoBlocks.armoredThoriumConveyor.buildVisibility =
        HaoBlocks.thoriumConveyor.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.thorium-conveyor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.surgeConveyor.buildVisibility =
        HaoBlocks.armoredSurgeConveyor.buildVisibility =  enable && !Core.settings.getBool("hao1337.gameplay.serpulo.surge-conveyor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.box.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.box") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.silo.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.silo") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.ultraVault.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.ultra-vault") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.valveUnloader.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.valve-unloader") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.leviathanReconstructor.buildVisibility  = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.leviathan-reconstructor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.m1014.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.m1014") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.dropper.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.dropper") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.giganticDome.buildVisibility = enable && !Core.settings.getBool("hao1337.gameplay.serpulo.gigantic-dome") ? BuildVisibility.shown : BuildVisibility.hidden;        
    }

    public static void generateContent() {
        TechTreeModification.margeNode(Blocks.phaseHeater, Blocks.heatReactor, ItemStack.with(Items.beryllium, 2000, Items.oxide, 1500, Items.silicon, 3000), Seq.with(new Objectives.SectorComplete(SectorPresets.stronghold)));
        TechTreeModification.margeNodeProduce(Items.thorium, Items.fissileMatter, 1);
        TechTreeModification.margeNodeProduce(Items.fissileMatter, HaoItems.uranium, 0);
        // Vars.state.rules.hiddenBuildItems.clear();
    }

}
