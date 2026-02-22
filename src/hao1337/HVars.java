package hao1337;

import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Nullable;
import arc.scene.Group;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import hao1337.contents.HBlocks;
import hao1337.contents.HItems;
import hao1337.contents.HUnits;
import hao1337.net.Protocol;
import hao1337.ui.*;

public class HVars {
    /** Mod version */
    public static final String version = "1.8.2";
    /** Github API url to this mod repo */
    public static final String gitapi = "https://api.github.com/repos/Hao-1337/mindustry-better-vanilla/releases";
    /** Github repo name */
    public static final String repoName = "hao1337/mindustry-better-vanilla";
    /** Mod name that will get use as id in game */
    public static final String name = "hao1337-mod";
    /** Zip file name (using for auto update if user already unzip the mod file) */
    public static final String unzipName = "hao1337mindustry-better-vanilla";

    /** Unique instance of units counter UI */
    public static UnitsDisplay unitDisplay = new UnitsDisplay();
    /** Unique instance of core items UI */
    public static CoreItemsDisplay coreitemDisplay = new CoreItemsDisplay();
    /** Unique instace of settings UI */
    public static final SettingBuilder setting = new SettingBuilder();
    /** Control how mod block should be visible in different server */
    public static final ModState modState = new ModState();
    /** Unique instance of time control UI */
    public static final TimeControl timecontrol = new TimeControl();
    /** Internal mod networking */
    public static final hao1337.net.Net net = new hao1337.net.Net();

    /** Net channel for mod state */
    public static final short modStateNetChannel = 23554;
    /** Net channel for time control UI state */
    public static final short tcNetChannel = 23555;

    public HVars() {
        Htex.load(name);
        eventsLoader();
    }

    void eventsLoader() {
        Events.on(WorldLoadEndEvent.class, e -> {
            // Log.info("World load event");
            unitDisplay.resetUsed();
            coreitemDisplay.resetUsed();
        });

        Events.on(Protocol.ProtocolChange.class,  e -> {
            var auto = hao1337.net.Net.protocol.serverIsCompatible();
            modState.applyState(auto);
            timecontrol.useable = auto;
        });

        Events.on(ClientLoadEvent.class, e -> {
            contentsLoader();
            UILoader();
        });
    }

    void contentsLoader() {
        HItems.load();
        HBlocks.load();
        HUnits.load();
        modState.load();
        modState.techTree();
    }
    
    void UILoader() {
        unitDisplay.name = "unit-display";
        coreitemDisplay.name = "coreitem-display";

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
        // If failed to get mobile layout then go back to desktop one
        if (Vars.mobile) {
            @Nullable TextButton button = hud.find(e -> e instanceof TextButton btn && btn.getChildren().contains(t -> t instanceof Label l && l.toString().contains(getText("@command.queue"))));

            if (button != null && button.parent instanceof Table t && t.parent != null) {
                hud = (WidgetGroup) t;
            }
        }

        hud.fill(t -> {
            t.bottom().left();
            t.name = "Hao137 TimeControl";
            // t.table(Tex.pane, e -> AutoDrill.register(e));
            // t.row();
            t.table(null, e -> e.top().left().collapser(timecontrol, () -> Core.settings.getBool("hao1337.ui.timecontrol.enable")));
            if (Vars.mobile)
                t.moveBy(0, Scl.scl(46));
        });
    }

    String getText(String newText){
        if (Core.bundle != null && newText != null && newText.length() > 0 && (newText.charAt(0) == '$' || newText.charAt(0) == '@')) {
            String out = newText.toString().substring(1);
            return Core.bundle.get(out, newText.toString());
        } else {
            return newText;
        }
    }
}
