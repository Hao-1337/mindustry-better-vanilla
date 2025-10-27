package hao1337.lib;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.graphics.Drawf;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;

public class WarmupGenericCrafter extends GenericCrafter {
    /** Warmup speed */
    public float warmupSpeed = 0.02f;
    /** Rotator speed */
    public float rotateSpeed = 8.25f;
    /** Default offset for rotation (deg) */
    public float rotationOffset = 60.f;

    TextureRegion sRotatorRegion;

    public WarmupGenericCrafter(String name) {
        super(name);
        buildVisibility = Core.settings.getBool("hao1337.gameplay.experimental") ? BuildVisibility.shown : BuildVisibility.hidden;
        warmupSpeed = 0.01f;
    }

    @Override
    public void init() {
        super.init();
        sRotatorRegion = Core.atlas.find(name + "-rotator");
    }

    public class WarmupGenericCrafterBuild extends GenericCrafterBuild {
        public float rwarmup;
        float rotation = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            rotation += warmup * delta();

            var stat = status();
            if (stat == BlockStatus.active) {
                rwarmup = Mathf.approachDelta(rwarmup, 1.0f, warmupSpeed);

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2.5f), y + Mathf.range(size * 2.5f));
            } else {
                rwarmup = Mathf.approachDelta(rwarmup, 0f, warmupSpeed);
            }
        }
        
        @Override
        public void draw() {
            super.draw();
            if (sRotatorRegion != null)
                Drawf.spinSprite(sRotatorRegion, x, y, rotationOffset + rotation * rotateSpeed);
        }

        @Override
        public float totalProgress() {
            return warmup * 0.5f * delta();
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(rwarmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                rwarmup = read.f();
            }
        }
    }
}
