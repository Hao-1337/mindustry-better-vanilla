package hao1337.ui;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import mindustry.*;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.SettingsMenuDialog;

public class SettingBuilder {
    public class SettingTitle extends SettingsMenuDialog.SettingsTable.Setting {
        public String desc;

        public SettingTitle() {
            super((String) null);
        }

        public void add(SettingsMenuDialog.SettingsTable table) {
            table.table(t -> t.background(Tex.underline)).fillX().minHeight(1f);
            table.row();
            table.table(t -> {}).fillX().minHeight(5f);
            table.row();

            Label l = ((Label) table.labelWrap(desc).fillX().center().get());

            l.setWrap(true);
            l.setColor(Pal.accent);
            l.setFontScale(1.3f);
            l.setAlignment(1);

            table.row();
            table.table(t -> {}).fillX().minHeight(10f);
            table.row();
        }
    }

    public void build() {
        Vars.ui.settings.addCategory("Better Vanilla", new TextureRegionDrawable(Icon.settingsSmall), (t) -> {
            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.ui"); }});
            t.checkPref("hao1337.ui.coreinf.enable", true);
            t.checkPref("hao1337.ui.unitinf.enable", true);
            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.gameplay"); }});
            t.checkPref("hao1337.gameplay.vault-bigger", false);
            t.checkPref("hao1337.gameplay.better-override-dome", false);
            t.checkPref("hao1337.gameplay.better-shield", false);
            t.checkPref("hao1337.gameplay.scrap-wall", false);
            t.checkPref("hao1337.gameplay.add-new-content", false);
            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.other"); }});
            t.checkPref("hao1337.toggle.autoupdate", false);
        });
    }
}
