package hao1337.content.units;

import mindustry.type.UnitType;
import mindustry.Vars;

public class HaoUnits {
    public static UnitType zelovark;

    public static void load() {
        zelovark = Vars.content.unit("hao1337-mod-zelvorak");
    }
}
