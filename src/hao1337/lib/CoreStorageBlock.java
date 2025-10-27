package hao1337.lib;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.type.Item;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockStatus;

public class CoreStorageBlock extends StorageBlock {
    @SuppressWarnings("unused")
    private final int itemCapacity = -1;

    /** If this block must need power to working */
    public boolean needPower = true;
    /** If this block need power for working then this indicate how many should it use */
    public float powerRequire = 0.5f;

    /** Minimal warmup before it working (accpect value is [0.0, 1.0]) */
    public float minwarmup = 1.0f;
    /** How long the warmup take per frame */
    public float warmupSpeed = 0.005f;

    /** Rotator speed */
    public float rotateSpeed = 5.25f;
    /** Default offset for rotation (deg) */
    public float rotationOffset = 60.f;

    /** Random working effect spawn chance */
    public float updateEffectChance = 0.02f;
    /** Effect that get used as working effect */
    public Effect updateEffect = Fx.pulverizeSmall;

    TextureRegion sTopRegion, sRotatorRegion, sRotatorTeam;

    public CoreStorageBlock(String name) {
        super(name);
        super.itemCapacity = 10;
        coreMerge = true;
        update = true;
        solid = true;
        sync = true;
        separateItemCapacity = false;
        noUpdateDisabled = true;
        hasPower = needPower;

        if (needPower) consume(new ConsumePower(powerRequire, 5.0f, false));
    }

    @Override
    public void init() {
        super.init();
        sTopRegion = Core.atlas.find(name + "-top");
        sRotatorRegion = Core.atlas.find(name + "-rotator");
    }

    public class CoreStorageBuilding extends StorageBlock.StorageBuild {
        public float warmup;
        float rotation = 0f;

        boolean lastWorkingState = false;
    
        private void setCore(Team team) {
            linkedCore = team.core();
            if (linkedCore == null) return;
            if (!linkedCore.proximity.contains(this)) {
                linkedCore.proximity.add(this);
                linkedCore.onProximityUpdate();
            }
        }

        @Override
        public void onRemoved() {
            if (linkedCore != null) linkedCore.proximity.remove(this);
            super.onRemoved();
        }

        @Override
        public void onDestroyed() {
            if (linkedCore != null) linkedCore.proximity.remove(this);
            super.onDestroyed();
        }

        @Override
        public boolean canUnload() {
            return linkedCore != null && warmup >= minwarmup;
        }

        @Override
        public boolean canWithdraw() {
            return linkedCore != null && warmup >= minwarmup;
        }

        public boolean allowDeposit() {
            return linkedCore != null && warmup >= minwarmup;
        }

        @Override
        public boolean acceptItem(Building other, Item item) {
            return linkedCore != null && warmup >= minwarmup && super.acceptItem(other, item);
        }

        @Override
        public void updateTile() {
            setCore(team);

            var cu = canUnload();
            if (lastWorkingState != cu) {
                lastWorkingState = cu;
                onPowerStateChange();
            }

            rotation += warmup * delta();

            var stat = status();
            if (stat == BlockStatus.active) {
                warmup = Mathf.approachDelta(warmup, 1.0f, warmupSpeed);

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2.5f), y + Mathf.range(size * 2.5f));
            } else {
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
            }
        }

        @Override
        public void draw() {
            super.draw();
            if (sRotatorRegion != null)
                Drawf.spinSprite(sRotatorRegion, x, y, rotationOffset + rotation * rotateSpeed);

            if (sTopRegion != null) {
                Draw.color();
                Draw.blend(Blending.normal);
                Draw.rect(sTopRegion, x, y);
                Draw.blend();
                Draw.color();
            }
        }

        private void onPowerStateChange() {
            for(int i = 0; i < proximity.size; i++){
                Building other = proximity.get(i);
                if (other instanceof Unloader.UnloaderBuild unloader) {
                    unloader.onProximityUpdate();
                }
            }
        }

        @Override
        public boolean canPickup() {
            return false;
        }

        @Override
        public byte version(){
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
