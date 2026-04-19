package hao1337.addons.autodrill.ui;

import arc.Core;
import arc.Events;
import arc.func.Boolp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
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
import mindustry.graphics.Drawf;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.production.BeamDrill;

public class AutoDrill {
    public static final float buttonSize = Scl.scl(25f);
    public static final boolean heuristicPathFinder = false;

    final public DrillTable selectTable = new DrillTable();
    final public DirectionTable directionTable = new DirectionTable();
    final public Table uiTable = new Table((Drawable) null);
    public Boolp optionalCondition = () -> false;

    Tile selectedTile;
    Block selectDrill;
    ItemBridge bridge = (ItemBridge)Blocks.phaseConveyor;
    Direction outputDirection;
    boolean toggled = false;

    public void buildTable() {
        uiTable.margin(2f);
        uiTable.name = "hao137-auto-drill";
        uiTable.background(Htex.paneTopRight);

        var toggle = new TextButton("Auto Drill", Styles.flatToggleMenut);
        toggle.update(() -> toggle.setChecked(toggled));
        toggle.changed(() -> {
            toggled = toggle.isChecked();
            if (!toggled) {
                reset();
            }
        });
    
        uiTable.add(toggle).growX().height(48f);

        uiTable.update(() -> {
            if (toggled && selectedTile != null) {
                Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize, (selectedTile.centerY() + 1) * Vars.tilesize);
                selectTable.setPosition(v.x, v.y, Align.bottom);
                directionTable.setPosition(v.x, v.y, Align.bottom);
            }

            if (selectedTile != null) {
                var oldZ = Draw.z();
                Draw.z(oldZ + 20);
                Drawf.selected(selectedTile, Color.valueOf("aae5a4"));
                Draw.z(oldZ);
            }
        });

        uiTable.visible(() -> !optionalCondition.get() && Vars.ui.hudfrag.shown && !Vars.ui.minimapfrag.shown() && Core.settings.getBool("hao1337.ui.autodrill.enable", false));
    }

    void showOptionFragment() {
        // var frag = Vars.ui.hudfrag.blockfrag;
        
    }

    public void register() {
        Core.scene.root.addChildAt(0, selectTable);
        Core.scene.root.addChildAt(0, directionTable);

        Events.on(ResetEvent.class, e -> {
            reset();
        });

        Events.on(EventType.TapEvent.class, e -> {
            if (e.tile != null && e.tile.build == null && toggled) {
                selectedTile = e.tile;
                selectTable.reset();
                selectTable.build(selectedTile, this::onDrillSelected);

                Fx.tapBlock.at(selectedTile.getX(), selectedTile.getY());
            }
            else if (e.tile.build != null) {
                reset();
            }
        });
    }

    void reset() {
        toggled = false;
        selectedTile = null;
        selectDrill = null;
        selectTable.reset();
        directionTable.reset();
    }

    void onDrillSelected(@Nullable Block drill) {
        selectTable.reset();
        if (drill == null) {
            reset();
            return;
        }
        selectDrill = drill;
        directionTable.build(this::onDirectionSelected);
    }

    void onDirectionSelected(Direction direction) {
        reset();
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


