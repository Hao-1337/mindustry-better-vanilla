package hao1337.lib;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Geometry;
import arc.math.geom.Rect;
import arc.struct.EnumSet;
import arc.struct.IntSeq;
import arc.util.Eachable;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Tmp;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
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
    private Block[] blockIndex;
    private int solidCount = 0;

    MultiBlockMachine(String name) {
        super(name);
        update = true;
        solid = true;
        sync = true;
        flags = EnumSet.of(flag);
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
        buildVisibility = Core.settings.getBool("hao1337.gameplay.experimental") ? BuildVisibility.shown
                : BuildVisibility.hidden;
        consumePower(5f);

        if (selfIsMachine) {
            offsetX += size * 8;
            offsetY += size * 8;
        }

        capacity = areaSize * areaSize;
        blockIndex = new Block[build.length];

        for (int i = 0; i < build.length; i++) {
            MBMPlan plan = build[i];
            blockIndex[i] = Vars.content.block(plan.block);

            for (MBMBuildIndex index : plan.pos) {
                if (index.x >= areaSize || index.y >= areaSize) {
                    Vars.ui.showException(new Throwable("Invalid position (outside of bounding box)"));
                    return; // Exit early if there’s an error
                }
            }
            solidCount += blockIndex[i].size * blockIndex[i].size;
        }
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

    @Override
    public void setBars() {
        super.setBars();
    }

    public class MultiBlockMachineBuild extends Building {
        public final Color projectColor = Color.rgb(125, 125, 125);
        public final Color errorColor = Color.red;
        public final Color doneColor = Color.rgb(0, 156, 23);

        boolean isValid = false;
        IntSeq currentBuild = new IntSeq();
        Interval interval = new Interval();

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
                updateProjector();
                Drawf.dashRect(isValid ? doneColor : Pal.accent, getRect(Tmp.r1, x, y, rotation));
            }
            Draw.reset();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Drawf.dashRect(isValid ? doneColor : Color.white, getRect(Tmp.r1, x, y, rotation));
        }

        void updateProjector() {
            int rx = Geometry.d4x(rotation);
            int ry = Geometry.d4y(rotation);

            int i = 0, c = 0;
            boolean lst = true;

            correct = 0;
            currentBuild.clear();

            for (MBMPlan plan : build) {
                for (MBMBuildIndex index : plan.pos) {
                    Block b = blockIndex[i];
                    float hz = (float) b.size / 2;
                    float pz = b.size % 2 == 1 ? b.size == 1 ? -8f : 8f : 4f;

                    float cx = x + 8 * ((index.x - hz) * rx + (hz - index.y) * ry);
                    float cy = y + 8 * ((index.x - hz) * ry + (index.y - hz) * rx);
                    int x = b.size == 1 ? (int) Math.floor(cx / 8f) : Math.round(cx / 8f);
                    int y = b.size == 1 ? (int) Math.floor(cy / 8f) : Math.round(cy / 8f);

                    BoundingResult re = checkBounding(b, x, y);
                    if (!re.correct) projector(b.region, b.size, cx + hz * pz * (rx - ry), cy + hz * pz * (ry + rx), 50f);

                    if (!re.correct && lst)
                        lst = false;
                    correct += re.count;
                    c += re.count;
                    i++;
                }
            }
            Rect rec = getRect(Tmp.r1, x, y, rotation);

            int lx = Math.round(rec.x / 8);
            int ly = Math.round(rec.y / 8);
            int hx = (int) Math.floor((rec.height + rec.x) / 8);
            int hy = (int) Math.floor((rec.width + rec.y) / 8);

            for (int ax = lx; ax <= hx; ax++) {
                for (int ay = ly; ay <= hy; ay++) {
                    Tile t = Vars.world.tile(ax, ay);
                    if (t.block().isAir()) {
                        correct++;
                        continue;
                    }
                    if (t.build == null) {
                        projectorError(ax * 8, ay * 8, 45f);
                        continue;
                    }
                    if (currentBuild.contains(t.build.id)) {
                        continue;
                    }
                    if (lst) lst = false;
                    projectorError(ax * 8, ay * 8, 45f);
                }
            }
            isValid = lst;
            correct -= solidCount - c;
        }

        class BoundingResult {
            public boolean correct = true;
            public int count = 0;
        }

        void projector(TextureRegion region, int size, float x, float y, float layer) {
            float pz = Draw.z();
            Draw.z(layer);
            Draw.color(projectColor, 1f);
            Draw.blend(Blending.additive);
            Draw.rect(region, x, y, 0f);
            Draw.blend();
            Draw.z(layer + 10f);
            Draw.color(projectColor, 0f);
            Fill.square(x, y, size * 4f);
            Draw.color();
            Draw.z(pz);
        }

        void projectorError(float x, float y, float layer) {
            float pz = Draw.z();
            Draw.z(layer);

            Draw.color(errorColor, 0.5f);
            Fill.square(x, y, 4f);
            Draw.color();
            Draw.z(pz);
        }

        BoundingResult checkBounding(Block block, int x, int y) {
            BoundingResult should = new BoundingResult();
            boolean done = false;
            int build = -1;

            for (int x1 = x; x1 < x + block.size; x1++) {
                for (int y1 = y; y1 < y + block.size; y1++) {
                    Tile tile = Vars.world.tile(x1, y1);
                    @Nullable Block block1 = tile.block();
                    if (block1 == null) {
                        should.correct = false;
                        continue;
                    }
                    if (!block.name.equals(block1.name)) {
                        should.correct = false;
                        continue;
                    }
                    if (!done) {
                        done = true;
                        build = Vars.world.tile(x1, y1).build.id;
                    }
                    else if (tile.build == null || tile.build.id != build) {
                        should.correct = false;
                        continue;
                    }
                    should.count++;
                }
            }
            if (build != -1 && should.count == block.size * block.size) currentBuild.add(build);
            return should;
        }
    }
}
