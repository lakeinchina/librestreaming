package me.lake.librestreaming.sample.softfilter;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;

/**
 * Created by lake on 16-4-1.
 */
public class GrayFilterSoft extends BaseSoftVideoFilter {
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        System.arraycopy(orignBuff,0,targetBuff,0,SIZE_Y);
        for (int i = SIZE_Y; i < SIZE_TOTAL; i++) {
            targetBuff[i] = 127;
        }
        return true;
    }
}
