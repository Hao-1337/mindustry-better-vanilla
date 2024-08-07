package hao1337.lib;

import arc.Core;
import arc.func.Floatp;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.ScissorStack;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Scl;
import arc.util.pooling.Pools;
import mindustry.gen.Tex;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;

public class ProgressBar extends Bar {
    private static Rect scissor = new Rect();

    private Floatp fraction;
    private CharSequence name = "";
    private float value, lastValue, blink, outlineRadius;
    private Color blinkColor = new Color(), outlineColor = new Color();

    public ProgressBar(Prov<CharSequence> name, Color color, Floatp fraction) {
        this.fraction = fraction;
        this.name = name.get();
        this.blinkColor.set(color);
        lastValue = value = fraction.get();
        setColor(color);

        update(() -> {
            this.name = name.get();
        });
    }

    @Override
    public void reset(float value) {
        this.value = lastValue = blink = value;
    }

    @Override
    public void set(Prov<String> name, Floatp fraction, Color color) {
        this.fraction = fraction;
        this.lastValue = fraction.get();
        this.blinkColor.set(color);
        setColor(color);
        update(() -> this.name = name.get());
    }

    @Override
    public void snap() {
        lastValue = value = fraction.get();
    }

    @Override
    public ProgressBar outline(Color color, float stroke) {
        outlineColor.set(color);
        outlineRadius = Scl.scl(stroke);
        return this;
    }

    @Override
    public void flash() {
        blink = 1f;
    }

    @Override
    public ProgressBar blink(Color color) {
        blinkColor.set(color);
        return this;
    }

    @Override
    public void draw() {
        if (fraction == null)
            return;

        float computed = fraction.get();

        if (lastValue > computed) {
            blink = 1f;
            lastValue = computed;
        }

        if (Float.isNaN(lastValue))
            lastValue = 0;
        if (Float.isInfinite(lastValue))
            lastValue = 1f;
        if (Float.isNaN(value))
            value = 0;
        if (Float.isInfinite(value))
            value = 1f;
        if (Float.isNaN(computed))
            computed = 0;
        if (Float.isInfinite(computed))
            computed = 1f;

        blink = Mathf.lerpDelta(blink, 0f, 0.2f);
        value = Mathf.lerpDelta(value, computed, 0.15f);

        Drawable bar = Tex.bar;

        if (outlineRadius > 0) {
            Draw.color(outlineColor);
            bar.draw(x - outlineRadius, y - outlineRadius, width + outlineRadius * 2, height + outlineRadius * 2);
        }

        Draw.colorl(0.1f);
        Draw.alpha(parentAlpha);
        bar.draw(x, y, width, height);
        Draw.color(color, blinkColor, blink);
        Draw.alpha(parentAlpha);

        Drawable top = Tex.barTop;
        float topWidth = width * value;

        if (topWidth > Core.atlas.find("bar-top").width) {
            top.draw(x, y, topWidth, height);
        } else {
            if (ScissorStack.push(scissor.set(x, y, topWidth, height))) {
                top.draw(x, y, Core.atlas.find("bar-top").width, height);
                ScissorStack.pop();
            }
        }

        Draw.color();

        Font font = Fonts.outline;
        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        lay.setText(font, name);

        font.setColor(1f, 1f, 1f, 1f);
        font.getCache().clear();
        font.getCache().addText(name, x + width / 2f - lay.width / 2f, y + height / 2f + lay.height / 2f + 1);
        font.getCache().draw(parentAlpha);

        Pools.free(lay);
    }
}
