package me.lake.librestreaming.sample.hardfilter;

/**
 * Created by lake on 03/06/16.
 */
public class SobelEdgeDetectionHardVideoFilter extends Base3x3SamplingHardVideoFilter {
    public static final String FRAGMENTSHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n" +
            "\n" +
            "uniform sampler2D uCamTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    float bottomLeftIntensity = texture2D(uCamTexture, bottomLeftTextureCoordinate).r;\n" +
            "    float topRightIntensity = texture2D(uCamTexture, topRightTextureCoordinate).r;\n" +
            "    float topLeftIntensity = texture2D(uCamTexture, topLeftTextureCoordinate).r;\n" +
            "    float bottomRightIntensity = texture2D(uCamTexture, bottomRightTextureCoordinate).r;\n" +
            "    float leftIntensity = texture2D(uCamTexture, leftTextureCoordinate).r;\n" +
            "    float rightIntensity = texture2D(uCamTexture, rightTextureCoordinate).r;\n" +
            "    float bottomIntensity = texture2D(uCamTexture, bottomTextureCoordinate).r;\n" +
            "    float topIntensity = texture2D(uCamTexture, topTextureCoordinate).r;\n" +
            "    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "    float mag = length(vec2(h, v));\n" +
            "    gl_FragColor = vec4(vec3(mag), 1.0);\n" +
            "}";

    public SobelEdgeDetectionHardVideoFilter() {
        super(null, FRAGMENTSHADER);
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
    }
}
