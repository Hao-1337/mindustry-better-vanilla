precision mediump float;

varying vec2 v_texCoords;
varying vec4 v_color;

uniform sampler2D u_texture;

uniform float u_progress;
uniform vec4 u_tint;
uniform float u_glow;

uniform float u_vmin;
uniform float u_vmax;

void main() {
    float v = (v_texCoords.y - u_vmin) / (u_vmax - u_vmin);
    v = 1.0 - v;
    v = clamp(v, 0.0, 1.0);

    if(v > u_progress)
        discard;

    vec4 tex = texture2D(u_texture, v_texCoords);
    vec4 col = tex * v_color * u_tint;
    float d = abs(v - u_progress);
    float glow = exp(-d * 120.0);
    col.rgb += glow * u_glow;

    gl_FragColor = vec4(col.rgb, col.a);
}