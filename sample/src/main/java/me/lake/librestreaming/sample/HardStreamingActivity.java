package me.lake.librestreaming.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;

import me.lake.librestreaming.model.RESConfig;

/**
 * Created by lake on 16-5-31.
 */
public class HardStreamingActivity extends BaseStreamingActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        filtermode = RESConfig.FilterMode.HARD;
        super.onCreate(savedInstanceState);
    }
}
