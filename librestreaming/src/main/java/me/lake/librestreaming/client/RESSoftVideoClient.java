package me.lake.librestreaming.client;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;
import java.util.List;

import me.lake.librestreaming.core.CameraHelper;
import me.lake.librestreaming.core.RESSoftVideoCore;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESSoftVideoClient implements RESVideoClient {
    RESCoreParameters resCoreParameters;
    private Camera camera;
    private SurfaceTexture videoTexture;
    private int cameraNum;
    private int currentCameraIndex;
    private RESSoftVideoCore softVideoCore;

    public RESSoftVideoClient(RESCoreParameters parameters) {
        resCoreParameters = parameters;
        cameraNum = Camera.getNumberOfCameras();
        currentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        if ((cameraNum - 1) >= resConfig.getDefaultCamera()) {
            currentCameraIndex = resConfig.getDefaultCamera();
        }
        if (null == (camera = createCamera(currentCameraIndex))) {
            LogTools.e("can not open camera");
            return false;
        }
        Camera.Parameters parameters = camera.getParameters();
        CameraHelper.selectCameraPreviewWH(parameters, resCoreParameters, resConfig.getTargetVideoSize());
        if (resCoreParameters.isPortrait) {
            resCoreParameters.videoHeight = resCoreParameters.previewVideoWidth;
            resCoreParameters.videoWidth = resCoreParameters.previewVideoHeight;
        } else {
            resCoreParameters.videoWidth = resCoreParameters.previewVideoWidth;
            resCoreParameters.videoHeight = resCoreParameters.previewVideoHeight;
        }
        CameraHelper.selectCameraFpsRange(parameters, resCoreParameters);
        if (!CameraHelper.selectCameraColorFormat(parameters, resCoreParameters)) {
            LogTools.e("CameraHelper.selectCameraColorFormat,Failed");
            resCoreParameters.dump();
            return false;
        }
        if (!CameraHelper.configCamera(camera, resCoreParameters)) {
            LogTools.e("CameraHelper.configCamera,Failed");
            resCoreParameters.dump();
            return false;
        }
        softVideoCore = new RESSoftVideoCore(resCoreParameters);
        softVideoCore.setCurrentCamera(currentCameraIndex);
        if (!softVideoCore.prepare(resConfig)) {
            return false;
        }
        prepareVideo();
        return true;
    }

    private Camera createCamera(int cameraId) {
        try {
            camera = Camera.open(cameraId);
        } catch (SecurityException e) {
            LogTools.trace("no permission", e);
            return null;
        } catch (Exception e) {
            LogTools.trace("camera.open()failed", e);
            return null;
        }
        try {
            videoTexture = new SurfaceTexture(10);
            camera.setPreviewTexture(videoTexture);
        } catch (IOException e) {
            LogTools.trace(e);
            camera.release();
            return null;
        }
        return camera;
    }

    private boolean prepareVideo() {
        camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
        camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
        return true;
    }

    private boolean startVideo() {
        //some fucking phone release their callback after stopPreview
        //so we set it at startVideo
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (softVideoCore != null && data != null) {
                    softVideoCore.queueVideo(data);
                }
                camera.addCallbackBuffer(data);
            }
        });
        camera.startPreview();
        return true;
    }

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter) {
        if (!startVideo()) {
            resCoreParameters.dump();
            LogTools.e("RESClient,start(),failed");
            return false;
        }
        softVideoCore.start(flvDataCollecter);
        return true;
    }

    @Override
    public boolean stop() {
        softVideoCore.stop();
        camera.stopPreview();
        return true;
    }

    @Override
    public boolean destroy() {
        camera.release();
        softVideoCore.destroy();
        return true;
    }

    @Override
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        softVideoCore.createPreview(surfaceTexture, visualWidth, visualHeight);
    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {
        softVideoCore.updatePreview(visualWidth, visualHeight);
    }

    @Override
    public void destroyPreview() {
        softVideoCore.destroyPreview();
    }

    @Override
    public boolean swapCamera() {
        LogTools.d("RESClient,swapCamera()");
        camera.stopPreview();
        camera.release();
        if (null == (camera = createCamera(currentCameraIndex = (++currentCameraIndex) % cameraNum))) {
            LogTools.e("can not swap camera");
            return false;
        }
        softVideoCore.setCurrentCamera(currentCameraIndex);
        CameraHelper.configCamera(camera, resCoreParameters);
        prepareVideo();
        startVideo();
        return true;
    }

    @Override
    public boolean toggleFlashLight() {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    return true;
                }
            } else if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    return true;
                }
            }
        } catch (Exception e) {
            LogTools.d("toggleFlashLight,failed" + e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public boolean setZoomByPercent(float targetPercent) {
        targetPercent = Math.min(Math.max(0f, targetPercent), 1f);
        Camera.Parameters p = camera.getParameters();
        p.setZoom((int) (p.getMaxZoom() * targetPercent));
        camera.setParameters(p);
        return true;
    }

    @Override
    public BaseVideoFilter acquireVideoFilter() {
        return softVideoCore.acquireVideoFilter();
    }

    @Override
    public void releaseVideoFilter() {
        softVideoCore.releaseVideoFilter();
    }

    @Override
    public void setVideoFilter(BaseVideoFilter baseVideoFilter) {
        softVideoCore.setVideoFilter(baseVideoFilter);
    }

    @Override
    public void takeScreenShot(RESScreenShotListener listener) {
        softVideoCore.takeScreenShot(listener);
    }
}
