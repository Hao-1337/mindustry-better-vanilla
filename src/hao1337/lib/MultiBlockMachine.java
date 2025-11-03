package hao1337.lib;

import java.util.Arrays;

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
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Interval;
import arc.util.Nullable;
import arc.util.Tmp;

import mindustry.Vars;

import static mindustry.Vars.*;
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
    public static class InvalidSize extends Exception {
        public InvalidSize() { super(); }
        public InvalidSize(String mess) { super(mess); }
    }

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
        consumePower(5f);
        updateClipRadius(areaSize * tilesize);

        super.init();

        buildVisibility = Core.settings.getBool("hao1337.gameplay.experimental") ? BuildVisibility.shown
                : BuildVisibility.hidden;

        if (selfIsMachine) {
            offsetX += size * 8;
            offsetY += size * 8;
        }

        capacity = areaSize * areaSize;
        blockIndex = new Block[build.length];

        if (size % 2 == 0)
            throw new RuntimeException("Multi block machine size must be a odd number");

        int[] overlap = new int[capacity];
        Arrays.fill(overlap, 0);
    
        for (int i = 0; i < build.length; i++) {
            MBMPlan plan = build[i];
            var oi = i + 1;
            blockIndex[i] = Vars.content.block(plan.block);
            if (blockIndex[i] == null) throw new RuntimeException("Invalid block id: " + plan.block);

            for (MBMBuildIndex index : plan.pos) {
                if (index.x >= areaSize || index.y >= areaSize)
                    throw new RuntimeException("Block position out of bound!");

                for (int j = index.y; j < index.y + blockIndex[i].size; j++) {
                    for (int i1 = index.x; i1 < index.x + blockIndex[i].size; i1++) {
                        int idx = (areaSize - 1 - j) * areaSize + i1;

                        if (idx > capacity)
                            throw new RuntimeException("Block position out of bound!");
                        if (overlap[idx] != 0)
                            throw new RuntimeException("Block: " + plan.block + "(" + index.x + ", " + index.y + ") overlap with other block: " + blockIndex[overlap[idx] - 1].name + "(" + i + ", " + j + ")");
                        overlap[idx] = oi;
                    }
                }
            }
            solidCount += blockIndex[i].size * blockIndex[i].size;
        }
    }

    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        Rect rect = getRect(Tmp.r1, tile.worldx() + this.offset, tile.worldy() + this.offset, rotation).grow(0.1f);
        return !Vars.indexer.getFlagged(team, flag).contains(b -> getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect));
    }

    public Rect getRect(Rect rect, float x, float y, int rotation) {
        int rx = Geometry.d4x(rotation);
        int ry = Geometry.d4y(rotation);

        rect.setCentered(x, y, areaSize * tilesize);

        float len = tilesize / 2f * (areaSize + size);
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
        int worldX = x * tilesize + Math.round(offset);
        int worldY = y * tilesize + Math.round(offset);
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

        int correct = 0;
        boolean isValid = false, needRebuild = true;
        IntSeq currentBuild = new IntSeq();
        Interval interval = new Interval();
        Seq<Projector> projectors = new Seq<Projector>();

        @Override
        public void draw() {
            super.draw();
            addBar("dai-vip-vai-lon", (e) -> new ProgressBar(
                    () -> Core.bundle.format("hao1337.block.correct-count", (float) correct / capacity * 100),
                    Color.orange,
                    () -> (float) correct / capacity));

            BlockStatus status = status();
            if (status != BlockStatus.active) return;
            updateProjector();
    
            Draw.z(80);
            Drawf.dashRect(isValid ? doneColor : Pal.accent, getRect(Tmp.r1, x, y, rotation));

            for (Projector pr : projectors) pr.load();
            Draw.reset();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Drawf.dashRect(isValid ? doneColor : Color.white, getRect(Tmp.r1, x, y, rotation));
        }

        class Projector {
            public TextureRegion region;
            public int size;
            public float x, y, layer;
            public boolean error = false;

            public Projector(TextureRegion region, int size, float x, float y, float layer) {
                this.region = region;
                this.x = x;
                this.y = y;
                this.layer = layer;
                this.size = size;
            }

            public Projector(float x, float y, float layer) {
                error = true;
                this.x = x;
                this.y = y;
                this.layer = layer;
            }

            public void load() {
                if (error) {
                    projectorError();
                    return;
                }
                projector();
            }

            void projector() {
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
    
            void projectorError() {
                float pz = Draw.z();
                Draw.z(layer);
    
                Draw.color(errorColor, 0.5f);
                Fill.square(x, y, 4f);
                Draw.color();
                Draw.z(pz);
            }
        }

        void updateProjector() {
            if (!interval.get(30f)) return;
            projectors.clear();

            int rx = Geometry.d4x(rotation);
            int ry = Geometry.d4y(rotation);

            int i = 0, c = 0;
            boolean lst = true;

            correct = 0;
            currentBuild.clear();

            for (MBMPlan plan : build) {
                for (MBMBuildIndex index : plan.pos) {
                    Block b = blockIndex[i];

                    float
                        hz = b.size / 2f,
                        pz = b.size % 2 == 1 ? b.size == 1 ? -tilesize : tilesize : tilesize / 2f,
                        cx = x + tilesize * ((index.x - hz) * rx + (hz - index.y) * ry),
                        cy = y + tilesize * ((index.x - hz) * ry + (index.y - hz) * rx),
                        px = cx + hz * pz * (rx - ry),
                        py = cy + hz * pz * (ry + rx);
                    int
                        lz = Math.max(0, (b.size > 2 ? (int) Math.floor(hz) : 0) - (b.size % 2 == 0 ? 1 : 0)),
                        lx = (int)(px / tilesize) - lz,
                        ly = (int)(py / tilesize) - lz;

                    BoundingResult re = checkBounding(b, lx, ly);
                    if (!re.correct) projectors.add(
                        new Projector(
                            b.region,
                            b.size,
                            cx + hz * pz * (rx - ry),
                            cy + hz * pz * (ry + rx),
                            50f
                        )
                    );

                    if (!re.correct && lst)
                        lst = false;
                    correct += re.count;
                    c += re.count;
                    i++;
                }
            }
            Rect rec = getRect(Tmp.r1, x, y, rotation);

            int lx = Math.round(rec.x / tilesize);
            int ly = Math.round(rec.y / tilesize);
            int hx = (int) Math.floor((rec.height + rec.x) / tilesize);
            int hy = (int) Math.floor((rec.width + rec.y) / tilesize);

            for (int ax = lx; ax <= hx; ax++) {
                for (int ay = ly; ay <= hy; ay++) {
                    Tile t = Vars.world.tile(ax, ay);
                    if (t.block().isAir()) {
                        correct++;
                        continue;
                    }
                    if (t.build == null) {
                        projectors.add(new Projector(ax * tilesize, ay * tilesize, 45f));
                        continue;
                    }
                    if (currentBuild.contains(t.build.id)) {
                        continue;
                    }
                    if (lst) lst = false;
                    projectors.add(new Projector(ax * tilesize, ay * tilesize, 45f));
                }
            }
            isValid = lst;
            correct -= solidCount - c;
        }

        class BoundingResult {
            public boolean correct = true;
            public int count = 0;
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
