package hao1337.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
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
    public Drawable left = new TextureRegionDrawable(Icon.left);
    public Drawable right = new TextureRegionDrawable(Icon.right);
    public Drawable reset = new TextureRegionDrawable(Icon.refresh);

    private int displaytime = 1;
    private float gametime = 1;
    private Color[] gadient = { Pal.lancerLaser, Pal.accent, Color.valueOf("cc6eaf") };
    private Cell<Label> label;

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
                  .tooltip(ht -> ht.background(Styles.black6).margin(4f).add(Core.bundle.format("hao1337.speeddown")).style(Styles.outlineLabel));
                t1.button(reset, 10f, () -> update()).height(37f).padLeft(2.5f).padRight(2.5f)
                  .tooltip(ht -> ht.background(Styles.black6).margin(4f).add(Core.bundle.format("hao1337.speedreset")).style(Styles.outlineLabel));
                t1.button(right, 10f, () -> update(true)).height(37f)
                  .tooltip(ht -> ht.background(Styles.black6).margin(4f).add(Core.bundle.format("hao1337.speedup")).style(Styles.outlineLabel));
            });

        }).minHeight(94f).minWidth(160f);

        visibility = () -> {
            if (!Vars.ui.hudfrag.shown || Vars.ui.minimapfrag.shown())
                return true;
            InputHandler input = Vars.control.input;
            return input.lastSchematic == null || input.selectPlans.isEmpty();
        };

        touchable = Touchable.enabled;

        if (Vars.mobile) moveBy(0, 46f);
    }

    void timeUpdate() {
        gametime = Math.abs(time);
        displaytime = (int)(time < 0 ? gametime + 1 : gametime);
        if (time < 0)
            gametime = 1 / (gametime + 1);

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60 * gametime, 3 * gametime));
        label.color(Tmp.c1.lerp(gadient, (time + maxSpeed) / (2 * minSpeed)));
    }

    void update(boolean step) {
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
