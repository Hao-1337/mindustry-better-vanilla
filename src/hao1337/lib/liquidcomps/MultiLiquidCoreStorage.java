package hao1337.lib.liquidcomps;

import static mindustry.Vars.iconSmall;
import static mindustry.Vars.tilesize;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.ObjectFloatMap;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.graphics.Drawf;
import mindustry.logic.LAccess;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawPumpLiquid;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.modules.LiquidModule.LiquidCalculator;

/**
 * Main storage whose hold shared LiquidGraph.
 */
public class MultiLiquidCoreStorage extends LiquidRouter {
    public @Nullable Effect explodeEffect;
    public @Nullable Sound explodeSound;

    public boolean haveWarmup = true;
    public float minWarmup = 0.75f;
    public float warmupSpeed = 0.0005f;
    public float maxConsumePower = 80f;

    public static float pumpAmount = 0.35f;
    public DrawBlock drawer = new DrawMulti(new DrawDefault(), new DrawPumpLiquid());
    
    /** Random working effect spawn chance */
    public float updateEffectChance = 0.02f;
    /** Effect that get used as working effect */
    public Effect updateEffect = Fx.pulverizeSmall;
    
    /** Rotator speed */
    public float rotateSpeed = 8.25f;
    /** Default offset for rotation (deg) */
    public float rotationOffset = 0f;
    public Color heatColor = Color.valueOf("ff5512");

    TextureRegion rotationTopRegion, rotatorRegion, rimRegion;

    public MultiLiquidCoreStorage(String name) {
        super(name);

        buildVisibility = BuildVisibility.shown;
        hasLiquids = true;
        update = true;
        solid = true;
        hasPower = true;
        noUpdateDisabled = false;
        enableDrawStatus = true;
        canOverdrive = true;

        if (!consumeBuilder.contains(c -> c instanceof ConsumePower)) {
            consumePowerDynamic((MultiLiquidCoreStorageBuilding build) -> {
                if (build.graph == null) return 0f;
                for (var l : Vars.content.liquids()) build.graph.caculator.set(l, build.liquids.get(l));
                build.graph.caculator.updateFlow();

                float totalFlowRate = 0f;
                for (var liq : Vars.content.liquids())
                    totalFlowRate += build.graph.caculator.getFlowRate(liq);

                return Math.clamp(totalFlowRate, 0f, maxConsumePower);
            });
        }
    }

    @Override
    public boolean consumesLiquid(Liquid liq){
        return true;
    }

    @Override
    public void init() {
        super.init();
        try {
            NuclearReactor thoriumReactor = (NuclearReactor) Blocks.thoriumReactor;
            explodeEffect = thoriumReactor.explodeEffect;
            explodeSound = thoriumReactor.explodeSound;

            rotationTopRegion = Core.atlas.find(name + "-top");
            rotatorRegion = Core.atlas.find(name + "-rotator");
            rimRegion = Core.atlas.find(name + "-rim");
        } catch (Throwable e) {}
    }

    @Override
    public void setBars() {
        super.setBars();

        // remove vanilla single-liquid bar
        barMap.remove("liquid");

        addBar("", (MultiLiquidCoreStorageBuilding e) -> {
            var liqSize = Vars.content.liquids().sum(l -> !l.unlockedNow() || !l.isOnPlanet(Vars.state.getPlanet()) || l.isHidden() ? 0 : 1);
            float sum = e.liquids.sum(new LiquidCalculator() { public float get(Liquid liquid, float sum) { return sum > 0.01f ? 1f : 0f; } });
            return new Bar(() -> Core.bundle.format("hao1337.block.multi-liquid.total-liquid", (int)sum, liqSize), () -> Color.lime, () ->  sum / liqSize);
        });
    }

    public static @Nullable MultiLiquidCoreStorageBuilding get(Team team) {
        for (Building build : Groups.build) {
            if (build.team == team && build instanceof MultiLiquidCoreStorageBuilding storage) {
                return storage;
            }
        }
        return null;
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        return get(team) == null && super.canPlaceOn(tile, team, rotation);
    }

