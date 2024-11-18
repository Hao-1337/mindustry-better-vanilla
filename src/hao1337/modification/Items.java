package hao1337.modification;

import arc.Core;

public class Items {
    public static void apply(boolean hidden) {
        mindustry.content.Items.fissileMatter.hidden = mindustry.content.Items.dormantCyst.hidden = hidden;
    }

}
