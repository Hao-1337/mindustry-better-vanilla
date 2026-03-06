package hao1337.ui;

import arc.Core;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.scene.style.Drawable;
import arc.scene.style.ScaledNinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Scl;

/**
 * Utility for loading and caching scaled drawables from the mod's texture
 * atlas.  This helper handles both nine-patch and simple texture regions,
 * applying a global scale factor obtained from the UI scaling system.
 */
public class Htex {
    /** scale factor applied to every drawable returned by {@link #loadUIbg}. */
    protected static float drawableScale;

    /** preloaded drawable for the pane-topright background (common GUI element). */
    public static Drawable paneTopRight;

    /**
     * Load the textures required by the UI and initialise cached drawables.
     * Must be called during mod initialisation before any UI elements attempt
     * to use {@link #paneTopRight} or {@link #loadUIbg}.
     *
     * @param modname prefix used in atlas regions (usually the mod's name)
     */
    public static void load(String modname) {
        drawableScale = Scl.scl(1f);
        paneTopRight = loadUIbg("pane-topright", modname);
    }

    /**
     * Retrieve a drawable from the atlas identified by a name and the mod
     * prefix.  The method automatically handles nine-patches and applies the
     * previously calculated {@link #drawableScale}. If the requested region is
     * absent the result may be {@code null}.
     *
     * @param name    base name of the region (e.g. "pane-topright")
     * @param modname prefix that identifies the mod atlas entries
     * @return a scaled {@link Drawable} instance ready for UI use
     */
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
