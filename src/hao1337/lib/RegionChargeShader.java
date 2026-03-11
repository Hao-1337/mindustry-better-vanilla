package hao1337.lib;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import arc.graphics.gl.GLVersion.GlType;
import arc.util.Log;
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

    public RegionChargeShader() { super(processVertex(), processFragment()); }

    private static String stripGLES(String shader) {
        boolean found = false;

        if (arc.Core.graphics.getGLVersion().type != GlType.GLES) {
            Log.warn("GL version isn't GLES, try to strip percision!");
            StringBuilder out = new StringBuilder();
            for (String line : shader.split("\n")) {
                if (!found && line.contains("precision"))  {
                    found = true;
                    continue;
                }
                out.append(line).append("\n");
            }
            return out.toString();
        }

        return shader;
    }

    private static String processVertex() {
        return stripGLES(Vars.tree.get("shaders/chargeregion.vert").readString());
    } 

    private static String processFragment() {
        return stripGLES(Vars.tree.get("shaders/chargeregion.frag").readString());
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