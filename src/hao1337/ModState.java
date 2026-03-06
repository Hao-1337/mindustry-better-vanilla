package hao1337;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import hao1337.contents.HBlocks;
import hao1337.contents.HItems;
import hao1337.modification.ForceProjector;
import hao1337.modification.HeatReactor;
import hao1337.modification.Liquids;
import hao1337.modification.OverrideDome;
import hao1337.modification.SlagCentrifuge;
import hao1337.modification.TechTreeModification;
import hao1337.modification.Vault;
import hao1337.net.IORouter;
import hao1337.net.Protocol.PlayerAuthSuccess;

import static hao1337.net.Net.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.SectorPresets;
import mindustry.game.Objectives;
import mindustry.net.NetConnection;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;
import arc.util.io.Reads;
import arc.util.io.Writes;

/**
 * ModState manages the configuration state for the Mindustry Better Vanilla mod.
 * 
 * <p>This class handles:
 * <ul>
 * <li>Loading and storing mod feature toggle states from game settings</li>
 * <li>Applying feature states to modify block visibility and game mechanics</li>
 * <li>Synchronizing mod state between server and client via networking</li>
 * <li>Managing technology tree modifications</li>
 * </ul>
 * 
 * <p>The class automatically registers network handlers on instantiation to synchronize state 
 * between clients and server, ensuring all players have consistent mod configurations.
 */
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

    public ModState() { netWorking(); }

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

        HBlocks.armoredThoriumConveyor.buildVisibility =
        HBlocks.thoriumConveyor.buildVisibility = check(shouldEnable, thoriumConveyor);
        HBlocks.surgeConveyor.buildVisibility =
        HBlocks.armoredSurgeConveyor.buildVisibility = check(shouldEnable, surgeConveyor);
        HBlocks.box.buildVisibility = check(shouldEnable, box);
        HBlocks.silo.buildVisibility = check(shouldEnable, silo);
        HBlocks.ultraVault.buildVisibility = check(shouldEnable, ultraVault);
        HBlocks.valveUnloader.buildVisibility =  check(shouldEnable, valveUnloader);
        HBlocks.leviathanReconstructor.buildVisibility = check(shouldEnable, leviathanReconstructor);
        HBlocks.m1014.buildVisibility = check(shouldEnable, m1014);
        HBlocks.dropper.buildVisibility = check(shouldEnable, dropper);
        HBlocks.giganticDome.buildVisibility = check(shouldEnable, giganticDome);
        HBlocks.noConnectContainer.buildVisibility = check(shouldEnable, noConnectContainer);
        
        Vars.maxSchematicSize = sechematicSize;
        try {
            // It also gone!
            // Vars.experimental = experimental;
        } catch (Throwable ex) {}
    }

    BuildVisibility check(boolean shouldEnable, boolean state) {
        return (shouldEnable && !state) ? BuildVisibility.shown : BuildVisibility.hidden;
    }

    public void techTree() {
        TechTreeModification.margeNode(Blocks.phaseHeater, Blocks.heatReactor, ItemStack.with(Items.beryllium, 2000, Items.oxide, 1500, Items.silicon, 3000), Seq.with(new Objectives.SectorComplete(SectorPresets.stronghold)));
        // TechTreeModification.margeNode(Blocks.vault, HaoBlocks.noConnectContainer, ItemStack.with(Items.copper, 20000, Items.graphite, 15000, Items.silicon, 30000, Items.titanium, 10000, Items.thorium, 5000), Seq.with(new Objectives.SectorComplete(SectorPresets.impact0078)));
        TechTreeModification.margeNodeProduce(Items.thorium, Items.fissileMatter, 1);
        TechTreeModification.margeNodeProduce(Items.fissileMatter, HItems.uranium, 0);
    }

    public void netWorking() {
        router.register(HVars.modStateNetChannel, new IORouter.ChannelHandler() {
            public void handleClient(byte[] payload) {
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                thoriumConveyor = r.bool();
                surgeConveyor = r.bool();
                box = r.bool();
                silo = r.bool();
                ultraVault = r.bool();
                valveUnloader = r.bool();
                leviathanReconstructor = r.bool();
                dropper = r.bool();
                m1014 = r.bool();
                giganticDome = r.bool();
                slagCentrifuge = r.bool();
                heatReactor = r.bool();
                betterShield = r.bool();
                betterOverrideDome = r.bool();
                betterSerpuloVault = r.bool();
                betterErekirVault = r.bool();
                hiddenLiquids = r.bool();
                hiddenItems = r.bool();
                sechematicSize = r.i();
                experimental = r.bool();

                r.close();
            }

            public void handleServer(NetConnection connection, byte[] payload) {}
        });

        Events.on(PlayerAuthSuccess.class, t -> {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream s = new DataOutputStream(bos);
            Writes w = new Writes(s);

            w.bool(thoriumConveyor);
            w.bool(surgeConveyor);
            w.bool(box);
            w.bool(silo);
            w.bool(ultraVault);
            w.bool(valveUnloader);
            w.bool(leviathanReconstructor);
            w.bool(dropper);
            w.bool(m1014);
            w.bool(giganticDome);
            w.bool(slagCentrifuge);
            w.bool(heatReactor);
            w.bool(betterShield);
            w.bool(betterOverrideDome);
            w.bool(betterSerpuloVault);
            w.bool(betterErekirVault);
            w.bool(hiddenLiquids);
            w.bool(hiddenItems);
            w.i(sechematicSize);
            w.bool(experimental);

            var auto = bos.toByteArray();
            w.close();
            router.sendTo(t.connection, HVars.modStateNetChannel, auto);
        });
    }
}
