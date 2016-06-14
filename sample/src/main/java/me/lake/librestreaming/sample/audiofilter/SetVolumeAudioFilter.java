package me.lake.librestreaming.sample.audiofilter;

import me.lake.librestreaming.filter.softaudiofilter.BaseSoftAudioFilter;

/**
 * Created by lake on 14/06/16.
 * Librestreaming project.
 */
public class SetVolumeAudioFilter extends BaseSoftAudioFilter {
    private float volumeScale=1.0f;

    public SetVolumeAudioFilter() {
    }

    /**
     * @param scale 0.0~
     */
    public void setVolumeScale(float scale) {
        volumeScale = scale;
    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        for (int i = 0; i < SIZE; i += 2) {
            short origin = (short) (((orignBuff[i + 1] << 8) | orignBuff[i] & 0xff));
            origin = (short) (origin * volumeScale);
            orignBuff[i + 1] = (byte) (origin >> 8);
            orignBuff[i] = (byte) (origin);
        }
        return false;
    }
}