    protected boolean canPump(Tile tile){
        return tile != null && tile.floor().liquidDrop != null;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        if (!canPlaceOn(Vars.world.tile(x, y), Vars.player.team(), rotation)) {
            drawPlaceText(Core.bundle.format("hao1337.block.multi-liquid.only-one"), x, y, valid);
            return;
        }

        Tile tile = Vars.world.tile(x, y);

        if(valid && tile != null && pumpAmount > 0f){
            float amount = 0f;
            Liquid liquidDrop = null;

            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(canPump(other)){
                    if(liquidDrop != null && other.floor().liquidDrop != liquidDrop){
                        liquidDrop = null;
                        break;
                    }
                    liquidDrop = other.floor().liquidDrop;
                    amount += other.floor().liquidMultiplier;
                }
            }

            if(liquidDrop != null){
                float width = drawPlaceText(Core.bundle.formatFloat("bar.pumpspeed", amount * pumpAmount * 60f, 0), x, y, valid);
                float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;
                float ratio = (float)liquidDrop.fullIcon.width / liquidDrop.fullIcon.height;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(liquidDrop.fullIcon, dx, dy - 1, s * ratio, s);
                Draw.reset();
                Draw.rect(liquidDrop.fullIcon, dx, dy, s * ratio, s);
            }
        }
    }

    public class MultiLiquidCoreStorageBuilding extends LiquidBuild implements LiquidGraph.InferLiquidGraph {
        LiquidGraph graph;
        Liquid liquidDrop;

        float amount = 0f;
        boolean isSecondOne = false;
        float warmup = 0f;
        float rotation = 0f;

        public LiquidGraph getGraph() { return graph; }
        public void setGraph(LiquidGraph graph) { /** This is master graph */ }

        @Override
        public Building create(Block block, Team team) {
            var res = super.create(block, team);

            if (get(team) != null) {
                isSecondOne = true;
            }
            else {
                graph = new LiquidGraph(this.liquids, liquidCapacity);
                graph.add(this);
            }

            return res;
        }

        @Override
        public void draw() {
            drawer.draw(this);
            this.drawTeamTop();
            this.drawStatus();

            float s = 0.45f;
            float ts = 0.75f;

            if (rimRegion != null) {
                Draw.color(heatColor);
                Draw.alpha(warmup * ts * (1f - s + Mathf.absin(Time.time, 3f, s)));
                Draw.blend(Blending.additive);
                Draw.rect(rimRegion, x, y);
                Draw.blend();
                Draw.color();
            }

            if (rotatorRegion != null) {
                Drawf.spinSprite(rotatorRegion, x, y, rotationOffset + rotation * rotateSpeed);
            }

            if (rotationTopRegion != null) {
                Draw.color();
                Draw.blend(Blending.normal);
                Draw.rect(rotationTopRegion, x, y);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public void updateTile() {
            if (isSecondOne) {
                Damage.damage(x, y, 12 * Vars.tilesize, 20f);

                if (explodeEffect != null) explodeEffect.at(this);
                if (explodeSound != null) explodeSound.at(this);
                Effect.shake(5f, 120f, this);
        
                return;
            }

            if (haveWarmup) {
                rotation += warmup * delta();
                
                var status = status();
                graph.allowTransfer = status == BlockStatus.logicDisable ? false : warmup >= minWarmup;

                if (status == BlockStatus.active) {
                    warmup = Mathf.approachDelta(warmup, 1.0f, warmupSpeed);
                    if(Mathf.chanceDelta(updateEffectChance * warmup)) updateEffect.at(x + Mathf.range(size * 3.5f), y + Mathf.range(size * 3.5f));
                } else {
                    warmup = Mathf.approachDelta(warmup, 0f, status == BlockStatus.logicDisable ? 1f : 0.25f);
                }
            }
            else warmup = 1f;

            if (warmup >= minWarmup) {
                float maxPump = Math.min(liquidCapacity - liquids.get(liquidDrop), amount * pumpAmount * edelta());
                liquids.add(liquidDrop, maxPump);
            }

            // Dynamically add bars for every stored liquid.
            for (var liq : Vars.content.liquids()) {
                if (liquids.get(liq) > 0.05f) {
                    String name = "liquid-" + liq.name;

                    if (!barMap.containsKey(name)) {
                        addBar("liquid-" + liq.name, entity -> !liq.unlockedNow() ? null : new Bar(() -> liq.localizedName, liq::barColor, () -> entity.liquids.get(liq) / liquidCapacity));
                    }
                }
            }

            // Intentionally DO NOT dump liquids.
            // We'll make a dedicated extractor/unloader later.
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.efficiency) return shouldConsume() ? efficiency : 0f;
            if(sensor == LAccess.totalLiquids) return liquidDrop == null ? 0f : liquids.get(liquidDrop);
            return super.sense(sensor);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            amount = 0f;
            liquidDrop = null;

            for (Tile other : tile.getLinkedTiles(tempTiles)) {
                if (canPump(other)) {
                    liquidDrop = other.floor().liquidDrop;
                    amount += other.floor().liquidMultiplier;
                }
            }
        }

        ObjectFloatMap<Liquid> visualLiquids = new ObjectFloatMap<>();

        @Override
        public void drawLight() {
            if (liquids == null) return;
            drawer.drawLight(this);

            for (var liq : Vars.content.liquids()) {
                if (liquids.get(liq) < 0.01f)
                    continue;

                if (this.block.hasLiquids && this.block.drawLiquidLight && liq.lightColor.a > 0.001F) {
                    var liqStep = this.visualLiquids.get(liq, 0f);
                    this.visualLiquids.put(liq, Mathf.lerpDelta(liqStep, this.liquids.get(liq) >= 0.01f ? 1.0f : 0.0f, 0.06f));
                    this.drawLiquidLight(liq, liqStep);
                }
            }
        }

        @Override
        public boolean canPickup() {
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return graph.allowTransfer && liquids.get(liquid) < liquidCapacity;
        }

        @Override
        public boolean canDumpLiquid(Building to, Liquid liquid) {
            return false;
        }

        @Override
        public void dumpLiquid(Liquid liquid) {
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                warmup = read.f();
            }
        }
    }
}