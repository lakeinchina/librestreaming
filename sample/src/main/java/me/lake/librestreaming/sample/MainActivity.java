package me.lake.librestreaming.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    RadioGroup rg_direction;
    RadioGroup rg_mode;
    EditText et_url;

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
                start();
            }
        });
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
        intent.putExtra(BaseStreamingActivity.RTMPADDR,et_url.getText().toString());
        startActivity(intent);
    }
}