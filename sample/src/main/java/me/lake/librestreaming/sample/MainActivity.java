package me.lake.librestreaming.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_STREAM = 1;
    private static String[] PERMISSIONS_STREAM = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    RadioGroup rg_direction;
    RadioGroup rg_mode;
    EditText et_url;
    boolean authorized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_url = (EditText) findViewById(R.id.et_url);
        rg_direction = (RadioGroup) findViewById(R.id.rg_direction);
        rg_mode = (RadioGroup) findViewById(R.id.rg_mode);
        rg_direction.check(R.id.rb_port);
        rg_mode.check(R.id.rb_hard);
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (authorized) {
                    start();
                } else {
                    Snackbar.make(MainActivity.this.getWindow().getDecorView().getRootView(), "streaming need permissions!", Snackbar.LENGTH_LONG)
                            .setAction("auth", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    verifyPermissions();
                                }
                            }).show();
                }
            }
        });
        verifyPermissions();
    }

    private void start() {
        Intent intent;
        boolean isport = false;
        if (rg_direction.getCheckedRadioButtonId() == R.id.rb_port) {
            isport = true;
        }
        if (rg_mode.getCheckedRadioButtonId() == R.id.rb_hard) {
            intent = new Intent(MainActivity.this, HardStreamingActivity.class);
        } else {
            intent = new Intent(MainActivity.this, SoftStreamingActivity.class);
        }
        intent.putExtra(BaseStreamingActivity.DIRECTION, isport);
        intent.putExtra(BaseStreamingActivity.RTMPADDR, et_url.getText().toString());
        startActivity(intent);
    }

    public void verifyPermissions() {
        int CAMERA_permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        int RECORD_AUDIO_permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_STORAGE_permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (CAMERA_permission != PackageManager.PERMISSION_GRANTED ||
                RECORD_AUDIO_permission != PackageManager.PERMISSION_GRANTED ||
                WRITE_EXTERNAL_STORAGE_permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STREAM,
                    REQUEST_STREAM
            );
            authorized = false;
        } else {
            authorized = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STREAM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                authorized = true;
            }
        }
    }
}