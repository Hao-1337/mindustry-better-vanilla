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
import arc.util.Log;
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
    public static final int heuristicBatchBudget = 8;
    public static final String planningBundleKey = "hao1337.ui.addon.autodrill.planning";

    final public DrillTable selectTable = new DrillTable();
    final public DirectionTable directionTable = new DirectionTable();
    final public Table uiTable = new Table((Drawable) null);
    public Boolp optionalCondition = () -> false;

    Tile selectedTile;
    Block selectDrill;
    ItemBridge bridge = (ItemBridge)Blocks.itemBridge;
    Direction outputDirection;
    BatchProcessing runner = new BatchProcessing();
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
            runner.advance();
            if (runner.running()) Log.info("Batch process: @", runner.process());

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
        // TODO it should stop on reset?
        // runner.stop();
        toggled = false;
        selectedTile = null;
        selectDrill = null;
        outputDirection = null;
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

    synchronized void onDirectionSelected(Direction direction) {
        toggled = false;
        selectTable.reset();
        directionTable.reset();

        if (direction == null) return;

        outputDirection = direction;

        if (selectDrill instanceof @SuppressWarnings("unused") BeamDrill beamDrill) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        //   E  FATAL EXCEPTION: GLThread 637
        //     Process: io.anuke.mindustry, PID: 7853
        //     java.lang.NullPointerException: Attempt to read from field 'int mindustry.world.Block.size' on a null object reference in method 'void hao1337.addons.autodrill.ui.AutoDrill.onDirectionSelected(hao1337.addons.autodrill.HeuristicGroundOrePF$Direction)'
        //     	at hao1337.addons.autodrill.ui.AutoDrill.onDirectionSelected(AutoDrill.java:138)
        //     	at hao1337.addons.autodrill.ui.AutoDrill$6.get(D8$$SyntheticClass:0)
        //     	at hao1337.addons.autodrill.ui.DirectionTable.lambda$build$3$hao1337-addons-autodrill-ui-DirectionTable(DirectionTable.java:76)
        //     	at hao1337.addons.autodrill.ui.DirectionTable$5.run(D8$$SyntheticClass:0)
        //     	at arc.scene.Element.lambda$clicked$2(Element.java:1)
        //     	at arc.scene.Element.$r8$lambda$HTGgyzI_sft57MMc4a-IcKQNKts(Element.java:1)
        //     	at arc.Events$$ExternalSyntheticLambda1.get(R8$$SyntheticClass:34)
        //     	at arc.scene.Element$4.clicked(Element.java:21)
        //     	at arc.scene.event.ClickListener.touchUp(ClickListener.java:58)
        //     	at arc.scene.event.InputListener.handle(InputListener.java:116)
        //     	at arc.scene.Scene.touchUp(Scene.java:106)
        //     	at arc.input.InputMultiplexer.touchUp(InputMultiplexer.java:19)
        //     	at arc.backend.android.AndroidInput.processEvents(AndroidInput.java:139)
        //     	at arc.backend.android.AndroidGraphics.onDrawFrame(AndroidGraphics.java:99)
        //     	at android.opengl.GLSurfaceView$GLThread.guardedRun(GLSurfaceView.java:1577)
        //     	at android.opengl.GLSurfaceView$GLThread.run(GLSurfaceView.java:1276)

        try {
            GroundOrePathFinding algorithm = heuristicPathFinder || selectDrill.size >= 3
                ? new HeuristicGroundOrePF(selectDrill, selectedTile, outputDirection, bridge)
                : new BlindGroundOrePF(selectDrill, selectedTile, outputDirection, null);
            runner.start(algorithm);
        } catch (Throwable e) {
            Log.err(e);
        }
    }

    class BatchProcessing {
        GroundOrePathFinding.BuildBatch planningBatch;
        boolean onRunning = false;

        public boolean running() { return onRunning; }
        public float process() { return planningBatch != null ? planningBatch.progress() : 0f; }

        public void start(GroundOrePathFinding algorithm) {
            stop();
        
            GroundOrePathFinding.BuildBatch batch = algorithm.createBuildBatch();
            if (batch == null) {
                finish(algorithm.build());
                return;
            }
        
            onRunning = true;
            planningBatch = batch;
        }

    
        public void advance() {
            if (planningBatch == null) return;
        
            try {
                planningBatch.step(heuristicBatchBudget);
                if (!planningBatch.isDone()) return;
            
                Seq<BuildPlan> result = planningBatch.result();
                stop();
                finish(result);
            } catch (Throwable e) {
                Log.err(e);
                stop();
            }
        }
    
        void finish(Seq<BuildPlan> result) {
            for (BuildPlan plan : result)  Vars.player.unit().addBuild(plan);
        
            selectedTile = null;
            selectDrill = null;
            outputDirection = null;
        }
    
        public void stop() {
            if (planningBatch == null) return;
            planningBatch = null;
            onRunning = false;
        }
    }
}


