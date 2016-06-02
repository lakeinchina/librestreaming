package me.lake.librestreaming.sample.softfilter;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;

/**
 * Created by lake on 16-4-1.
 */
public class DoNothingFilterSoft extends BaseSoftVideoFilter {
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        return false;
    }
}
