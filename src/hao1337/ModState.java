package hao1337;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.Seq;
import hao1337.content.blocks.HaoBlocks;
import hao1337.content.items.HaoItems;
import hao1337.modification.ForceProjector;
import hao1337.modification.HeatReactor;
import hao1337.modification.Liquids;
import hao1337.modification.OverrideDome;
import hao1337.modification.SlagCentrifuge;
import hao1337.modification.TechTreeModification;
import hao1337.modification.Vault;
import hao1337.net.ModStatePackage;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.SectorPresets;
import mindustry.game.Objectives;
import mindustry.game.EventType.PlayerJoin;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;
public class ModState {
    public boolean thoriumConveyor;
    public boolean surgeConveyor;

    public boolean box;
    public boolean silo;
    public boolean ultraVault;
    public boolean valveUnloader;
    public boolean noConnectContainer;
    public boolean leviathanReconstructor;

    public boolean dropper;
    public boolean m1014;

    public boolean giganticDome;
    public boolean slagCentrifuge;
    public boolean heatReactor;

    public boolean betterShield;
    public boolean betterOverrideDome;
    public boolean betterSerpuloVault;
    public boolean betterErekirVault;

    public boolean hiddenLiquids;
    public boolean hiddenItems;

    public int sechematicSize;
    public boolean experimental;

    public ModState() {
        netWorking();
    }

    public void load() {
        ForceProjector.load();
        OverrideDome.load();
        Vault.load();

        updateState();
        applyState(true);
    }

    public void updateState() {
        thoriumConveyor = Core.settings.getBool("hao1337.gameplay.serpulo.thorium-conveyor");
        surgeConveyor = Core.settings.getBool("hao1337.gameplay.serpulo.surge-conveyor");

        box = Core.settings.getBool("hao1337.gameplay.serpulo.box");
        silo = Core.settings.getBool("hao1337.gameplay.serpulo.silo");
        noConnectContainer = Core.settings.getBool("hao1337.gameplay.serpulo.no-connect-container");
        ultraVault = Core.settings.getBool("hao1337.gameplay.serpulo.ultra-vault");
        valveUnloader = Core.settings.getBool("hao1337.gameplay.serpulo.valve-unloader");
        leviathanReconstructor = Core.settings.getBool("hao1337.gameplay.serpulo.leviathan-reconstructor");

        dropper = Core.settings.getBool("hao1337.gameplay.serpulo.dropper");
        m1014 = Core.settings.getBool("hao1337.gameplay.serpulo.m1014");

        giganticDome = Core.settings.getBool("hao1337.gameplay.serpulo.gigantic-dome");
        slagCentrifuge =  Core.settings.getBool("hao1337.gameplay.erekir.slag-centrifuge");
        heatReactor = Core.settings.getBool("hao1337.gameplay.erekir.heat-generator");

        betterShield = Core.settings.getBool("hao1337.gameplay.serpulo.better-shield");
        betterOverrideDome = Core.settings.getBool("hao1337.gameplay.serpulo.better-override-dome");
        betterSerpuloVault = Core.settings.getBool("hao1337.gameplay.serpulo.vault-bigger");
        betterErekirVault = Core.settings.getBool("hao1337.gameplay.erekir.vault-bigger");

        hiddenLiquids = Core.settings.getBool("hao1337.gameplay.erekir.hidden-liquid");
        hiddenItems = Core.settings.getBool("hao1337.gameplay.erekir.hidden-item");

        sechematicSize = Core.settings.getInt("hao1337.sechematic.size");
        experimental = Core.settings.getBool("hao1337.gameplay.experimental");
    }

