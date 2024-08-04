package hao1337.modification;

import arc.Core;

public class Items {
    public static void load() {
        mindustry.content.Items.fissileMatter.hidden = mindustry.content.Items.dormantCyst.hidden = Core.settings.getBool("hao1337.gameplay.erekir.hidden-item");
    }

}
