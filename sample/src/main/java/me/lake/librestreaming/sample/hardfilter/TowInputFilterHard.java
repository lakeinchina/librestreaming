package me.lake.librestreaming.sample.hardfilter;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 03/06/16.
 */
public class TowInputFilterHard extends BaseHardVideoFilter {
    protected int glProgram;
    protected int glCamTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected int glImageTextureLoc;
    protected int glImageTextureCoordLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "attribute vec2 aImageTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "varying vec2 vImageTextureCoord;\n" +
            "void main(){\n" +
            "   gl_Position= aCamPosition;\n" +
            "   vCamTextureCoord = aCamTextureCoord;\n" +
            "   vImageTextureCoord = aImageTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "precision highp float;\n" +
            "varying highp vec2 vCamTextureCoord;\n" +
            "varying highp vec2 vImageTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform sampler2D uImageTexture;\n" +
            "void main(){\n" +
            "   lowp vec4 c1 = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "   lowp vec4 c2 = texture2D(uImageTexture, vImageTextureCoord);\n" +
            "   lowp vec4 outputColor = c2+c1*c1.a*(1.0-c2.a);\n" +
            "   outputColor.a = 1.0;\n" +
            "   gl_FragColor = outputColor;\n" +
            "}";
    protected static float texture2Vertices[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};
    protected FloatBuffer textureImageCoordBuffer;
    protected int imageTexture;
    private Bitmap image;

    public TowInputFilterHard(String vertexShaderCode, String fragmentShaderCode, Bitmap image) {
        this.image = image;
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
        imageTexture = GLESTools.loadTexture(image, GLESTools.NO_TEXTURE);
        glProgram = GLESTools.createProgram(vertexShader_filter, fragmentshader_filter);
        GLES20.glUseProgram(glProgram);
        glCamTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glImageTextureLoc = GLES20.glGetUniformLocation(glProgram, "uImageTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        glImageTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aImageTextureCoord");

        textureImageCoordBuffer = ByteBuffer.allocateDirect(4 * texture2Vertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        textureImageCoordBuffer.put(texture2Vertices);
        textureImageCoordBuffer.position(0);
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glCamTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        GLES20.glUniform1i(glImageTextureLoc, 1);
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glEnableVertexAttribArray(glImageTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        textureImageCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(glImageTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textureImageCoordBuffer);
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
        GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
        textureImageCoordBuffer.clear();
    }
}
