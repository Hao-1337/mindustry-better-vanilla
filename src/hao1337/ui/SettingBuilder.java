package hao1337.ui;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Slider;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.*;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SettingsMenuDialog;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.Setting;
import mindustry.ui.dialogs.SettingsMenuDialog.StringProcessor;

/**
 * SettingBuilder manages the creation and organization of settings for the "Better Vanilla" mod category.
 * 
 * This class provides a fluent API for building custom UI settings elements including titles, labels,
 * spacing, and divider lines. It organizes settings into logical categories for UI customization,
 * gameplay enhancements on Serpulo and Erekir planets, and miscellaneous options.
 *
 * @see SettingsMenuDialog.SettingsTable.Setting
 */
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
    public class SettingLabel extends SettingsMenuDialog.SettingsTable.Setting {
        public String content;

        public SettingLabel() {
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

    public static class SliderSetting extends Setting {
        int def, min, max, step;
        StringProcessor sp;

        public SliderSetting(String name, int def, int min, int max, int step, StringProcessor s){
            super(name);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sp = s;
        }

        @Override
        public void add(SettingsTable table){
            Slider slider = new Slider(min, max, step, false);
            slider.setValue(Core.settings.getInt(name));

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();

            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                Core.settings.put(name, (int)slider.getValue());
                value.setText(sp.get((int)slider.getValue()));
            });

            slider.change();
            addDesc(table.stack(slider, content).fillX().left().padTop(4f).get());
            table.row();
        }
    }

    public void build() {
        Vars.ui.settings.addCategory("Better Vanilla", new TextureRegionDrawable(Icon.settingsSmall), (t) -> {
            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.ui"); height = 0f; }});
            t.pref(new SettingLabel(){{ content = Core.bundle.format("setting.hao1337.ui.landscape"); }});
            t.checkPref("hao1337.ui.coreinf.enable", true);
            t.checkPref("hao1337.ui.unitinf.enable", true);
            t.checkPref("hao1337.ui.timecontrol.enable", true);
            if (Core.settings.getBool("hao1337.mod.experimental")) {
                t.checkPref("hao1337.ui.autodrill.enable", true);
            }

            t.pref(new SettingTitle(){{ desc = Core.bundle.format("hao1337.setting.category.gameplay.serpulo"); }});
            t.pref(new SettingLabel(){{ content = Core.bundle.format("setting.hao1337.ui.restart.label"); }});
            t.checkPref("hao1337.gameplay.serpulo.vault-bigger", false);
            t.checkPref("hao1337.gameplay.serpulo.better-override-dome", false);
            t.checkPref("hao1337.gameplay.serpulo.better-shield", false);
            // t.checkPref("hao1337.gameplay.serpulo.scrap-wall", false); // GONE

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
            t.pref(new SettingLabel(){{ content = Core.bundle.format("setting.hao1337.ui.restart.label"); }});
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
            t.pref(new SliderSetting("hao1337.sechematic.size", 64, 32, 128, 1, n -> n + "×" + n));
            t.checkPref("hao1337.mod.experimental", false);
            t.checkPref("hao1337.toggle.autoupdate", true);
            
            t.pref(new Padding(){{ height = 40f; }});
            t.pref(new SettingLabel(){{ content = Core.bundle.format("setting.hao1337.ui.devtools.label"); }});
            t.checkPref("hao1337.mod.devtools.server", false);
            t.pref(new SliderSetting("hao1337.mod.devtools.port", 1337, 1024, 6400, 1, s -> "127.0.0.1:" + s));
        });
    }
}
