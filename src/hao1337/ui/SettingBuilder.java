package hao1337.ui;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Align;
import mindustry.*;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.SettingsMenuDialog;

public class SettingBuilder {
    public class SettingTitle extends SettingsMenuDialog.SettingsTable.Setting {
        public String desc;
        public float height = 1f;

        public SettingTitle() {
            super((String) null);
        }

        public void add(SettingsMenuDialog.SettingsTable table) {
            if (height > 0) table.table(t -> t.background(Tex.underline)).fillX().minHeight(height);
            table.row();
            table.table(t -> {}).fillX().minHeight(10f);
            table.row();

            arc.scene.ui.Label l = ((arc.scene.ui.Label) table.labelWrap(desc).fillX().center().get());

            l.setWrap(true);
            l.setColor(Pal.accent);
            l.setFontScale(1.3f);
            l.setAlignment(1);

            table.row();
            table.table(t -> {}).fillX().minHeight(10f);
            table.row();
        }
    }
    public class Line extends SettingsMenuDialog.SettingsTable.Setting {
        public float height = .25f;
        public float width = 120f;

        public Line() {
            super((String) null);
        }

        public void add(SettingsMenuDialog.SettingsTable table) {
            table.table(t -> t.background(Tex.underline)).width(width).minHeight(height);
            table.row();
        }
    }
    public class Padding extends SettingsMenuDialog.SettingsTable.Setting {
        public float height = 25f;

        public Padding() {
            super((String) null);
        }

        public void add(SettingsMenuDialog.SettingsTable table) {
            table.table(t -> {}).fillX().minHeight(height);
            table.row();
        }
    }
    public class Label extends SettingsMenuDialog.SettingsTable.Setting {
        public String content;

        public Label() {
            super((String) null);
        }

        public void add(SettingsMenuDialog.SettingsTable table) {
            table.align(Align.left);
            table.table(t -> {
                t.label(() -> content).align(Align.left);
            }).fillX();
            table.row();
        }
    }


    public void build() {
        Vars.ui.settings.addCategory("Better Vanilla", new TextureRegionDrawable(Icon.settingsSmall), (t) -> {
            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.ui"); height = 0f; }});
            t.checkPref("hao1337.ui.coreinf.enable", true);
            t.checkPref("hao1337.ui.unitinf.enable", true);
            t.checkPref("hao1337.ui.timecontrol.enable", true);


            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.gameplay.serpulo"); }});
            t.pref(new Label(){{ content = Core.bundle.format("setting.hao1337.ui.restart.label"); }});
            t.checkPref("hao1337.gameplay.serpulo.vault-bigger", false);
            t.checkPref("hao1337.gameplay.serpulo.better-override-dome", false);
            t.checkPref("hao1337.gameplay.serpulo.better-shield", false);
            t.checkPref("hao1337.gameplay.serpulo.scrap-wall", false);

            t.pref(new Padding(){{ height = 40f; }});
            t.checkPref("hao1337.gameplay.serpulo.thorium-conveyor", true);
            t.checkPref("hao1337.gameplay.serpulo.surge-conveyor", true);
            t.checkPref("hao1337.gameplay.serpulo.box", true);
            t.checkPref("hao1337.gameplay.serpulo.silo", true);
            t.checkPref("hao1337.gameplay.serpulo.no-connect-container", true);
            t.checkPref("hao1337.gameplay.serpulo.ultra-vault", true);
            t.checkPref("hao1337.gameplay.serpulo.gigantic-dome", true);
            t.checkPref("hao1337.gameplay.serpulo.valve-unloader", true);
            t.checkPref("hao1337.gameplay.serpulo.leviathan-reconstructor", true);
            t.checkPref("hao1337.gameplay.serpulo.m1014", true);
            t.checkPref("hao1337.gameplay.serpulo.dropper", true);


            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.gameplay.erekir"); }});
            t.pref(new Label(){{ content = Core.bundle.format("setting.hao1337.ui.restart.label"); }});
            t.checkPref("hao1337.gameplay.erekir.vault-bigger", false);
            t.checkPref("hao1337.gameplay.erekir.heat-generator", false);
            t.checkPref("hao1337.gameplay.erekir.slag-centrifuge", false);
            t.checkPref("hao1337.gameplay.erekir.hidden-item", true);
            t.checkPref("hao1337.gameplay.erekir.hidden-liquid", true);


            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.other"); }});
            /**
             * [E] java.io.IOException: Invalid schematic: Too large (max possible size is 128x128)
	         * at mindustry.game.Schematics.read(Schematics.java:545)
	         * at mindustry.game.Schematics.read(Schematics.java:525)
	         * at mindustry.game.Schematics.loadFile(Schematics.java:138)
             */
            t.sliderPref("hao1337.sechematic.size", 64, 32, 128, n -> n + "×" + n);
            t.checkPref("hao1337.gameplay.experimental", false);
            t.checkPref("hao1337.mod.experimental", false);
            t.checkPref("hao1337.toggle.autoupdate", true);
        });
    }
}
