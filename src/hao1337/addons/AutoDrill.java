package hao1337.addons;

// import java.util.InputMismatchException;

import arc.Core;
import arc.Events;
import arc.input.InputProcessor;
import arc.input.KeyCode;
// import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
// import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import hao1337.lib.Direction;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Icon;
// import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;

public class AutoDrill {
    public static int maxTile = 200;
    public static boolean enabled = false;
    public static Tile selectedTile;
    public static boolean isBeamDrill = false;
    public static boolean useWaterExtractor = false;
    public static boolean useWaterPipe = true;
    public static boolean useNittogenPipe = true;

    public static final float buttonSize = 30f;

    private static final Table selectTable = new Table(Styles.none);
    private static final Table directionTable = new Table(Styles.none);
    private static Block selectDrill;
    private static Direction outputDirection;
    
    private static ImageButton
    // Serpulo
    mechanicalDrillButton, pneumaticDrillButton, blastDrillButton, laserDrillButton,
    // Erekir
    plasmaBoreButton, largePlasmaBoreButton, impactDrillButton, eruptionDrillButton;

    public static void buildButton(Table t, Runnable action, Drawable texture, String toolTip, float padLeft, float padRight) {
        t.button(texture, 10f, action)
        .height(40f)
        .padLeft(padLeft)
        .padRight(padRight)
        .tooltip(ht -> ht.background(Styles.black6).add(toolTip)
        .style(Styles.outlineLabel))
        .get().resizeImage(18f);
    }

