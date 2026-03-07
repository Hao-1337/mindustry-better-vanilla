package hao1337;

import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Spacer;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Nullable;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.style.Drawable;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.input.MobileInput;
import hao1337.addons.autodrill.ui.AutoDrill;
import hao1337.contents.HBlocks;
import hao1337.contents.HItems;
import hao1337.contents.HUnits;
import hao1337.net.Protocol;
import hao1337.ui.*;

public class HVars {
    /** Mod version */
    public static final String version = "1.8.5";
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
    /** Auto drill addons */
    public static final AutoDrill autoDrill = new AutoDrill();

    /** Net channel for mod state */
    public static final short modStateNetChannel = 23554;
    /** Net channel for time control UI state */
    public static final short tcNetChannel = 23555;

    public static final boolean isSteam = Vars.steam;
    public static final boolean isBeta = true;

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
        autoDrill.register();
        HItems.load();
        HBlocks.load();
        HUnits.load();
        modState.load();
        modState.techTree();
    }
    
    private @Nullable TextButton toggleSeqButton = null;

    void UILoader() {
        unitDisplay.name = "unit-display";
        coreitemDisplay.name = "coreitem-display";

        timecontrol.rebuild();
        setting.build();
        autoDrill.buildTable();

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
        if (Vars.mobile && Vars.control.input instanceof MobileInput control) {
            @Nullable TextButton button = getButtonByI18NText(hud, "@command.queue");

            if (button != null && button.parent instanceof Table t && t.parent != null) {
                var parent = t.parent;
                var firstChild = parent.getChildren().get(0);
                Table tcTable = new Table();

                for (Element e : parent.getChildren()) {
                    if (e instanceof Table w) {
                        // remove any default spacer first!
                        boolean haveDefaultSpacer = false;
                        for (Element e1 : w.getChildren())
                            if (e1 instanceof Spacer) {
                                w.removeChild(e1);
                                haveDefaultSpacer = true;
                            }

                        if (haveDefaultSpacer) w.spacerY(() -> (control.showCancel() ? 149.5f : 98f) - (tcTable.visible ? 0f : 100f) + (autoDrill.uiTable.visible ? 47.5f : 0f));
                        if (getButtonByI18NText(w, "@cancel") != null) w.spacerY(() -> (tcTable.visible ? 249f : autoDrill.uiTable.visible ? 50f : 0f) + (autoDrill.uiTable.visible ? 96f : 0f));
                        var btn = getButtonByI18NText(w, "@command.queue");
                        if (btn != null) toggleSeqButton = btn;
                    }
                };

                tcTable.name = "Hao137 TimeControl";
                tcTable.bottom().left();
                tcTable.setFillParent(true);
                tcTable.visible(() -> Core.settings.getBool("hao1337.ui.timecontrol.enable"));
                tcTable.add(timecontrol).width(155f).height(100f);
                tcTable.row();

                parent.addChildBefore(firstChild, tcTable);

                Table autoDrillTable = new Table((Drawable) null);
                autoDrillTable.bottom().left();
                autoDrillTable.setFillParent(true);
                autoDrillTable.add(autoDrill.uiTable).width(155f);
                autoDrillTable.spacerY(() -> tcTable.visible ? 248f : 0f);
    
                parent.addChildBefore(firstChild, autoDrillTable);

                return;
            }
        }

        hud.fill(t -> {
            t.bottom().left();
            t.name = "Hao137 TimeControl";
            t.add(autoDrill.uiTable).width(158f);
            t.row();
            t.collapser(timecontrol, () -> Core.settings.getBool("hao1337.ui.timecontrol.enable"));
            if (Vars.mobile)
                t.moveBy(0, Scl.scl(46));
        });
    }

    @Nullable TextButton getButtonByI18NText(Group elementGroup, String name) {
        if (Core.bundle != null && name != null && name.length() > 0 && (name.charAt(0) == '$' || name.charAt(0) == '@')) {
            String out = name.toString().substring(1);
            String newName = Core.bundle.get(out, name.toString());

            return elementGroup.find(e -> e instanceof TextButton btn && btn.getChildren().contains(t -> t instanceof Label l && l.toString().contains(newName)));
        }

        return elementGroup.find(e -> e instanceof TextButton btn && btn.getChildren().contains(t -> t instanceof Label l && l.toString().contains(name)));
    }
}
