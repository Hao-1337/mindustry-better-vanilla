package hao1337.lib;

import arc.Core;
import arc.util.Time;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;

public class WarmupGenericCrafter extends GenericCrafter {
    public float warmupSpeed = 0.1f;
    public float minWarmup = 1f;

    public WarmupGenericCrafter(String name) {
        super(name);
        buildVisibility = Core.settings.getBool("hao1337.gameplay.experimental") ? BuildVisibility.shown : BuildVisibility.hidden;
        warmupSpeed = 0.01f;
    }

    public class WarmupGenericCrafterBuild extends GenericCrafterBuild {
        @Override
        public float totalProgress() {
            return warmup * 0.5f * Time.delta;
        }
    }
}
