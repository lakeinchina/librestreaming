package me.lake.librestreaming.sample.hardfilter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 26/09/16.
 * Librestreaming project.
 */
public class TextHardFilter extends BaseHardVideoFilter {
    private CharSequence text;
    private int textColor;
    private int textSize;
    private boolean textNeedUpdate;
    private boolean postionNeedUpdate;

    protected int imageTexture = GLESTools.NO_TEXTURE;
    protected Bitmap textBitmap;

    private Rect iconRect;
    private int gravity;
    private int verticalMargin;
    private int horizontalMargin;

    public TextHardFilter(CharSequence text, int textColor, int textSize) {
        iconRect = new Rect(0, 0, 0, 0);
        setText(text, textColor, textSize);
    }

    public TextHardFilter(@Nullable CharSequence text) {
        iconRect = new Rect(0, 0, 0, 0);
        setText(text);
    }

    public void setText(CharSequence charSequence, @ColorInt int textColor, int textSize) {
        this.text = charSequence;
        this.textColor = textColor;
        this.textSize = textSize;
        textNeedUpdate = true;
        postionNeedUpdate = true;
    }

    public void setText(CharSequence charSequence) {
        setText(charSequence, Color.WHITE, 30);
    }

    public void setPostion(int gravity, int verticalMargin, int horizontalMargin) {
        this.gravity = gravity;
        if (this.verticalMargin != verticalMargin || this.horizontalMargin != horizontalMargin) {
            textNeedUpdate = true;
            this.verticalMargin = verticalMargin;
            this.horizontalMargin = horizontalMargin;
        }
        postionNeedUpdate = true;
    }

    private void updateText() {
        if (textNeedUpdate) {
            int w = 0, h = 0;
            TextPaint textPaint = new TextPaint();
            textPaint.setColor(textColor);
            textPaint.setTextSize(textSize);
            textPaint.setAntiAlias(true);
            StaticLayout innerStaticLayout = new StaticLayout(text,
                    textPaint,
                    SIZE_WIDTH - horizontalMargin,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,//相对行间距,字体高度倍数
                    0.0f,//在基础行距上添加多少
                    false);
            int lineCount = innerStaticLayout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                float lineW = innerStaticLayout.getLineWidth(i);
                if (lineW > w) {
                    w = (int) (lineW + 0.5f);
                }
            }
            h = innerStaticLayout.getHeight();
            Canvas innerCanvas;
            if (textBitmap == null || textBitmap.getWidth() != w || textBitmap.getHeight() != h) {
                textBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                if (imageTexture != GLESTools.NO_TEXTURE) {
                    GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
                }
                imageTexture = GLESTools.NO_TEXTURE;
            }
            innerCanvas = new Canvas(textBitmap);
            innerStaticLayout.draw(innerCanvas);
            imageTexture = GLESTools.loadTexture(textBitmap, imageTexture);
            textNeedUpdate = false;
        }
        if (postionNeedUpdate) {
            updatePostion();
            postionNeedUpdate = false;
        }
    }


    protected int glProgram;
    protected int glCamTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected int glImageTextureLoc;
    protected int glImageRectLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "void main(){\n" +
            "   gl_Position= aCamPosition;\n" +
            "   vCamTextureCoord = aCamTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "precision mediump float;\n" +
            "varying mediump vec2 vCamTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform sampler2D uImageTexture;\n" +
            "uniform vec4 imageRect;\n" +
            "void main(){\n" +
            "   lowp vec4 c1 = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "   lowp vec2 vCamTextureCoord2 = vec2(vCamTextureCoord.x,1.0-vCamTextureCoord.y);\n" +
            "   if(vCamTextureCoord2.x>imageRect.r && vCamTextureCoord2.x<imageRect.b && vCamTextureCoord2.y>imageRect.g && vCamTextureCoord2.y<imageRect.a)\n" +
            "   {\n" +
            "        vec2 imagexy = vec2((vCamTextureCoord2.x-imageRect.r)/(imageRect.b-imageRect.r),(vCamTextureCoord2.y-imageRect.g)/(imageRect.a-imageRect.g));\n" +
            "        lowp vec4 c2 = texture2D(uImageTexture, imagexy);\n" +
            "        lowp vec4 outputColor = c2+c1*c1.a*(1.0-c2.a);\n" +
            "        outputColor.a = 1.0;\n" +
            "        gl_FragColor = outputColor;\n" +
            "   }else\n" +
            "   {\n" +
            "       gl_FragColor = c1;\n" +
            "   }\n" +
            "}";

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        glProgram = GLESTools.createProgram(vertexShader_filter, fragmentshader_filter);
        GLES20.glUseProgram(glProgram);
        glCamTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glImageTextureLoc = GLES20.glGetUniformLocation(glProgram, "uImageTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        glImageRectLoc = GLES20.glGetUniformLocation(glProgram, "imageRect");
        textNeedUpdate = true;
        postionNeedUpdate = true;
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        updateText();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glUniform4f(glImageRectLoc, iconRect.left / (float) SIZE_WIDTH,
                iconRect.top / (float) SIZE_HEIGHT,
                iconRect.right / (float) SIZE_WIDTH,
                iconRect.bottom / (float) SIZE_HEIGHT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glCamTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        GLES20.glUniform1i(glImageTextureLoc, 1);
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
        GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
        imageTexture=GLESTools.NO_TEXTURE;
    }

    private void updatePostion() {
        int v, h;
        int top, bottom, left, right;
        v = gravity & Gravity.VERTICAL_MASK;
        h = gravity & Gravity.HORIZONTA_MASK;
        switch (v) {
            case Gravity.CENTER_VERTICAL:
                top = (SIZE_HEIGHT - textBitmap.getHeight()) / 2;
                bottom = top + textBitmap.getHeight();
                break;
            case Gravity.BOTTOM:
                bottom = SIZE_HEIGHT - verticalMargin;
                top = bottom - textBitmap.getHeight();
                break;
            case Gravity.TOP:
            default:
                top = verticalMargin;
                bottom = verticalMargin + textBitmap.getHeight();
                break;
        }
        switch (h) {
            case Gravity.CENTER_HORIZONTAL:
                left = (SIZE_WIDTH - textBitmap.getWidth()) / 2;
                right = left + textBitmap.getWidth();
                break;
            case Gravity.RIGHT:
                right = SIZE_WIDTH - horizontalMargin;
                left = right - textBitmap.getWidth();
                break;
            case Gravity.LEFT:
            default:
                left = horizontalMargin;
                right = horizontalMargin + textBitmap.getWidth();
                break;
        }
        iconRect.set(left, top, right, bottom);
    }

    public class Gravity {
        public static final int TOP = 0x0000;
        public static final int BOTTOM = 0x0001;
        public static final int CENTER_VERTICAL = 0x0010;
        public static final int VERTICAL_MASK = 0x0011;
        public static final int LEFT = 0x0000;
        public static final int RIGHT = 0x0100;
        public static final int CENTER_HORIZONTAL = 0x1000;
        public static final int HORIZONTA_MASK = 0x1100;
    }
}
