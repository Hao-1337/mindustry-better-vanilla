package hao1337.lib;

import mindustry.world.blocks.storage.Unloader;

public class ValveUnloader extends Unloader {
    public ValveUnloader(String name) {
        super(name);
        speed = 60f / 40f;
    }

    public class ValveUnloaderBuild extends Unloader.UnloaderBuild {}
}
