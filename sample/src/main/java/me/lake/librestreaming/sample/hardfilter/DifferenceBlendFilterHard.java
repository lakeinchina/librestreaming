package me.lake.librestreaming.sample.hardfilter;

import android.graphics.Bitmap;

/**
 * Created by lake on 03/06/16.
 */
public class DifferenceBlendFilterHard extends TowInputFilterHard {
    private static String FRAGMENTSHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vCamTextureCoord;\n" +
            "varying highp vec2 vImageTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform sampler2D uImageTexture;\n" +
            "void main(){\n" +
            "   lowp vec4 c1 = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "   lowp vec4 c2 = texture2D(uImageTexture, vImageTextureCoord);\n" +
            "   gl_FragColor = vec4(abs(c1.rgb - c2.rgb), 1.0);\n" +
            "}";

    public DifferenceBlendFilterHard(Bitmap image) {
        super(null,FRAGMENTSHADER,image);
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
    }
}
