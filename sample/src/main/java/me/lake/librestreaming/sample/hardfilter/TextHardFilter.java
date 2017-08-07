package me.lake.librestreaming.sample.hardfilter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
    private int textColor=Color.WHITE;
    private int textSize=30;
    private boolean textNeedUpdate;
    private boolean postionNeedUpdate;

    protected int imageTexture = GLESTools.NO_TEXTURE;
    protected Bitmap textBitmap;

    private Rect textRect =new Rect(0, 0, 0, 0);
    private int gravity;
    private int verticalMargin;
    private int horizontalMargin;

    public TextHardFilter(CharSequence text, int textColor, int textSize) {
        setText(text, textColor, textSize);
    }

    public TextHardFilter(@Nullable CharSequence text) {
        setText(text);
    }

    /**
     *  update text.
     * @param charSequence html,spannablestring supported.
     * @param textColor if (textColor == Color.TRANSPARENT), The text color will become inverted.
     * @param textSize text size in pixel
     */
    public void setText(CharSequence charSequence, @ColorInt int textColor, int textSize) {
        this.text = charSequence;
        this.textColor = textColor;
        this.textSize = textSize;
        textNeedUpdate = true;
        postionNeedUpdate = true;
    }

    public void setText(CharSequence charSequence) {
        setText(charSequence, textColor, textSize);
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
            if (textColor == Color.TRANSPARENT) {
                textPaint.setColor(Color.BLACK);
            } else {
                textPaint.setColor(textColor);
            }
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
            innerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
    protected int glInverseColorLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "void main(){\n" +
            "   gl_Position= aCamPosition;\n" +
            "   vCamTextureCoord = aCamTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "precision highp float;\n" +
            "varying highp vec2 vCamTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform sampler2D uImageTexture;\n" +
            "uniform vec4 imageRect;\n" +
            "uniform int inverseColor;\n" +
            "void main(){\n" +
            "    lowp vec4 c1 = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "    lowp vec2 vCamTextureCoord2 = vec2(vCamTextureCoord.x,1.0-vCamTextureCoord.y);\n" +
            "    if(vCamTextureCoord2.x>imageRect.r && vCamTextureCoord2.x<imageRect.b && vCamTextureCoord2.y>imageRect.g && vCamTextureCoord2.y<imageRect.a)\n" +
            "    {\n" +
            "        vec2 imagexy = vec2((vCamTextureCoord2.x-imageRect.r)/(imageRect.b-imageRect.r),(vCamTextureCoord2.y-imageRect.g)/(imageRect.a-imageRect.g));\n" +
            "        lowp vec4 c2 = texture2D(uImageTexture, imagexy);\n" +
            "        if(inverseColor==1){\n" +
            "            if(c2.a==0.0){\n" +
            "                gl_FragColor = c1;\n" +
            "            }else{\n" +
            "                gl_FragColor = vec4(c1.r-c2.a*2.0*(c1.r-0.5),\n" +
            "                    c1.g-c2.a*2.0*(c1.g-0.5),\n" +
            "                    c1.b-c2.a*2.0*(c1.b-0.5),1.0);\n" +
            "            }\n" +
            "        }else{\n" +
            "            lowp vec4 outputColor = c2+c1*c1.a*(1.0-c2.a);\n" +
            "            outputColor.a = 1.0;\n" +
            "            gl_FragColor = outputColor;\n" +
            "        }\n" +
            "    }else{\n" +
            "        gl_FragColor = c1;\n" +
            "    }\n" +
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
        glInverseColorLoc = GLES20.glGetUniformLocation(glProgram, "inverseColor");
        textNeedUpdate = true;
        postionNeedUpdate = true;
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        updateText();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glUniform1i(glInverseColorLoc, textColor == Color.TRANSPARENT ? 1 : 0);
        GLES20.glUniform4f(glImageRectLoc, textRect.left / (float) SIZE_WIDTH,
                textRect.top / (float) SIZE_HEIGHT,
                textRect.right / (float) SIZE_WIDTH,
                textRect.bottom / (float) SIZE_HEIGHT);
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
        imageTexture = GLESTools.NO_TEXTURE;
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
        textRect.set(left, top, right, bottom);
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
