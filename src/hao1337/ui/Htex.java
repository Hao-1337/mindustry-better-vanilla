package hao1337.ui;

import arc.Core;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.scene.style.Drawable;
import arc.scene.style.ScaledNinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Scl;

public class Htex {
    protected static float drawableScale;
    public static Drawable paneTopRight;

    public static void load(String modname) {
        drawableScale = Scl.scl(1f);
        paneTopRight = loadUIbg("pane-topright", modname);
    }

    public static Drawable loadUIbg(String name, String modname) {
        AtlasRegion region = Core.atlas.find(modname + "-" + name);

        if (region.splits != null) {
            int[] splits = region.splits;
            NinePatch patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
            int[] pads = region.pads;
            if (pads != null)
                patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
            return new ScaledNinePatchDrawable(patch, drawableScale);
        } else {
            return new TextureRegionDrawable(region, drawableScale);
        }
    }
}
