package me.lake.librestreaming.sample.hardfilter;

import android.opengl.GLES20;

import me.lake.librestreaming.filter.hardvideofilter.OriginalHardVideoFilter;

/**
 * Created by lake on 14/07/16.
 * Librestreaming project.
 */
public class ColorMixHardFilter extends OriginalHardVideoFilter {
    private static String fragmentshader = "" +
            "precision highp float;\n" +
            "varying highp vec2 vCamTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform vec4 mixcolor;" +
            "void main(){\n" +
            "    vec4  color = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "    gl_FragColor = vec4(mix(color.rgb,mixcolor.rgb,mixcolor.a),1.0);\n" +
            "}";

    private int mixColorLoc;
    private float r, g, b, a;

    public ColorMixHardFilter(float r, float g, float b, float a) {
        super(null, fragmentshader);
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        mixColorLoc = GLES20.glGetUniformLocation(glProgram, "mixcolor");
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        GLES20.glUniform4f(mixColorLoc, r, g, b, a);
    }

    public void setMixColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
