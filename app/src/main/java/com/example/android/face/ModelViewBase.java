package com.example.android.face;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Isa Abuljalil with Matriculation number: 13/SCI01/010
 * on 04/19/2017.
 */

public abstract class ModelViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Sample::SurfaceView";

    public Camera mCamera;
    public CameraInfo mCameraInfo;
    private SurfaceHolder mHolder;
    public Camera.Parameters mParams;
    private int mFrameWidth;
    private int mFrameHeight;
    private byte[] mFrame;
    private boolean mThreadRun;
    private boolean faceDetectionRunning;
    private byte[] mBuffer;
    Context mContext;
    private int cam;

    public ModelViewBase(Context context) {
        super(context);
        this.mContext = context;
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public void setPreview() throws IOException {
        mCamera.setPreviewDisplay(null);
        mCameraInfo = new CameraInfo();
    }

    int getFrontCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }

    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        releaseCamera();
        cam = getFrontCameraId();
        Log.i(TAG, "Front Camera ID " + cam);
        // if(cam!=-1)
        mCamera = Camera.open(cam);
        if (mCamera == null) {
            Log.e(TAG, "Can't open camera!");
            return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (ModelViewBase.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    ModelViewBase.this.notify();
                }
                camera.addCallbackBuffer(mBuffer);
            }
        });
        return true;
    }

    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        mThreadRun = false;
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewStopped();
    }

    public void setupCamera(int width, int height) {
        Log.i(TAG, "setupCamera");
        synchronized (this) {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                mFrameWidth = width;
                mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    int minDiff = Integer.MAX_VALUE;
                    for (Camera.Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = size.width;
                            mFrameHeight = size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }

                params.setPreviewSize(getFrameWidth(), getFrameHeight());

                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                mCamera.setParameters(params);
                
                /* Now allocate the buffer */
                params = mCamera.getParameters();
                int size = params.getPreviewSize().width * params.getPreviewSize().height;
                size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8 * 2;
                mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
                mFrame = new byte[size];
                mCamera.addCallbackBuffer(mBuffer);

                try {
                    setPreview();
                } catch (IOException e) {
                    Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
                }

                /* Notify that the preview is about to be started and deliver preview size */
                onPreviewStarted(params.getPreviewSize().width, params.getPreviewSize().height);

                /* Now we can start a preview */
                mCamera.startPreview();
            }
        }
    }

    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        if (_holder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCameraDisplayOrientation((Activity) mContext, cam, mCamera);
        setupCamera(width, height);
        try {
            mCamera.setPreviewDisplay(_holder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.i(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }

//    public int doFaceDetection() {
//        if (faceDetectionRunning) {
//            return 0;
//        }
//
//        if (mParams.getMaxNumDetectedFaces() <= 0) {
//            Log.e(TAG, "Face Detection not supported");
//            return -1;
//        }
//
//        MyFaceDetector mDetectionListener = new MyFaceDetector();
//        mCamera.setFaceDetectionListener(mDetectionListener);
//        mCamera.startFaceDetection();
//        faceDetectionRunning = true;
//        return 1;
//    }
//
//    public int stopFaceDetection() {
//        if (faceDetectionRunning) {
//            mCamera.stopFaceDetection();
//            faceDetectionRunning = false;
//            return 1;
//        }
//        return 0;
//    }

    /* The bitmap returned by this method shall be owned by the child and released in onPreviewStopped() */
    protected abstract Bitmap processFrame(byte[] data);

    /**
     * This method is called when the preview process is being started. It is called before the first frame delivered and processFrame is called
     * It is called with the width and height parameters of the preview process. It can be used to prepare the data needed during the frame processing.
     *
     * @param previewWidth  - the width of the preview frames that will be delivered via processFrame
     * @param previewHeight - the height of the preview frames that will be delivered via processFrame
     */
    protected abstract void onPreviewStarted(int previewWidth, int previewHeight);

    /**
     * This method is called when preview is stopped. When this method is called the preview stopped and all the processing of frames already completed.
     * If the Bitmap object returned via processFrame is cached - it is a good time to recycle it.
     * Any other resources used during the preview can be released.
     */
    protected abstract void onPreviewStopped();

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {
            Bitmap bmp = null;

            synchronized (this) {
                try {
                    this.wait();
                    bmp = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas == null) {
                    try {
                        canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2,
                                (canvas.getHeight() - getFrameHeight()) / 2, null);
                        mHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

//    private class MyFaceDetector implements Camera.FaceDetectionListener {
//
//        @Override
//        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
//            if (faces.length == 0) {
//                Log.i(TAG, "No faces detected");
//            } else if (faces.length > 0) {
//                Log.i(TAG, "Faces Detected = " +
//                        String.valueOf(faces.length));
//
//                List<Rect> faceRects;
//                faceRects = new ArrayList<Rect>();
//
//                for (int i = 0; i < faces.length; i++) {
//                    int left = faces[i].rect.left;
//                    int right = faces[i].rect.right;
//                    int top = faces[i].rect.top;
//                    int bottom = faces[i].rect.bottom;
//
//                    Rect mRect = new Rect(left, top, right, bottom);
//                    faceRects.add(mRect);
//                }
//            }
//        }
//    }
}
