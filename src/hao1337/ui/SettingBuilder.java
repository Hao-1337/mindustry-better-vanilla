package hao1337.ui;

import mindustry.*;

public class SettingBuilder {
    public void build() {
        Vars.ui.settings.addCategory("Better Vanilla", "setting-icoMenu", (t) -> {
            t.checkPref("hao1337.ui.coreinf.enable", true);
            t.checkPref("hao1337.toggle.autoupdate", false);
            t.row();
            t.label(() -> "test");
            t.checkPref("hao1337.gameplay.vault-bigger", false);
            t.checkPref("hao1337.gameplay.better-override-dome", false);
            t.checkPref("hao1337.gameplay.better-shield", false);
        });
        updateData();
    }
    public void updateData() {

    }
}
