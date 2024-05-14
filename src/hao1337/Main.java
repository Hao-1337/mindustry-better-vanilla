package hao1337;

import arc.*;
import arc.scene.Group;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.*;
import hao1337.ui.*;
import hao1337.modification.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.mod.Mod;

public class Main extends Mod {
    public static final String version = "1.2.2";
    public static final String gitapi = "https://api.github.com/repos/Hao-1337/mindustry-better-vanilla/releases/latest";
    public static final String repoName = "hao1337/mindustry-better-vanilla";
    public static final String name = "hao1337-mod";
    public static final String unzipName = "hao1337mindustry-better-vanilla";

    private UnitsDisplay unitDisplay = new UnitsDisplay();
    private CoreItemsDisplay coreitemDisplay = new CoreItemsDisplay();
    public SettingBuilder setting = new SettingBuilder();
    public TimeControl timecontrol;

    public Main() {
        unitDisplay.name = "unit-info";
        coreitemDisplay.name = "item-info";

        Events.on(ClientLoadEvent.class, e -> {
            load();
        });
        Events.on(WorldLoadEvent.class, e -> {
            unitDisplay.resetUsed();
            coreitemDisplay.resetUsed();
        });
    }

    public void load() {
        Log.info("[Hao1337: Better Vanilla] is launching.");

        unitDisplay.name = "unit-display";
        coreitemDisplay.name = "coreitem-display";
        timecontrol = new TimeControl();

        timecontrol.build();
        setting.build();
        loadUI();

        OverrideDome.load();
        Vault.load();
        ForceProjector.load();

        if (Core.settings.getBool("hao1337.toggle.autoupdate")) {
          AutoUpdate.load(gitapi, name, repoName, unzipName, version);
          AutoUpdate.check();
        }
    }
    public void loadUI() {
        Group hud = Vars.ui.hudGroup;
        WidgetGroup coreinfo = (WidgetGroup)hud.find("coreinfo");
        WidgetGroup top = (WidgetGroup)coreinfo.getChildren().get(1);

        top.name = "modification";

        // Remove original core items display
        top.getChildren().get(0).remove();
        //Add core items display and unit display
        ((Table)coreinfo.find("modification")).table(t -> {
            t.top().center();
            t.name = "Hao1337 UI";
            
            t.collapser(coreitemDisplay, () -> Core.settings.getBool("hao1337.ui.coreinf.enable") && (!Vars.ui.hudfrag.shown || !Vars.ui.minimapfrag.shown())).top();
            t.collapser(unitDisplay, () -> Core.settings.getBool("hao1337.ui.unitinf.enable") && (!Vars.ui.hudfrag.shown || !Vars.ui.minimapfrag.shown())).top();
        });
        //Add time control
        hud.fill(t -> {
           t.bottom().left();
           t.name = "Hao137 TimeControl";

           t.table(null, e -> e.top().left().collapser(timecontrol, () -> true));
           if (Vars.mobile) t.moveBy(0, Scl.scl(46));
        });
    }
}