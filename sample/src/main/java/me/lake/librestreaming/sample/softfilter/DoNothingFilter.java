package me.lake.librestreaming.sample.softfilter;

import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;

/**
 * Created by lake on 16-4-1.
 */
public class DoNothingFilter extends BaseVideoFilter {
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        return false;
    }
}
