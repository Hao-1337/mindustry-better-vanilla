package hao1337.ui;

import mindustry.*;

public class SettingBuilder {
    public void build() {
        Vars.ui.settings.addCategory("Better Vanilla", "setting-icoMenu", (t) -> {
            t.checkPref("hao1337.ui.coreinf.enable", true);
            t.checkPref("hao1337.ui.unitinf.enable", true);
            t.checkPref("hao1337.gameplay.vault-bigger", false);
            t.checkPref("hao1337.gameplay.better-override-dome", false);
            t.checkPref("hao1337.gameplay.better-shield", false);
            t.checkPref("hao1337.toggle.autoupdate", false);
        });
    
        // Why not
        // Table table = Vars.ui.settings.getCategories().find(c -> c.name == "Better Vanilla").table;
        // Label l1 = new Label(Core.bundle.format("hao1337.setting.category.ui")) {{ name = "label-1"; }};
        // Label l2 = new Label(Core.bundle.format("hao1337.setting.category.gameplay")) {{ name = "label-2"; }};
        // Label l3 = new Label(Core.bundle.format("hao1337.setting.category.other")) {{ name = "label-3"; }};

        // table.addChildAt(0, l1);
        // table.addChildAt(2, l2);
        // table.addChildAt(6, l3);
    }
}
