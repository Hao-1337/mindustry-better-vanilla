package hao1337.contents;

import mindustry.Vars;
import mindustry.content.Items;
import mindustry.type.Item;

public class HItems {
    public static Item uranium;

    static boolean loaded = false;
    public static void load() {
        if (loaded) return;
        loaded = true;
        uranium = Vars.content.item("hao1337-mod-uranium");
        Items.erekirItems.add(uranium);
    }

}
