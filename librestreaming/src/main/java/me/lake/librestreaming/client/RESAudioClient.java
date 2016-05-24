package me.lake.librestreaming.client;

import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;

/**
 * Created by lake on 16-5-24.
 */
public interface RESAudioClient {
    boolean prepare(RESConfig resConfig);

    boolean start(RESFlvDataCollecter flvDataCollecter);

    boolean stop();

    boolean destroy();
}
