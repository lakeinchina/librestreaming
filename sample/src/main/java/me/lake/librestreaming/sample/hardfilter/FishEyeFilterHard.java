package me.lake.librestreaming.sample.hardfilter;

import me.lake.librestreaming.filter.hardvideofilter.OriginalHardVideoFilter;

/**
 * Created by lake on 16-5-31.
 */
public class FishEyeFilterHard extends OriginalHardVideoFilter {
    private static String FRAGMENTSHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "varying highp vec2 vCamTextureCoord;\n" +
            "const highp float PI = 3.1415926535;\n" +
            "const highp float aperture = 180.0;\n" +
            "const highp float apertureHalf = 0.5 * aperture * (PI / 180.0);\n" +
            "const highp float maxFactor = sin(apertureHalf);\n" +
            "void main(){\n" +
            "    vec2 pos = 2.0 * vCamTextureCoord.st - 1.0;\n" +
            "    float l = length(pos);\n" +
            "    if (l > 1.0) {\n" +
            "      gl_FragColor = vec4(0.0,0.0,0.0,1);\n" +
            "    }\n" +
            "    else {\n" +
            "        float x = maxFactor * pos.x;\n" +
            "        float y = maxFactor * pos.y;\n" +
            "        float n = length(vec2(x, y));\n" +
            "        float z = sqrt(1.0 - n * n);\n" +
            "        float r = atan(n, z) / PI;\n" +
            "        float phi = atan(y, x);\n" +
            "        float u = r * cos(phi) + 0.5;\n" +
            "        float v = r * sin(phi) + 0.5;\n" +
            "       gl_FragColor = texture2D(uCamTexture,vec2(u,v));\n" +
            "    }\n" +
            "}";

    public FishEyeFilterHard() {
        super(null, FRAGMENTSHADER);
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
    }
}