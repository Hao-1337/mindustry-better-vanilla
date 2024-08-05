package hao1337.modification;

import arc.Core;

public class Liquids {
    public static void load() {
        mindustry.content.Liquids.gallium.hidden = !Core.settings.getBool("hao1337.gameplay.erekir.hidden-liquid");
    }
}
