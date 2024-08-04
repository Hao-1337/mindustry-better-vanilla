package hao1337.content.items;

import mindustry.Vars;
import mindustry.content.Items;
import mindustry.type.Item;

public class HaoItems {
    public static Item uranium;
    
    public static void load() {
        uranium = Vars.content.item("hao1337-mod-uranium");
        Items.erekirItems.add(uranium);
    }

}
