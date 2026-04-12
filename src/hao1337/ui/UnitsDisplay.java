package hao1337.ui;

import static mindustry.Vars.content;
import static mindustry.Vars.iconSmall;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.type.UnitType;
import mindustry.ui.Styles;

/**
 * HUD widget showing the current units owned by the player&#39;s team along
 * with per-type counts and recent changes. The display updates on an
 * {@link Interval} tick to avoid querying every frame.
 */
public class UnitsDisplay extends Table {
    /** set of unit types currently present for the team. */
    private final ObjectSet<UnitType> units = new ObjectSet<>();
    /** mapping from unit type to previous tick count used for delta calculation. */
    private final ObjectMap<UnitType, Integer> cap = new ObjectMap<>();
    private Team playerTeam;
    private Teams.TeamData teamData;
    /** timer used to throttle updates. */
    private final Interval interval = new Interval();

    /**
     * tick interval in frames (approximate). 64 frames equals one second at
     * 60Hz.
     */
    public int time = 80;

    /**
     * Create the component and build its initial layout.
     */
    public UnitsDisplay() {
        rebuild();
    }

    /**
     * Clear stored counts and force a full rebuild.  This is useful when the
     * team has been reset (e.g. after a wave starts).
     */
    public void resetUsed() {
        units.clear();
        cap.clear();
        rebuild();
    }

    /**
     * Reconstruct the table contents and schedule periodic updates.  Called
     * whenever the set of tracked unit types may have changed.
     */
    void rebuild() {
        clear();
        background(null);
        if (units.size < 0) return;

        background(Styles.black6);
        margin(4);
        

        update(() -> {
            if (!interval.get(time))
                return;
            clearChildren();
            fetchUnit();
            buildUI();
            visible = units.size > 0;
        });
    }

    /**
     * Query the current team data and update the {@code units} set with
     * all types that have at least one instance.  Also triggers a layout
     * rebuild to reflect any changes.
     */
    private void fetchUnit() {
        units.clear();
        playerTeam = Vars.player.team();
        teamData = playerTeam.data();

        for (UnitType unit : content.units()) {
            if (teamData.countType(unit) > 0) {
                units.add(unit);
            }
        }
    }

    /**
     * Build the visible rows of the table based on the current {@code units}
     * set.  For each type present the method displays the icon, count, and
     * change since the last update.
     */
    private void buildUI() {
        int i = 0;
        for (UnitType unit : content.units()) {
            if (units.contains(unit)) {
                int current = teamData.countType(unit);
                int older = cap.get(unit, current);
                int balance = current - older;

                cap.put(unit, current);

                image(unit.uiIcon).size(iconSmall)
                        .padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f)
                        .add(unit.localizedName).style(Styles.outlineLabel));

                label(() -> UI.formatAmount(current) +
                        (balance > 0 ? " [green]+" : balance < 0 ? " [red]" : "[]") +
                        (balance != 0 ? UI.formatAmount(balance) : "")).padRight(3).minWidth(52f).left().get().hovered(() -> {});

                if (++i % 4 == 0) {
                    row();
                }
            }
        }
    }
}
