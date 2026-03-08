package hao1337.contents;

import mindustry.type.UnitType;
import mindustry.Vars;
public class HUnits {
    public static UnitType zelovark;
    static boolean loaded = false;

    public static void load() {
        if (loaded)
            return;
        loaded = true;
        zelovark = Vars.content.unit("hao1337-mod-zelvorak");
    }
}
