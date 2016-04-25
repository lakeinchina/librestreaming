## Android real-time effect filter rtmp streaming library 
## 安卓实时滤镜RTMP推流库

## Requirements
* Android 4.3 or higher (MediaCodec&surfacetexture)

This library is based on Mediacodec & librtmp.
You can create your custom filter extend with BaseVideoFilter.
There are some demo filter using Java,NDK & RenderScript in sample.
Access filter between acquireVideoFilter & releaseVideoFilter in 3ms.
The colorformat in filter is NV21.

基于Mediacodec和librtmp.
你可以通过继承BaseVideoFilter创建自定义滤镜.
sample中有一些使用Java,NDK和RenderScript的滤镜示例
在acquireVideoFilter和releaseVideoFilter之间更新滤镜的属性,不要超过3毫秒.
滤镜中的颜色格式为NV21

### Sample use:
```java
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
    
        RESConfig resConfig = RESConfig.obtain();
        resConfig.setTargetVideoSize(new Size(720, 480));
        resConfig.setBitRate(1000 * 1000);
        resConfig.setRtmpAddr("rtmp://****");
        if (!resClient.prepare(resConfig)) {
            Log.e("Main", "prepare,failed!!");
        }
        /*
        *resClient.start();//start streaming & preview
        *resClient.stop();//stop streaming & preview
        */
    }
    @Override
    protected void onDestroy() {
        resClient.destroy();
        super.onDestroy();
    }
```

### About
camera preivew using nv21.
mediacodec using COLOR_FormatYUV420SemiPlanar or COLOR_FormatYUV420Planar