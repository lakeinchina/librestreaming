package me.lake.librestreaming.sample;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.filter.softaudiofilter.BaseSoftAudioFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.sample.audiofilter.SetVolumeAudioFilter;
import me.lake.librestreaming.sample.ui.AspectTextureView;

/**
 * Created by lake on 16-5-31.
 */
public class BaseStreamingActivity extends AppCompatActivity implements RESConnectionListener, TextureView.SurfaceTextureListener, View.OnClickListener, RESVideoChangeListener {
    private static final String TAG = "RES";
    public static final String DIRECTION = "direction";
    public static final String RTMPADDR = "rtmpaddr";
    protected RESClient resClient;
    protected AspectTextureView txv_preview;
    protected ListView lv_filter;
    protected SeekBar sb_attr;
    protected SeekBar sb_zoom;
    protected SeekBar sb_volume;
    protected TextView tv_speed;
    protected TextView tv_rtmp;
    protected Handler mainHander;
    protected Button btn_toggle;
    protected boolean started;
    protected String rtmpaddr = "rtmp://10.57.9.88/live/livestream";
    protected int filtermode = RESConfig.FilterMode.SOFT;
    RESConfig resConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i.getBooleanExtra(DIRECTION, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if (i.getStringExtra(RTMPADDR) != null && !i.getStringExtra(RTMPADDR).isEmpty()) {
            rtmpaddr = i.getStringExtra(RTMPADDR);
        }
        started = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        txv_preview = (AspectTextureView) findViewById(R.id.txv_preview);
        lv_filter = (ListView) findViewById(R.id.lv_filter);
        sb_attr = (SeekBar) findViewById(R.id.sb_attr);
        sb_zoom = (SeekBar) findViewById(R.id.sb_zoom);
        sb_volume = (SeekBar) findViewById(R.id.sb_volume);
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_rtmp = (TextView) findViewById(R.id.tv_rtmp);
        txv_preview.setKeepScreenOn(true);
        txv_preview.setSurfaceTextureListener(this);
        resClient = new RESClient();
         resConfig = RESConfig.obtain();
        resConfig.setFilterMode(filtermode);
        resConfig.setTargetVideoSize(new Size(720, 480));
        resConfig.setBitRate(750 * 1024);
        resConfig.setVideoFPS(20);
        resConfig.setRenderingMode(RESConfig.RenderingMode.OpenGLES);
        resConfig.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        int frontDirection, backDirection;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
        frontDirection = cameraInfo.orientation;
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        backDirection = cameraInfo.orientation;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            resConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
            resConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270));
        } else {
            resConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180));
            resConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
        }
        resConfig.setRtmpAddr(rtmpaddr);
        if (!resClient.prepare(resConfig)) {
            resClient = null;
            Log.e(TAG, "prepare,failed!!");
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Size s = resClient.getVideoSize();
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) s.getWidth()) / s.getHeight());
        Log.d(TAG, "version=" + resClient.getVertion());
        resClient.setConnectionListener(this);
        resClient.setVideoChangeListener(this);
        btn_toggle = (Button) findViewById(R.id.btn_toggle);
        btn_toggle.setOnClickListener(this);
        findViewById(R.id.btn_swap).setOnClickListener(this);
        findViewById(R.id.btn_flash).setOnClickListener(this);
        findViewById(R.id.btn_screenshot).setOnClickListener(this);
        mainHander = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                tv_speed.setText("byteSpeed=" + (resClient.getAVSpeed() / 1024) + ";drawFPS=" + resClient.getDrawFrameRate() + ";sendFPS=" + resClient.getSendFrameRate() + ";sendbufferfreepercent=" + resClient.getSendBufferFreePercent());
                sendEmptyMessageDelayed(0, 3000);
                if (resClient.getSendBufferFreePercent() <= 0.05) {
                    Toast.makeText(BaseStreamingActivity.this, "sendbuffer is full,netspeed is low!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        mainHander.sendEmptyMessageDelayed(0, 3000);

        resClient.setSoftAudioFilter(new SetVolumeAudioFilter());
        sb_zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                resClient.setZoomByPercent(progress / 100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BaseSoftAudioFilter audioFilter = resClient.acquireSoftAudioFilter();
                if (audioFilter != null) {
                    if (audioFilter instanceof SetVolumeAudioFilter) {
                        SetVolumeAudioFilter blackWhiteFilter = (SetVolumeAudioFilter) audioFilter;
                        blackWhiteFilter.setVolumeScale((float) (progress / 10.0));
                    }
                }
                resClient.releaseSoftAudioFilter();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb_volume.setProgress(10);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mainHander != null) {
            mainHander.removeCallbacksAndMessages(null);
        }
        if (started) {
            resClient.stopStreaming();
        }
        if (resClient != null) {
            resClient.destroy();
        }
        super.onDestroy();
    }

    protected void reStartWithResolution(int w, int h) {
        /*
        //===========don`t interrupt streaming
        resClient.reSetVideoBitrate(1200*1024);
        resClient.reSetVideoSize(new Size(1280,720));
        //===or======interrupt streaming
        if (started) {
            resClient.stopStreaming();
        }
        resClient.stopPreview(false);
        resClient.destroy();
        resConfig.setTargetVideoSize(new Size(w, h));
        resClient.prepare(resConfig);
        resClient.startPreview(texture, sw, sh);
        Size s = resClient.getVideoSize();
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) s.getWidth()) / s.getHeight());
        if (started) {
            resClient.startStreaming();
        }
        //===========
        */
    }

    @Override
    public void onOpenConnectionResult(int result) {
        if (result == 0) {
            Log.e(TAG, "server IP = " + resClient.getServerIpAddr());
        }else {
            Toast.makeText(this, "startfailed", Toast.LENGTH_SHORT).show();
        }
        /**
         * result==0 success
         * result!=0 failed
         */
        tv_rtmp.setText("open=" + result);
    }

    @Override
    public void onWriteError(int errno) {
        if (errno == 9) {
            resClient.stopStreaming();
            resClient.startStreaming();
            Toast.makeText(this, "errno==9,restarting", Toast.LENGTH_SHORT).show();
        }
        /**
         * failed to write data,maybe restart.
         */
        tv_rtmp.setText("writeError=" + errno);
    }

    @Override
    public void onCloseConnectionResult(int result) {
        /**
         * result==0 success
         * result!=0 failed
         */
        tv_rtmp.setText("close=" + result);
    }

    protected SurfaceTexture texture;
    protected int sw,sh;

    @Override
    public void onVideoSizeChanged(int width, int height) {
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) width) / height);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.startPreview(surface, width, height);
        }
        texture = surface;
        sw = width;
        sh = height;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (resClient != null) {
            resClient.stopPreview(true);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_toggle:
                if (!started) {
                    btn_toggle.setText("stop");
                    resClient.startStreaming();
                } else {
                    btn_toggle.setText("start");
                    resClient.stopStreaming();
                }
                started = !started;
                break;
            case R.id.btn_swap:
                resClient.swapCamera();
                break;
            case R.id.btn_flash:
                resClient.toggleFlashLight();
                break;
            case R.id.btn_screenshot:
                resClient.takeScreenShot(new RESScreenShotListener() {
                    @Override
                    public void onScreenShotResult(Bitmap bitmap) {
                        File f = new File("/sdcard/" + System.currentTimeMillis() + "_libres.png");
                        try {
                            if (!f.exists()) {
                                f.createNewFile();
                            }
                            OutputStream outputStream = new FileOutputStream(f);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            outputStream.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
                break;
        }
    }

}
