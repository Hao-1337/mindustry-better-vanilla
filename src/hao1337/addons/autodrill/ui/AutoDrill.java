package hao1337.addons.autodrill.ui;

import arc.Core;
import arc.Events;
import arc.math.geom.Vec2;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import hao1337.addons.autodrill.BlindGroundOrePF;
import hao1337.addons.autodrill.GroundOrePathFinding;
import hao1337.addons.autodrill.HeuristicGroundOrePF;
import hao1337.addons.autodrill.HeuristicGroundOrePF.Direction;
import hao1337.ui.Htex;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType;
import mindustry.game.EventType.ResetEvent;

import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.production.BeamDrill;

public class AutoDrill {
    public static final float buttonSize = Scl.scl(40f);
    public static final boolean heuristicPathFinder = false;

    final public DrillTable selectTable = new DrillTable();
    final public DirectionTable directionTable = new DirectionTable();

    Tile selectedTile;
    Block selectDrill;
    ItemBridge bridge = (ItemBridge)Blocks.phaseConveyor;
    Direction outputDirection;
    boolean toggled = false;

    public void buildTable(Table uiTable) {
        uiTable.margin(0);
        uiTable.name = "Hao137 AutoDrill";
        uiTable.top();
        uiTable.background(Htex.paneTopRight);

        var toggle = new TextButton("Auto Drill", Styles.flatToggleMenut);
        toggle.update(() -> toggle.setChecked(toggled));
        toggle.changed(() -> {
            toggled = toggle.isChecked();
            if (!toggled) {
                selectTable.reset();
                directionTable.reset();
            }
        });
    
        uiTable.add(toggle).grow().height(48f);

        uiTable.update(() -> {
            if (toggled && selectedTile != null) {
                Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize, (selectedTile.centerY() + 1) * Vars.tilesize);
                selectTable.setPosition(v.x, v.y, Align.bottom);
                directionTable.setPosition(v.x, v.y, Align.bottom);
            }
        });
    }

    public void register() {
        Core.scene.root.addChildAt(0, selectTable);
        Core.scene.root.addChildAt(0, directionTable);

        Events.on(ResetEvent.class, e -> {
            toggled = false;
            selectTable.reset();
            directionTable.reset();
        });

        Events.on(EventType.TapEvent.class, e -> {
            if (e.tile != null && toggled) {
                selectedTile = e.tile;
                selectTable.reset();
                selectTable.build(selectedTile, this::onDrillSelected);

                Fx.tapBlock.at(selectedTile.getX(), selectedTile.getY());
            }
        });
    }

    void onDrillSelected(Block drill) {
        selectDrill = drill;
        selectTable.reset();
        directionTable.build(this::onDirectionSelected);
    }

    void onDirectionSelected(Direction direction) {
        toggled = false;
        directionTable.reset();
        if (direction == null) return;
        outputDirection = direction;

        if (selectDrill instanceof @SuppressWarnings("unused") BeamDrill beamDrill) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        GroundOrePathFinding algorithm = heuristicPathFinder || selectDrill.size >= 3 ?
            new HeuristicGroundOrePF(selectDrill, selectedTile, outputDirection, bridge):
            new BlindGroundOrePF(selectDrill, selectedTile, outputDirection, null);

        Seq<BuildPlan> result = algorithm.build();
        for (var plan : result) Vars.player.unit().addBuild(plan);
    } 
}


