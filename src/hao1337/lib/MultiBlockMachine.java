package hao1337.lib;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.geom.Geometry;
import arc.math.geom.Rect;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.util.Eachable;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Tmp;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;

public class MultiBlockMachine extends Block {
    // ObjectMap that contains build schematic
    public MBMPlan[] build;
    // Build size
    public int areaSize = 32;
    protected int capacity;
    // Block flag
    protected BlockFlag flag = BlockFlag.factory;
    // Frame offset (tile)
    protected float offsetX = 0f, offsetY = 0f;
    // Indicate that block should be a part of the machine
    public boolean selfIsMachine = true;
    // Indicate the block this machine transforms to when build complete. Using null
    // to keep the machine
    @Nullable
    public String mergeInto = null;
    private int correct = 0;
    @Nullable
    private int[][] buildPlans;
    @Nullable
    private Block[] blockIndex;

    MultiBlockMachine(String name) {
        super(name);
        update = true;
        solid = true;
        sync = true;
        flags = EnumSet.of(flag);
        buildVisibility = Core.settings.getBool("hao1337.gameplay.experimental") ? BuildVisibility.shown
                : BuildVisibility.hidden;
    }

    @Override
    public void init() {
        hasItems = false;
        hasLiquids = false;
        hasPower = true;
        outputsPower = false;
        outputsPayload = false;
        rotate = true;
        offset = 0f;
        buildVisibility = BuildVisibility.shown;
        consumePower(5f);

        if (selfIsMachine) {
            offsetX += size * 8;
            offsetY += size * 8;
        }

        capacity = areaSize * areaSize;
        buildPlans = new int[areaSize][areaSize];
        blockIndex = new Block[build.length];
        Matrix.fill(buildPlans, -1);

        for (int i = 0; i < build.length; i++) {
            MBMPlan plan = build[i];
            blockIndex[i] = Vars.content.block(plan.block);

            for (MBMBuildIndex index : plan.pos) {
                if (index.x >= areaSize || index.y >= areaSize) {
                    Vars.ui.showException(new Throwable("Invalid position (outside of bounding box)"));
                    return; // Exit early if there’s an error
                }
                Matrix.squareFill(buildPlans, index.x, index.y, blockIndex[i].size, i);
            }
        }

        buildPlans = Matrix.rotate(buildPlans, 3);

        super.init();
    }

    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        Rect rect = getRect(Tmp.r1, tile.worldx() + this.offset, tile.worldy() + this.offset, rotation).grow(0.1F);
        return !Vars.indexer.getFlagged(team, flag).contains(b -> getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect));
    }

    public Rect getRect(Rect rect, float x, float y, int rotation) {
        int rx = Geometry.d4x(rotation);
        int ry = Geometry.d4y(rotation);

        rect.setCentered(x, y, areaSize * 8);

        float len = 4f * (areaSize + size);
        rect.x += (rx - ry) * len + (ry - rx) * offsetX;
        rect.y += (ry + rx) * len - (ry + rx) * offsetY;

        return rect;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
    }

    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        int worldX = x * 8 + Math.round(offset);
        int worldY = y * 8 + Math.round(offset);
        Rect rect = getRect(Tmp.r1, worldX, worldY, rotation);
        Drawf.dashRect(valid ? Pal.accent : Pal.remove, rect);
    }

    private class Bulding2D {
        public boolean isValid = false;
        public int lx, ly;

        Interval interval = new Interval();
        Seq<Block> matching = new Seq<Block>();
        int[][] buildPlansT;
        int lastRotate = -1;

        void updateMatching(int rotation) {
            if (rotation == lastRotate)
                return;

            lastRotate = rotation;
            matching.clear();
            buildPlansT = Matrix.rotate(buildPlans, rotation);

            for (int i = 0; i < capacity; i++) {
                int indexX = i / areaSize;
                int indexY = i % areaSize;

                if (buildPlansT[indexX][indexY] == -1) {
                    matching.add((Block) null);
                    continue;
                }
                matching.add(blockIndex[buildPlansT[indexX][indexY]]);
            }
        }

        public void updateBounding(Rect rec, int rotation) {
            if (!interval.get(10))
                return;

            updateMatching(rotation);
            correct = 0;

            lx = Math.round(rec.x / 8);
            ly = Math.round(rec.y / 8);
            int hx = Math.round((rec.height + rec.x) / 8);
            int hy = Math.round((rec.width + rec.y) / 8);
            boolean invalid = false;

            for (int x = lx; x < hx && !invalid; x++) {
                for (int y = ly; y < hy; y++) {
                    int indexX = areaSize - x + lx - 1;
                    int indexY = y - ly;
                    int indexM = indexX * areaSize + indexY;

                    Tile tile = Vars.world.tiles.get(x, y);
                    @Nullable
                    Building build = tile.build;
                    Block target = matching.get(indexM);

                    if (target == null) {
                        if (!tile.block().name.equals("air")) {
                            invalid = true;
                            continue;
                        }
                        correct++;
                        continue;
                    }
                    if (build == null || !build.block.name.equals(target.name))
                        invalid = true;
                    else
                        correct++;
                }
            }
            isValid = !invalid;
        }
    }

    @Override
    public void setBars() {
        super.setBars();
    }

    public class MultiBlockMachineBuild extends Building {
        final Color doneColor = Color.rgb(0, 156, 23);
        Bulding2D buildT = new Bulding2D();

        public void updateTile() {
            buildT.updateBounding(getRect(Tmp.r1, x, y, rotation), rotation);
        }

        @Override
        public void draw() {
            super.draw();
            addBar("dai-vip-vai-lon", (e) -> new ProgressBar(
                    () -> Core.bundle.format("hao1337.block.correct-count", (float) correct / capacity * 100),
                    Color.orange,
                    () -> (float) correct / capacity));

            Draw.z(80);
            BlockStatus status = status();
            if (status == BlockStatus.active) {
                projector();
                Drawf.dashRect(buildT.isValid ? doneColor : Pal.accent, getRect(Tmp.r1, x, y, rotation));
            }
            Draw.reset();
        }

        @Override
        public void displayBars(Table table) {
            super.displayBars(table);
        }

        @Override
        public void drawStatus() {
            super.drawStatus();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Drawf.dashRect(buildT.isValid ? doneColor : Color.white, getRect(Tmp.r1, x, y, rotation));
        }

        void projector() {
            float pz = Draw.z();
            Draw.z(20);
            int rx = Geometry.d4x(rotation);
            int ry = Geometry.d4y(rotation);
            int i = 0;

            for (MBMPlan plan : build) {
                for (MBMBuildIndex index : plan.pos) {
                    Block b = blockIndex[i];
                    float c = b.size % 2 == 0 ? 0.5f : b.size == 1 ? 1f : 0f;

                    float projectedX = x + 8 * ((index.x - c) * rx + (c - index.y) * ry);
                    float projectedY = y + 8 * ((index.x - c) * ry + (index.y - c) * rx);

                    int nX = (int)projectedX / 8;
                    int nY = (int)projectedY / 8;
                    Tile tile = Vars.world.tile(nX, nY);
                    Log.info("" + tile);

                    Drawf.additive(b.region, Color.valueOf("eeeeeeff"), projectedX, projectedY, 0f);
                }
                i++;
            }
            Draw.z(pz);
        }
    }
}
