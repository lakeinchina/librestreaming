package me.lake.librestreaming.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.sample.filter.AnimationFilter;
import me.lake.librestreaming.sample.filter.BlackWhiteFilter;
import me.lake.librestreaming.sample.filter.BlurFilter;
import me.lake.librestreaming.sample.filter.DoNothingFilter;
import me.lake.librestreaming.sample.filter.FixYFilter;
import me.lake.librestreaming.sample.filter.GrayFilter;
import me.lake.librestreaming.sample.filter.IconFilter;
import me.lake.librestreaming.sample.filter.SkinBlurFilter;

public class MainActivity extends AppCompatActivity implements RESConnectionListener, TextureView.SurfaceTextureListener {
    RESClient resClient;
    TextureView txv_preview;
    ListView lv_filter;
    FilterAdapter filterAdapter;
    SeekBar sb_attr;
    SeekBar sb_zoom;
    TextView tv_speed;
    TextView tv_rtmp;
    Handler mainHander;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        resConfig.setTargetVideoSize(new Size(720, 480));
        resConfig.setBitRate(1000 * 1000);
        resConfig.setRtmpAddr("rtmp://****");
//        resConfig.setRtmpAddr("rtmp://10.57.9.190/live/test");
        if (!resClient.prepare(resConfig)) {
            Log.e("aa", "prepare,failed!!");
        }
        resClient.setConnectionListener(this);
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resClient.start();
            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resClient.stop();
            }
        });
        findViewById(R.id.btn_swap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resClient.swapCamera();
            }
        });
        findViewById(R.id.btn_flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resClient.toggleFlashLight();
            }
        });
        findViewById(R.id.btn_screenshot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
        /**
         * filters just for demo
         */
        ArrayList<FilterItem> filterItems = new ArrayList<FilterItem>();
        filterItems.add(new FilterItem("nofilter", null));
        filterItems.add(new FilterItem("DoNothingFilter", new DoNothingFilter()));
        filterItems.add(new FilterItem("GrayFilter", new GrayFilter()));
        filterItems.add(new FilterItem("BlackWhiteFilter", new BlackWhiteFilter((byte) 0x80)));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        filterItems.add(new FilterItem("IconFilter", new IconFilter(bitmap)));
        filterItems.add(new FilterItem("BlurFilter", new BlurFilter(this)));
        filterItems.add(new FilterItem("SkinBlurFilter", new SkinBlurFilter(this)));
        filterItems.add(new FilterItem("FixYFilter", new FixYFilter((byte) 0)));
        Bitmap[] ans = new Bitmap[4];
        ans[0] = BitmapFactory.decodeResource(getResources(), R.drawable.animationa);
        ans[1] = BitmapFactory.decodeResource(getResources(), R.drawable.animationb);
        ans[2] = BitmapFactory.decodeResource(getResources(), R.drawable.animationc);
        ans[3] = BitmapFactory.decodeResource(getResources(), R.drawable.animationd);
        filterItems.add(new FilterItem("AnimationFilter", new AnimationFilter(ans)));
        filterAdapter = new FilterAdapter();
        filterAdapter.updateFilters(filterItems);
        lv_filter.setAdapter(filterAdapter);
        lv_filter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (filterAdapter.selectItem(position)) {
                    //changeFilter
                    BaseVideoFilter filter = ((FilterItem) filterAdapter.getItem(position)).filter;
                    resClient.setVideoFilter(filter);
                }
            }
        });
        sb_attr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BaseVideoFilter filter = resClient.acquireVideoFilter();
                if (filter != null) {
                    if (filter instanceof BlackWhiteFilter) {
                        BlackWhiteFilter blackWhiteFilter = (BlackWhiteFilter) filter;
                        blackWhiteFilter.setGap((byte) ((255 * progress) / 100));
                    }
                    if (filter instanceof IconFilter) {
                        IconFilter iconFilter = (IconFilter) filter;
                        iconFilter.setXPos(progress * 5);
                    }
                    if (filter instanceof FixYFilter) {
                        FixYFilter fixYFilter = (FixYFilter) filter;
                        fixYFilter.setY((byte) ((progress / 100.0) * 255));
                    }
                    if (filter instanceof BlurFilter) {
                        BlurFilter blurFilter = (BlurFilter) filter;
                        blurFilter.setRadius(progress / 4);
                    }
                    if (filter instanceof SkinBlurFilter) {
                        SkinBlurFilter skinBlurFilter = (SkinBlurFilter) filter;
                        skinBlurFilter.setRadius(progress / 4);
                    }
                    if (filter instanceof AnimationFilter) {
                        AnimationFilter animationFilter = (AnimationFilter) filter;
                        animationFilter.setXPos(progress * 5);
                    }
                }
                resClient.releaseVideoFilter();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
        resClient.destroy();
        super.onDestroy();
        mainHander.removeCallbacksAndMessages(null);
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
        resClient.createPreview(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        resClient.updatePreview(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        resClient.destroyPreview();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    class FilterItem {
        String name;
        BaseVideoFilter filter;

        public FilterItem(String name, BaseVideoFilter filter) {
            this.name = name;
            this.filter = filter;
        }
    }

    class FilterAdapter extends BaseAdapter {
        private List<FilterItem> filters;
        private int selectIndex = 0;

        FilterAdapter() {
            filters = new ArrayList<>(0);
        }

        public boolean selectItem(int index) {
            if (selectIndex == index) {
                return false;
            }
            selectIndex = index;
            notifyDataSetChanged();
            return true;
        }

        public void updateFilters(List<FilterItem> filters) {
            this.filters = filters == null ? new ArrayList<FilterItem>(0) : filters;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filters.size();
        }

        @Override
        public Object getItem(int position) {
            return filters.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_filter, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }
            if (selectIndex == position) {
                ((ViewHolder) convertView.getTag()).iv_star.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                ((ViewHolder) convertView.getTag()).iv_star.setImageResource(android.R.drawable.btn_star_big_off);
            }

            ((ViewHolder) convertView.getTag()).tv_name.setText(filters.get(position).name);
            return convertView;
        }

        class ViewHolder {
            TextView tv_name;
            ImageView iv_star;

            public ViewHolder(View v) {
                this.tv_name = (TextView) v.findViewById(R.id.tv_name);
                this.iv_star = (ImageView) v.findViewById(R.id.iv_star);
            }
        }
    }
}
