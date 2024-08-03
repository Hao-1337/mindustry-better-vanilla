package hao1337.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
// import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.input.InputHandler;
import mindustry.ui.Styles;

public class TimeControl extends Table {
    public int time = 1;
    public int maxSpeed = 32;
    public int minSpeed = -16;
    public static boolean enableSpeedUp = true;
    public Drawable left = new TextureRegionDrawable(Icon.left);
    public Drawable right = new TextureRegionDrawable(Icon.right);
    public Drawable reset = new TextureRegionDrawable(Icon.refresh);

    private int displaytime = 1;
    private float gametime = 1;
    private Color[] gadient = { Pal.lancerLaser, Pal.accent, Color.valueOf("cc6eaf") };
    private Cell<Label> label;
    private boolean needRebuild = true;
    private boolean ignored = false;

    public void build() {
        background(Styles.black6);
        top();

        table(Tex.pane, t -> {
            t.top().center();
            t.margin(4f);

            t.table(e -> {
                e.top();
                e.label(() -> Core.bundle.format("hao1337.speedlabel"));
                label = e.label(() -> (time < 0 ? "×1/" : "×") + displaytime);
            }).width(80f);

            t.row();

            t.table(null, t1 -> {
                t1.button(left, 10f, () -> update(false)).height(37f)
                        .tooltip(ht -> ht.background(Styles.black6).margin(4f)
                                .add(Core.bundle.format("hao1337.speeddown")).style(Styles.outlineLabel));
                t1.button(reset, 10f, () -> update()).height(37f).padLeft(2.5f).padRight(2.5f)
                        .tooltip(ht -> ht.background(Styles.black6).margin(4f)
                                .add(Core.bundle.format("hao1337.speedreset")).style(Styles.outlineLabel));
                t1.button(right, 10f, () -> update(true)).height(37f)
                        .tooltip(ht -> ht.background(Styles.black6).margin(4f)
                                .add(Core.bundle.format("hao1337.speedup")).style(Styles.outlineLabel));
            });
        }).minHeight(94f).minWidth(154f);

    }

    public void rebuild() {
        reset();

        update(() -> {
            boolean enable = Core.settings.getBool("hao1337.ui.timecontrol.enable");
            // Log.info("Enable: @", enable);
            if (!enable) { 
                if (!ignored) {
                    ignored = true;
                    reset();
                    update();
                }
                return;
            }

            ignored = false;
            boolean disable = shouldDisable();
            // Log.info("Disable: @", disable);

            touchable = disable ? Touchable.disabled : Touchable.enabled;

            if (disable && !needRebuild) {
                reset();
                needRebuild = true;
                return;
            }

            if (needRebuild && !disable) {
                build();
                needRebuild = false;
            }
        });
    }

    boolean shouldDisable() {
        if (!Vars.ui.hudfrag.shown || Vars.ui.minimapfrag.shown())
            return true;
        InputHandler input = Vars.control.input;

        return input.lastSchematic != null || !input.selectPlans.isEmpty();
    }

    void timeUpdate() {
        gametime = Math.abs(time);
        displaytime = (int) (time < 0 ? gametime + 1 : gametime);
        if (time < 0)
            gametime = 1 / (gametime + 1);

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60 * gametime, 3 * gametime));
        if (label != null) label.color(Tmp.c1.lerp(gadient, (time + maxSpeed) / (2 * minSpeed)));
    }

    void update(boolean step) {
        if (step && !enableSpeedUp && time >= 1.0) {
            Vars.ui.showInfoToast(Core.bundle.format("hao1337.ui.speedup.error"), 5);
            return;
        }
        time += step ? 1 : -1;
        if (time > maxSpeed)
            time = maxSpeed;
        if (time < minSpeed + 1)
            time = minSpeed;
        timeUpdate();
    }

    void update() {
        time = 1;
        timeUpdate();
    }
}