    public void applyState(boolean shouldEnable) {
        SlagCentrifuge.apply(shouldEnable && slagCentrifuge);
        HeatReactor.apply(shouldEnable && heatReactor);
        Vault.apply(shouldEnable && betterSerpuloVault, shouldEnable && betterErekirVault);
        OverrideDome.apply(shouldEnable && betterOverrideDome);
        ForceProjector.apply(shouldEnable && betterShield);
        Liquids.apply(shouldEnable && !hiddenLiquids);
        hao1337.modification.Items.apply(shouldEnable && !hiddenItems);

        HaoBlocks.armoredThoriumConveyor.buildVisibility =
        HaoBlocks.thoriumConveyor.buildVisibility = check(shouldEnable, thoriumConveyor);
        HaoBlocks.surgeConveyor.buildVisibility =
        HaoBlocks.armoredSurgeConveyor.buildVisibility = check(shouldEnable, surgeConveyor);
        HaoBlocks.box.buildVisibility = check(shouldEnable, box);
        HaoBlocks.silo.buildVisibility = check(shouldEnable, silo);
        HaoBlocks.ultraVault.buildVisibility = check(shouldEnable, ultraVault);
        HaoBlocks.valveUnloader.buildVisibility =  check(shouldEnable, valveUnloader);
        HaoBlocks.leviathanReconstructor.buildVisibility = check(shouldEnable, leviathanReconstructor);
        HaoBlocks.m1014.buildVisibility = check(shouldEnable, m1014);
        HaoBlocks.dropper.buildVisibility = check(shouldEnable, dropper);
        HaoBlocks.giganticDome.buildVisibility = check(shouldEnable, giganticDome);
        HaoBlocks.noConnectContainer.buildVisibility = check(shouldEnable, noConnectContainer);
        
        Vars.maxSchematicSize = sechematicSize;
        try {
            // It also gone!
            Vars.experimental = experimental;
        } catch (Throwable ex) {}
    }

    BuildVisibility check(boolean shouldEnable, boolean state) {
        return (shouldEnable && !state) ? BuildVisibility.shown : BuildVisibility.hidden;
    }

    public void techTree() {
        TechTreeModification.margeNode(Blocks.phaseHeater, Blocks.heatReactor, ItemStack.with(Items.beryllium, 2000, Items.oxide, 1500, Items.silicon, 3000), Seq.with(new Objectives.SectorComplete(SectorPresets.stronghold)));
        // TechTreeModification.margeNode(Blocks.vault, HaoBlocks.noConnectContainer, ItemStack.with(Items.copper, 20000, Items.graphite, 15000, Items.silicon, 30000, Items.titanium, 10000, Items.thorium, 5000), Seq.with(new Objectives.SectorComplete(SectorPresets.impact0078)));
        TechTreeModification.margeNodeProduce(Items.thorium, Items.fissileMatter, 1);
        TechTreeModification.margeNodeProduce(Items.fissileMatter, HaoItems.uranium, 0);
    }

    public void netWorking() {
        Vars.net.handleClient(ModStatePackage.class, new Cons<ModStatePackage>() {
            @Override
            public void get(ModStatePackage pack) {
                thoriumConveyor = pack.thoriumConveyor;
                surgeConveyor = pack.surgeConveyor;
                box = pack.box;
                silo = pack.silo;
                ultraVault = pack.ultraVault;
                valveUnloader = pack.valveUnloader;
                leviathanReconstructor = pack.leviathanReconstructor;
                dropper = pack.dropper;
                m1014 = pack.m1014;
                giganticDome = pack.giganticDome;
                slagCentrifuge = pack.slagCentrifuge;
                heatReactor = pack.heatReactor;
                betterShield = pack.betterShield;
                betterOverrideDome = pack.betterOverrideDome;
                betterSerpuloVault = pack.betterSerpuloVault;
                betterErekirVault = pack.betterErekirVault;
                hiddenLiquids = pack.hiddenLiquids;
                hiddenItems = pack.hiddenItems;
                sechematicSize = pack.sechematicSize;
                experimental = pack.experimental;
            }
        });

        Events.on(PlayerJoin.class, t -> {
            ModStatePackage pack = new ModStatePackage();

            pack.thoriumConveyor = thoriumConveyor;
            pack.surgeConveyor = surgeConveyor;
            pack.box = box;
            pack.silo = silo;
            pack.ultraVault = ultraVault;
            pack.valveUnloader = valveUnloader;
            pack.leviathanReconstructor = leviathanReconstructor;
            pack.dropper = dropper;
            pack.m1014 = m1014;
            pack.giganticDome = giganticDome;
            pack.slagCentrifuge = slagCentrifuge;
            pack.heatReactor = heatReactor;
            pack.betterShield = betterShield;
            pack.betterOverrideDome = betterOverrideDome;
            pack.betterSerpuloVault = betterSerpuloVault;
            pack.betterErekirVault = betterErekirVault;
            pack.hiddenLiquids = hiddenLiquids;
            pack.hiddenItems = hiddenItems;
            pack.sechematicSize = sechematicSize;
            pack.experimental = experimental;

            Core.app.post(() -> t.player.con.send(pack, true));
        });
    }
}
