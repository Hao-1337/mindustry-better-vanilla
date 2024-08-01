package hao1337;

import arc.util.Log;
import hao1337.content.blocks.HaoBlocks;
import hao1337.modification.ForceProjector;
import hao1337.modification.OverrideDome;
import hao1337.modification.ScrapWall;
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
        HaoBlocks.armoredSurgeConveyor.buildVisibility =
        HaoBlocks.thoriumConveyor.buildVisibility = 
        HaoBlocks.box.buildVisibility =
        HaoBlocks.silo.buildVisibility =
        HaoBlocks.ultraVault.buildVisibility =
        HaoBlocks.surgeConveyor.buildVisibility =
        HaoBlocks.valveUnloader.buildVisibility =
        HaoBlocks.leviathanReconstructor.buildVisibility =
        HaoBlocks.giganticDome.buildVisibility =
        HaoBlocks.armoredThoriumConveyor.buildVisibility = enable ? BuildVisibility.shown : BuildVisibility.hidden;
    }
}
