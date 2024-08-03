package hao1337;

import arc.Core;
import arc.util.Log;
import hao1337.content.blocks.HaoBlocks;
import hao1337.modification.ForceProjector;
import hao1337.modification.HeatReactor;
import hao1337.modification.OverrideDome;
import hao1337.modification.ScrapWall;
import hao1337.modification.SlagCentrifuge;
import hao1337.modification.Vault;
import hao1337.ui.TimeControl;
import mindustry.world.meta.BuildVisibility;

public class Loader {
    public void load() {
        Log.info("[Mod]Load vanilla modification");
        ForceProjector.load();
        OverrideDome.load();
        Vault.load();
        ScrapWall.load();
        HeatReactor.load();
        SlagCentrifuge.load();
    }

    public void updateState(boolean state) {
        Log.info("[Mod]Current state: @", state);

        ForceProjector.apply(state);
        OverrideDome.apply(state);
        Vault.apply(state);
        ScrapWall.apply(state);
        
        TimeControl.enableSpeedUp = state;
        contentSwitcher(state);
    }

    public void contentSwitcher(boolean enable) {
        HaoBlocks.surgeConveyor.buildVisibility =
        HaoBlocks.armoredSurgeConveyor.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.thorium-conveyor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.armoredThoriumConveyor.buildVisibility =
        HaoBlocks.thoriumConveyor.buildVisibility =  enable && Core.settings.getBool("hao1337.gameplay.serpulo.surge-conveyor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.box.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.box") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.silo.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.silo") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.ultraVault.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.ultra-vault") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.valveUnloader.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.valve-unloader") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.leviathanReconstructor.buildVisibility  = enable && Core.settings.getBool("hao1337.gameplay.serpulo.leviathan-reconstructor") ? BuildVisibility.shown : BuildVisibility.hidden;

        HaoBlocks.giganticDome.buildVisibility = enable && Core.settings.getBool("hao1337.gameplay.serpulo.gigantic-dome") ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
