package hao1337.lib;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import mindustry.Vars;

public abstract class RegionChargeShader extends Shader {
    /** Progress get passed to shader, allow range (0.0, 1.0) */
    public float progress;
    /** How this texture region should glow */
    public float glow = 0.8f;
    /** Color tint */
    public Color tint = Color.navy;
    /** The texture itself for compute quad coordinate */
    public TextureRegion region;

    public RegionChargeShader() {
        super(Vars.tree.get("shaders/chargeregion.vert"), Vars.tree.get("shaders/chargeregion.frag"));
    }

    /** In some case, you might need to normalize progress. Like [0.0, 1.0] nomalize to [0.5, 1.0]*/
    abstract float normalizeProgress(float progress);

    @Override
    public void apply(){
        setUniformf("u_progress", normalizeProgress(progress));
        setUniformf("u_glow", glow);
        setUniformf("u_tint", tint);
        setUniformf("u_vmin", region.v);
        setUniformf("u_vmax", region.v2);
    }
}