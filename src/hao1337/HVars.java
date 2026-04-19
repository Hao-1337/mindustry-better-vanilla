package hao1337;

import java.lang.reflect.Field;
import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Log;
import arc.util.Nullable;
import arc.scene.Group;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.input.MobileInput;
import hao1337.addins.AutoUpdate;
import hao1337.addins.StateController;
import hao1337.addons.autodrill.ui.AutoDrill;
import hao1337.contents.HBlocks;
import hao1337.contents.HItems;
import hao1337.contents.HUnits;
import hao1337.net.Protocol;
import hao1337.ui.*;

public class HVars {
    /** Unique instance of units counter UI */
    public static UnitsDisplay unitDisplay = new UnitsDisplay();
    /** Unique instance of core items UI */
    public static CoreItemsDisplay coreitemDisplay = new CoreItemsDisplay();
    /** Unique instace of settings UI */
    public static final SettingBuilder setting = new SettingBuilder();
    /** Control how mod block should be visible in different server */
    public static final StateController modState = new StateController();
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
    public static final boolean isBeta = Version.type.equals("bleeding-edge") || Version.build < 0;

    public HVars() {
        Core.settings.defaults(
            "hao1337.ui.autodrill.enable",
            false,
            "hao1337.toggle.autoupdate.prerelease",
            false,
            "hao1337.toggle.autoupdate",
            true
        );

        try {
            Class<?> clazz = Class.forName("hao1337.addins.DevTools");
            clazz.getMethod("init").invoke(null);
        } catch (ClassNotFoundException e) {} catch (Exception e) {
            Log.err(e);
        }

        AutoUpdate.load();
        Htex.load(hao1337.Version.name);

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
                var childs = parent.getChildren();
                var allChild = childs.toArray();
                var lastChild = allChild[childs.size - 1];
                var commandToggle = getButtonByI18NText(parent, "@command.queue");
                if (commandToggle != null) toggleSeqButton = commandToggle;

                parent.clearChildren();
                autoDrill.optionalCondition = control::hasSchematic;

                parent.fill(e -> e.bottom().left().table(mergeTable -> {
                    mergeTable.fill(c -> c.bottom().left().table(t1 -> {
                        t1.add(autoDrill.uiTable).minWidth(155f).fillX();
                        t1.row();
                        t1.spacerY(() -> timecontrol.visible ? 105f : 0f);
                    }));

                    mergeTable.fill(c -> c.bottom().left().table(t1 -> {
                        t1.add(timecontrol).minWidth(155f).growX();
                        t1.row();
                    }));
    
                    for (var c1: allChild) {
                        mergeTable.fill(c -> c.bottom().left().table(t1 -> {
                            t1.add(c1);
                            t1.row();
                            if (lastChild != c1) t1.spacerY(() -> (autoDrill.uiTable.visible ? 51.5f : 0f) + (timecontrol.visible ? 105f : 0f));
                        }));                        
                    }
                }));
                return;
            }
        }

        hud.fill(t -> {
            t.bottom().left();
            t.name = "Hao137 Bottom Left Area";
            t.add(autoDrill.uiTable).minWidth(155f).fillX();
            t.row();
            t.add(timecontrol).minWidth(155f).fillX();
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

    public static @Nullable Object getField(Object obj, String... names) {
        for (String name : names) {
            try {
                Field f = obj.getClass().getField(name);
                return f.get(obj);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        return null;
    }
}
