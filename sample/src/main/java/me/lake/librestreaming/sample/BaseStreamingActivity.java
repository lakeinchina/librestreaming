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
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;

/**
 * Created by lake on 16-5-31.
 */
public class BaseStreamingActivity extends AppCompatActivity implements RESConnectionListener, TextureView.SurfaceTextureListener, View.OnClickListener {
    private static final String TAG = "RES";
    public static final String DIRECTION = "direction";
    protected RESClient resClient;
    protected TextureView txv_preview;
    protected ListView lv_filter;
    protected SeekBar sb_attr;
    protected SeekBar sb_zoom;
    protected TextView tv_speed;
    protected TextView tv_rtmp;
    protected Handler mainHander;
    protected Button btn_toggle;
    protected boolean started;
    protected int filtermode = RESConfig.FilterMode.SOFT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i.getBooleanExtra(DIRECTION, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        started = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        txv_preview = (TextureView) findViewById(R.id.txv_preview);
        lv_filter = (ListView) findViewById(R.id.lv_filter);
        sb_attr = (SeekBar) findViewById(R.id.sb_attr);
        sb_zoom = (SeekBar) findViewById(R.id.sb_zoom);
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_rtmp = (TextView) findViewById(R.id.tv_rtmp);
        txv_preview.setKeepScreenOn(true);
        txv_preview.setSurfaceTextureListener(this);
        resClient = new RESClient();
        final RESConfig resConfig = RESConfig.obtain();
        resConfig.setFilterMode(filtermode);
        resConfig.setTargetVideoSize(new Size(720, 480));
        resConfig.setBitRate(1000 * 1024);
        resConfig.setRenderingMode(RESConfig.RenderingMode.OpenGLES);
        resConfig.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            resConfig.setFrontCameraDirectionMode(RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
            resConfig.setBackCameraDirectionMode(RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90);
        } else {
            resConfig.setFrontCameraDirectionMode(RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
        }
        resConfig.setRtmpAddr("rtmp://10.57.8.120/live/livestream");
        resConfig.setRtmpAddr("rtmp://upload.rtmp.kukuplay.com/live/a26c42ø");
//        resConfig.setRtmpAddr("rtmp://10.57.9.190/live/test");
        if (!resClient.prepare(resConfig)) {
            resClient = null;
            Log.e(TAG, "prepare,failed!!");
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        resClient.setConnectionListener(this);
        btn_toggle = (Button) findViewById(R.id.btn_toggle);
        btn_toggle.setOnClickListener(this);
        findViewById(R.id.btn_swap).setOnClickListener(this);
        findViewById(R.id.btn_flash).setOnClickListener(this);
        findViewById(R.id.btn_screenshot).setOnClickListener(this);
        mainHander = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                tv_speed.setText("speed=" + (resClient.getAVSpeed() / 1024) + ";");
                sendEmptyMessageDelayed(0, 3000);
            }
        };
        mainHander.sendEmptyMessageDelayed(0, 3000);
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
        if (started) {
            resClient.stop();
        }
        if (resClient != null) {
            resClient.destroy();
        }
        if (mainHander != null) {
            mainHander.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @Override
    public void onOpenConnectionResult(int result) {
        /**
         * result==0 success
         * result!=0 failed
         */
        tv_rtmp.setText("open=" + result);
    }

    @Override
    public void onWriteError(int error) {
        /**
         * failed to write data,maybe restart.
         */
        tv_rtmp.setText("writeError=" + error);
    }

    @Override
    public void onCloseConnectionResult(int result) {
        /**
         * result==0 success
         * result!=0 failed
         */
        tv_rtmp.setText("close=" + result);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.createPreview(surface, width, height);
        }
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
            resClient.destroyPreview();
        }
        return true;
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
                    resClient.start();
                } else {
                    btn_toggle.setText("start");
                    resClient.stop();
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
