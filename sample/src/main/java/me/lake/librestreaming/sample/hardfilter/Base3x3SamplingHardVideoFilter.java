package me.lake.librestreaming.sample.hardfilter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.OriginalHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 03/06/16.
 * modified base on gpuimage:GPUImage3x3TextureSamplingFilter
 */
public class Base3x3SamplingHardVideoFilter extends BaseHardVideoFilter {
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "uniform highp float texelWidth; \n" +
            "uniform highp float texelHeight; \n" +
            "\n" +
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
            "void main()\n" +
            "{\n" +
            "    gl_Position = aCamPosition;\n" +
            "\n" +
            "    vec2 widthStep = vec2(texelWidth, 0.0);\n" +
            "    vec2 heightStep = vec2(0.0, texelHeight);\n" +
            "    vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" +
            "    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" +
            "\n" +
            "    vCamTextureCoord = aCamTextureCoord.xy;\n" +
            "    leftTextureCoordinate = aCamTextureCoord.xy - widthStep;\n" +
            "    rightTextureCoordinate = aCamTextureCoord.xy + widthStep;\n" +
            "\n" +
            "    topTextureCoordinate = aCamTextureCoord.xy - heightStep;\n" +
            "    topLeftTextureCoordinate = aCamTextureCoord.xy - widthHeightStep;\n" +
            "    topRightTextureCoordinate = aCamTextureCoord.xy + widthNegativeHeightStep;\n" +
            "\n" +
            "    bottomTextureCoordinate = aCamTextureCoord.xy + heightStep;\n" +
            "    bottomLeftTextureCoordinate = aCamTextureCoord.xy - widthNegativeHeightStep;\n" +
            "    bottomRightTextureCoordinate = aCamTextureCoord.xy + widthHeightStep;\n" +
            "}";
    protected String fragmentshader_filter;
    protected int glProgram;
    protected int glTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    private int mUniformTexelWidthLocation;
    private int mUniformTexelHeightLocation;
    private float mTexelWidth;
    private float mTexelHeight;
    private float mLineSize = 1.0f;

    public Base3x3SamplingHardVideoFilter(String vertexShaderCode, String fragmentShaderCode) {
        if (vertexShaderCode != null) {
            vertexShader_filter = vertexShaderCode;
        }
        if (fragmentShaderCode != null) {
            fragmentshader_filter = fragmentShaderCode;
        }
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        glProgram = GLESTools.createProgram(vertexShader_filter, fragmentshader_filter);
        GLES20.glUseProgram(glProgram);
        glTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        mUniformTexelWidthLocation = GLES20.glGetUniformLocation(glProgram, "texelWidth");
        mUniformTexelHeightLocation = GLES20.glGetUniformLocation(glProgram, "texelHeight");
        mTexelWidth = mLineSize / SIZE_WIDTH;
        mTexelHeight = mLineSize / SIZE_HEIGHT;
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1f(mUniformTexelWidthLocation, mTexelWidth);
        GLES20.glUniform1f(mUniformTexelHeightLocation, mTexelHeight);
        GLES20.glUniform1i(glTextureLoc, 0);
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        GLES20.glViewport(0, 0, SIZE_WIDTH, SIZE_HEIGHT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(glCamPostionLoc);
        GLES20.glDisableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
    }
}