    public static void register(Table drawTable) {
        buildButton(drawTable, () -> { enabled = !enabled; selectTable.visible = false; directionTable.visible = false; }, new TextureRegionDrawable(Icon.add), "Enable auto drill", 2f, 0f);
        buildButton(drawTable, () -> { useWaterExtractor = !useWaterExtractor; if (useWaterExtractor) useWaterPipe = false; }, new TextureRegionDrawable(Liquids.water.uiIcon), "Water Extractor", 2f, 2f);
        buildButton(drawTable, () -> { useWaterPipe = !useWaterPipe; if (useWaterPipe) useWaterExtractor = false; }, new TextureRegionDrawable(Blocks.waterExtractor.uiIcon), "Water pipe", 0f, 2f);

        buildDirectionTable();
        buildSelectTable();

        Events.on(EventType.TapEvent.class, event -> {
            if (enabled && event.tile != null) {
                selectTable.visible = true;
                selectedTile = event.tile;

                updateSelectTable();

                Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize, (selectedTile.centerY() + 1) * Vars.tilesize);
                selectTable.setPosition(v.x, v.y, Align.bottom);
                directionTable.setPosition(v.x, v.y, Align.bottom);

                Fx.tapBlock.at(selectedTile.getX(), selectedTile.getY());
            }
        });
    }
    
    private static void buildDirectionTable() {
        directionTable.update(() -> {
            if (Vars.state.isMenu()) {
                directionTable.visible = false;
                return;
            }
            Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize, (selectedTile.centerY() + 1) * Vars.tilesize);
            directionTable.setPosition(v.x, v.y, Align.bottom);
        });

        directionTable.table().get().button(Icon.up, Styles.defaulti, () -> {
            outputDirection = Direction.UP;
            onSelected();
            directionTable.visible = false;
        }).get().resizeImage(buttonSize);

        directionTable.row();

        Table row2 = directionTable.table().get();

        row2.button(Icon.left, Styles.defaulti, () -> {
            outputDirection = Direction.LEFT;
            directionTable.visible = false;
            onSelected();
        }).get().resizeImage(buttonSize);

        row2.button(Icon.cancel, Styles.defaulti, () -> {
            directionTable.visible = false;
        }).pad(5f).get().resizeImage(buttonSize);

        row2.button(Icon.right, Styles.defaulti, () -> {
            outputDirection = Direction.RIGHT;
            directionTable.visible = false;
            onSelected();
        }).get().resizeImage(buttonSize);

        directionTable.row();

        directionTable.table().get().button(Icon.down, Styles.defaulti, () -> {
            outputDirection = Direction.DOWN;
            directionTable.visible = false;
            onSelected();
        }).get().resizeImage(buttonSize);

        Core.input.addProcessor(new InputProcessor() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
                if (!directionTable.hasMouse()) directionTable.visible = false;

                return InputProcessor.super.touchDown(screenX, screenY, pointer, button);
            }
        });

        directionTable.pack();
        directionTable.act(0);
        directionTable.visible = false;
        Core.scene.root.addChildAt(1, directionTable);
    }

    private static void buildSelectTable() {
        selectTable.update(() -> {
            if (Vars.state.isMenu()) {
                selectTable.visible = false;
                return;
            }
            Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize, (selectedTile.centerY() + 1) * Vars.tilesize);
            selectTable.setPosition(v.x, v.y, Align.bottom);
        });

        mechanicalDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-mechanical-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.mechanicalDrill;
        }).get();
        mechanicalDrillButton.resizeImage(buttonSize);

        pneumaticDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-pneumatic-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.pneumaticDrill;
        }).get();
        pneumaticDrillButton.resizeImage(buttonSize);

        laserDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-laser-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.laserDrill;
        }).get();
        laserDrillButton.resizeImage(buttonSize);

        blastDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-blast-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.blastDrill;
        }).get();
        blastDrillButton.resizeImage(buttonSize);

        plasmaBoreButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-plasma-bore-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.plasmaBore;
        }).get();
        plasmaBoreButton.resizeImage(buttonSize);

        largePlasmaBoreButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-large-plasma-bore-full")), Styles.defaulti, () -> {
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.largePlasmaBore;
        }).get();
        largePlasmaBoreButton.resizeImage(buttonSize);

        impactDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-impact-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.impactDrill;
        }).get();
        impactDrillButton.resizeImage(buttonSize);

        eruptionDrillButton = selectTable.button(new TextureRegionDrawable(Core.atlas.find("block-eruption-drill-full")), Styles.defaulti, () -> {
            enabled = false;
            selectTable.visible = false;
            directionTable.visible = true;
            selectDrill = Blocks.eruptionDrill;
        }).get();
        eruptionDrillButton.resizeImage(buttonSize);

        Core.input.addProcessor(new InputProcessor() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
                if (!selectTable.hasMouse()) selectTable.visible = false;

                return InputProcessor.super.touchDown(screenX, screenY, pointer, button);
            }
        });

        selectTable.pack();
        selectTable.act(0);
        selectTable.visible = false;
        Core.scene.root.addChildAt(0, selectTable);
    }

    private static void updateSelectTable() {
        selectTable.removeChild(mechanicalDrillButton);
        if (Blocks.mechanicalDrill.environmentBuildable() && ((Drill) Blocks.mechanicalDrill).canMine(selectedTile)) {
            selectTable.add(mechanicalDrillButton);
        }

        selectTable.removeChild(pneumaticDrillButton);
        if (Blocks.pneumaticDrill.environmentBuildable() && ((Drill) Blocks.pneumaticDrill).canMine(selectedTile)) {
            selectTable.add(pneumaticDrillButton);
        }

        selectTable.removeChild(laserDrillButton);
        if (Blocks.laserDrill.environmentBuildable() && ((Drill) Blocks.laserDrill).canMine(selectedTile)) {
            selectTable.add(laserDrillButton);
        }

        selectTable.removeChild(blastDrillButton);
        if (Blocks.blastDrill.environmentBuildable() && ((Drill) Blocks.blastDrill).canMine(selectedTile)) {
            selectTable.add(blastDrillButton);
        }

        selectTable.removeChild(plasmaBoreButton);
        if (Blocks.plasmaBore.environmentBuildable() && selectedTile.wallDrop() != null && selectedTile.wallDrop().hardness <= ((BeamDrill) Blocks.plasmaBore).tier) {
            selectTable.add(plasmaBoreButton);
        }

        selectTable.removeChild(largePlasmaBoreButton);
        if (Blocks.largePlasmaBore.environmentBuildable() && selectedTile.wallDrop() != null && selectedTile.wallDrop().hardness <= ((BeamDrill)Blocks.largePlasmaBore).tier) {
            selectTable.add(largePlasmaBoreButton);
        }

        selectTable.removeChild(impactDrillButton);
        if (Blocks.impactDrill.environmentBuildable() && ((Drill) Blocks.impactDrill).canMine(selectedTile)) {
            selectTable.add(impactDrillButton);
        }

        selectTable.removeChild(eruptionDrillButton);
        if (Blocks.eruptionDrill.environmentBuildable() && ((Drill) Blocks.eruptionDrill).canMine(selectedTile)) {
            selectTable.add(eruptionDrillButton);
        }
    }

    private static void onSelected() {
        isBeamDrill = selectDrill instanceof BeamDrill;
        useNittogenPipe = selectDrill.equals(Blocks.largePlasmaBore);
        fill();
    }

    public static Seq<Tile> fetch(Tile tile, Block drill, Direction direction) {
        if (drill instanceof Drill) {
            Drill groundDrill = (Drill) drill;

            Seq<Tile> tiles = Utils.getConnectedTiles(tile, 300);
            Utils.expandArea(tiles, groundDrill.size);
            
            return tiles;
        }

        if (drill instanceof BeamDrill) {
            BeamDrill wallDrill = (BeamDrill) drill;
        }

        return new Seq<Tile>();
    }

    private static void fill() {
        Log.info("Auto Drill: Water = @, Nitrogen = @, Drill = @", useWaterExtractor || useWaterPipe, useNittogenPipe, selectDrill.name);

        Seq<Tile> tiles = fetch(selectedTile, selectDrill, outputDirection);
        Seq<Rect> rects = new Seq<>();

        for (Tile tile : tiles) {
            Rect rect = Utils.getBlockRect(tile, selectDrill);


            if (rects.contains(other -> other.overlaps(rect))) continue;

            rects.add(rect);
            BuildPlan plan = new BuildPlan(tile.x, tile.y, 0, selectDrill);
            Vars.player.unit().addBuild(plan);
        }
    }

    // public static void fill(Tile tile, Drill drill, Direction direction) {
    //     if (drill.size != 2) throw new InputMismatchException("Drill must have a size of 2");

    //     Seq<Tile> tiles = Utils.getConnectedTiles(tile, maxTiles);
    //     Utils.expandArea(tiles, drill.size / 2);
    //     placeDrillsAndBridges(tile, tiles, drill, direction);
    // }

    // private static void placeDrillsAndBridges(Tile source, Seq<Tile> tiles, Drill drill, Direction direction) {
    //     Point2 directionConfig = new Point2(direction.p.x * 3, direction.p.y * 3);

    //     Seq<Tile> drillTiles = tiles.copy().filter(BridgeDrill::isDrillTile);
    //     Seq<Tile> bridgeTiles = tiles.copy().filter(BridgeDrill::isBridgeTile);

    //     int minOresPerDrill = Core.settings.getInt((drill == Blocks.blastDrill ? "airblast" : (drill == Blocks.laserDrill ? "laser" : (drill == Blocks.pneumaticDrill ? "pneumatic" : "mechanical"))) + "-drill-min-ores");

    //     drillTiles.filter(t -> {
    //         ObjectIntMap.Entry<Item> itemAndCount = Util.countOre(t, drill);

    //         if (itemAndCount == null || itemAndCount.key != source.drop() || itemAndCount.value < minOresPerDrill) {
    //             return false;
    //         }

    //         Seq<Tile> neighbors = Util.getNearbyTiles(t.x, t.y, drill);
    //         neighbors.filter(BridgeDrill::isBridgeTile);

    //         for (Tile neighbor : neighbors) {
    //             if (bridgeTiles.contains(neighbor)) return true;
    //         }

    //         neighbors.filter(n -> {
    //             BuildPlan buildPlan = new BuildPlan(n.x, n.y, 0, Blocks.itemBridge);
    //             return buildPlan.placeable(Vars.player.team());
    //         });

    //         if (!neighbors.isEmpty()) {
    //             bridgeTiles.add(neighbors);
    //             return true;
    //         }

    //         return false;
    //     });

    //     Tile outerMost = bridgeTiles.max((t) -> direction.p.x == 0 ? t.y * direction.p.y : t.x * direction.p.x);
    //     if (outerMost == null) return;

    //     Tile outlet = outerMost.nearby(directionConfig);
    //     bridgeTiles.add(outlet);

    //     bridgeTiles.sort(t -> t.dst2(outlet.worldx(), outlet.worldy()));

    //     for (Tile drillTile : drillTiles) {
    //         BuildPlan buildPlan = new BuildPlan(drillTile.x, drillTile.y, 0, drill);
    //         Vars.player.unit().addBuild(buildPlan);
    //     }

    //     for (Tile bridgeTile : bridgeTiles) {
    //         Tile neighbor = bridgeTiles.find(t -> Math.abs(t.x - bridgeTile.x) + Math.abs(t.y - bridgeTile.y) == 3);

    //         Point2 config = new Point2();
    //         if (bridgeTile != outlet && neighbor != null) {
    //             config = new Point2(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);
    //         }

    //         BuildPlan buildPlan = new BuildPlan(bridgeTile.x, bridgeTile.y, 0, Blocks.itemBridge, config);
    //         Vars.player.unit().addBuild(buildPlan);
    //     }
    // }

    // private static boolean isDrillTile(Tile tile) {
    //     short x = tile.x;
    //     short y = tile.y;

    //     switch (x % 6) {
    //         case 0:
    //         case 2:
    //             if ((y - 1) % 6 == 0) return true;
    //             break;
    //         case 1:
    //             if ((y - 3) % 6 == 0 || (y - 3) % 6 == 2) return true;
    //             break;
    //         case 3:
    //         case 5:
    //             if ((y - 4) % 6 == 0) return true;
    //             break;
    //         case 4:
    //             if ((y) % 6 == 0 || (y) % 6 == 2) return true;
    //             break;
    //     }

    //     return false;
    // }

    // private static boolean isBridgeTile(Tile tile) {
    //     short x = tile.x;
    //     short y = tile.y;

    //     return x % 3 == 0 && y % 3 == 0;
    // }

    // java.lang.ClassCastException: class mindustry.content.Blocks$254 cannot be cast to class mindustry.world.blocks.production.Drill (mindustry.content.Blocks$254 and mindustry.world.blocks.production.Drill are in unnamed module of loader 'app')
	// at hao1337.addons.AutoDrill.fill(AutoDrill.java:290)
	// at hao1337.addons.AutoDrill.onSelected(AutoDrill.java:277)
	// at hao1337.addons.AutoDrill.lambda$buildDirectionTable$6(AutoDrill.java:97)
	// at arc.scene.Element.lambda$clicked$2(Element.java:913)
	// at arc.scene.Element$4.clicked(Element.java:922)
	// at arc.scene.event.ClickListener.touchUp(ClickListener.java:77)
	// at arc.scene.event.InputListener.handle(InputListener.java:31)
	// at arc.scene.Scene.touchUp(Scene.java:370)
	// at arc.input.InputMultiplexer.touchUp(InputMultiplexer.java:136)
	// at arc.input.InputEventQueue.drain(InputEventQueue.java:71)
	// at arc.backend.sdl.SdlInput.update(SdlInput.java:181)
	// at arc.backend.sdl.SdlApplication.loop(SdlApplication.java:189)
	// at arc.backend.sdl.SdlApplication.<init>(SdlApplication.java:54)
	// at mindustry.desktop.DesktopLauncher.main(DesktopLauncher.java:39)
}
