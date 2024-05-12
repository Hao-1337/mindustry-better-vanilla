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

public class UnitsDisplay extends Table {
    private final ObjectSet<UnitType> units = new ObjectSet<>();
    private final ObjectMap<UnitType, Integer> cap = new ObjectMap<>();
    private Team playerTeam;
    private Teams.TeamData teamData;
    private final Interval interval = new Interval();

    public int time = 80;

    public UnitsDisplay() {
        rebuild();
    }

    public void resetUsed() {
        units.clear();
        cap.clear();
        rebuild();
    }

    void rebuild() {
        clear();
        background(null);
        if (units.size < 0) return;

        background(Styles.black6);
        margin(4);
        buildUI();

        update(() -> {
            if (!interval.get(time))
                return;
            fetchUnit();
        });
    }

    private void fetchUnit() {
        units.clear();
        playerTeam = Vars.player.team();
        teamData = playerTeam.data();

        for (UnitType unit : content.units()) {
            if (teamData.countType(unit) > 0) {
                units.add(unit);
            }
        }
        rebuild();
    }

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
