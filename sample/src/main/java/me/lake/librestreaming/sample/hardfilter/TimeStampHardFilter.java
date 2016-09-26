package me.lake.librestreaming.sample.hardfilter;

import android.support.annotation.Nullable;

import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lake on 26/09/16.
 * Librestreaming project.
 */
public class TimeStampHardFilter extends TextHardFilter {
    private static final int UPDATE_INTERVAL = 1000;
    private static final String DEFAULT_TIME_FORMATER = "yyyy-MM-dd kk:mm:ss";
    private long lastUpdateTime;
    private SimpleDateFormat f4;

    public TimeStampHardFilter(@Nullable String formater,int textColor, int textSize) {
        super(DEFAULT_TIME_FORMATER,textColor,textSize);
        formater = formater==null?DEFAULT_TIME_FORMATER:formater;
        f4 = new SimpleDateFormat(formater, Locale.getDefault());
        setText(f4.format(new Date()));
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        if ((System.currentTimeMillis() - lastUpdateTime) > UPDATE_INTERVAL) {
            setText(f4.format(new Date()));
            lastUpdateTime = System.currentTimeMillis();
        }
        super.onDraw(cameraTexture, targetFrameBuffer, shapeBuffer, textrueBuffer);
    }
}
