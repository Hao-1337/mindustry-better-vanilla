package hao1337;

import arc.*;
import arc.scene.Group;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.*;
import hao1337.ui.*;
import hao1337.content.blocks.HaoBlocks;
import hao1337.content.items.HaoItems;
import hao1337.content.units.HaoUnits;
import hao1337.net.HaoNetPackage;
import hao1337.net.HaoNetPackageClient;
import hao1337.net.Server;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.mod.Mod;
import mindustry.net.Net;

public class Main extends Mod {
    public static final String version = "1.7.2";
    public static final String gitapi = "https://api.github.com/repos/Hao-1337/mindustry-better-vanilla/releases";
    public static final String repoName = "hao1337/mindustry-better-vanilla";
    public static final String name = "hao1337-mod";
    public static final String unzipName = "hao1337mindustry-better-vanilla";

    private UnitsDisplay unitDisplay = new UnitsDisplay();
    private CoreItemsDisplay coreitemDisplay = new CoreItemsDisplay();
    public SettingBuilder setting = new SettingBuilder();
    public Loader mod = new Loader();
    public TimeControl timecontrol;

    public Main() {
        Events.on(WorldLoadEvent.class, e -> {
            unitDisplay.resetUsed();
            coreitemDisplay.resetUsed();
        });
        Events.on(ClientServerConnectEvent.class, e -> {
            timecontrol.rebuild();
            mod.updateState(true);
        });
        Events.on(ClientLoadEvent.class, e -> {
            Net.registerPacket(HaoNetPackage::new);
            Net.registerPacket(HaoNetPackageClient::new);
            Server.load();
            Init();
        });
        Events.on(Server.ServerStateChange.class, s -> {
            timecontrol.useable = s.hasThisMod;
            timecontrol.rebuild();
        });
    }

    public void Init() {
        Log.info("[Hao1337: Better Vanilla] is launching.");
        AutoUpdate.load(gitapi, name, repoName, unzipName, version);

        if (Core.settings.getBool("hao1337.toggle.autoupdate")) {
            AutoUpdate.check();
        }

        loadUI();
        Server.interval();
        mod.load();
        HaoItems.load();
        HaoBlocks.load();
        HaoUnits.load();
        Loader.generateContent();

        Vars.maxSchematicSize = Core.settings.getInt("hao1337.sechematic.size");
        Vars.experimental = Core.settings.getBool("hao1337.gameplay.experimental");
    }

    public void loadUI() {
        unitDisplay.name = "unit-display";
        coreitemDisplay.name = "coreitem-display";
        timecontrol = new TimeControl();

        timecontrol.rebuild();
        setting.build();

        Group hud = Vars.ui.hudGroup;
        WidgetGroup coreinfo = (WidgetGroup) hud.find("coreinfo");
        WidgetGroup top = (WidgetGroup) coreinfo.getChildren().get(1);

        top.name = "modification";

        // Remove original core items display
        top.getChildren().get(0).remove();
        // Add core items display and unit display
        ((Table) coreinfo.find("modification")).table(t -> {
            t.top().center();
            t.name = "Hao1337 UI";

            t.collapser(coreitemDisplay, () -> Core.settings.getBool("hao1337.ui.coreinf.enable")
                    && (!Vars.ui.hudfrag.shown || !Vars.ui.minimapfrag.shown())).top();
            t.collapser(unitDisplay, () -> Core.settings.getBool("hao1337.ui.unitinf.enable")
                    && (!Vars.ui.hudfrag.shown || !Vars.ui.minimapfrag.shown())).top();
        });
        // Add time control
        hud.fill(t -> {
            t.bottom().left();
            t.name = "Hao137 TimeControl";

            t.table(null, e -> e.top().left().collapser(timecontrol, () -> Core.settings.getBool("hao1337.ui.timecontrol.enable")));
            if (Vars.mobile)
                t.moveBy(0, Scl.scl(46));
        });
    }
}
