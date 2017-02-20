package me.lake.librestreaming.sample.hardfilter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 20/02/17.
 * libREStreaming project.
 */

public class GaussianBlurHardFilter extends BaseHardVideoFilter {
    private int blurRadius;
    protected int glProgram;
    protected int glTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected int glStepLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aCamPosition;\n" +
            "    vCamTextureCoord = aCamTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "#ifdef GAUSSIAN_BLUR_RADIUS\n" +
            "#define RADIUS GAUSSIAN_BLUR_RADIUS\n" +
            "#else\n" +
            "#define RADIUS 10.\n" +
            "#endif\n" +
            "\n" +
            "precision highp float;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform vec2 step;\n" +
            "const int blurRadius = 10;\n" +
            "void main(){\n" +
            "\tvec2 currPos = vCamTextureCoord - (step*RADIUS);\n" +
            "\tvec2 endPos = vCamTextureCoord + (step*RADIUS);\n" +
            "\tvec3 sum=vec3(0.,0.,0.);\n" +
            "    float fact=0.;\n" +
            "    for(float r=-RADIUS;r<=RADIUS;r+=1.)\n" +
            "    {\n" +
            "    \tif(any(lessThanEqual(currPos,vec2(0.,0.)))||any(greaterThanEqual(currPos,vec2(1.,1.))))\n" +
            "    \t{\n" +
            "    \t\tcurrPos+=step;\n" +
            "    \t\tcontinue;\n" +
            "    \t}\n" +
            "    \tfloat gauss = exp(r*r / (-4.*RADIUS));\n" +
            "        sum += pow(texture2D(uCamTexture,currPos).rgb,vec3(2.,2.,2.))*gauss;\n" +
            "        fact += gauss;\n" +
            "        currPos+=step;\n" +
            "    }\n" +
            "    gl_FragColor = vec4(sqrt(sum/fact),1.);\n" +
            "}";
    int[] frameBuffer = new int[1];
    int[] frameBufferTexture = new int[1];

    public GaussianBlurHardFilter(int blurRadius) {
        this.blurRadius = blurRadius;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        String fragShader = "#define GAUSSIAN_BLUR_RADIUS "+blurRadius+".\n"+fragmentshader_filter;
        glProgram = GLESTools.createProgram(vertexShader_filter, fragShader);
        GLES20.glUseProgram(glProgram);
        glTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        glStepLoc = GLES20.glGetUniformLocation(glProgram,"step");
        GLESTools.createFrameBuff(frameBuffer,
                frameBufferTexture,
                SIZE_WIDTH,
                SIZE_HEIGHT);
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES20.glUseProgram(glProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glTextureLoc, 0);
        GLES20.glUniform2f(glStepLoc,1f/SIZE_WIDTH,0f);
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
        //=========================
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0]);
        GLES20.glUniform1i(glTextureLoc, 0);
        GLES20.glUniform2f(glStepLoc,0f,1f/SIZE_HEIGHT);
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
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
        GLES20.glDeleteTextures(1, frameBufferTexture, 0);
    }
}
