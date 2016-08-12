package com.desmond.squarecamera.camera1;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.desmond.squarecamera.ImageUtility;
import com.desmond.squarecamera.R;

import java.io.IOException;
import java.util.List;

public class NativeCameraFragment extends Fragment {

    private static final String TAG = NativeCameraFragment.class.getSimpleName();
    // Native camera.
    private Camera mCamera;

    // View to display the camera output.
    private CameraPreview mPreview;

    // Reference to the containing view.
    private View mCameraView;

    /**
     * Default empty constructor.
     */
    public NativeCameraFragment() {
        super();
    }

    /**
     * Static factory method
     *
     * @return
     */
    public static NativeCameraFragment newInstance() {
        NativeCameraFragment fragment = new NativeCameraFragment();
        return fragment;
    }

    /**
     * OnCreateView fragment override
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.easycamera__fragment_camera_native, container, false);

        // Create our Preview view and set it as the content of our activity.
        boolean opened = safeCameraOpenInView(view);

        if (opened == false) {
            Log.d("CameraGuide", "Error, Camera failed to open");
            return view;
        }

        // Trap the capture button.
        View captureButton = view.findViewById(R.id.capture_image_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, pictureCallback);
                    }
                }
        );

        return view;
    }

    /**
     * Recommended "safe" way to open the camera.
     *
     * @param view
     * @return
     */
    private boolean safeCameraOpenInView(View view) {
        boolean qOpened = false;
        releaseCameraAndPreview();
        mCamera = getCameraInstance();
        mCameraView = view;
        qOpened = (mCamera != null);

        if (qOpened == true) {
            mPreview = new CameraPreview(getActivity().getBaseContext(), mCamera, view);
            FrameLayout preview = (FrameLayout) view.findViewById(R.id.fl_camera_preview);
            preview.addView(mPreview);
            mPreview.startCameraPreview();
        }
        return qOpened;
    }

    /**
     * Safe method for getting a camera instance.
     *
     * @return
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
    }

    /**
     * Clear any existing preview / camera.
     */
    private void releaseCameraAndPreview() {

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mPreview != null) {
            mPreview.destroyDrawingCache();
            mPreview.mCamera = null;
        }
    }

    /**
     * Surface on which the camera projects it's capture results. This is derived both from Google's docs and the
     * excellent StackOverflow answer provided below.
     * <p>
     * Reference / Credit: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
     */
    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        // SurfaceHolder
        private SurfaceHolder mHolder;

        // Our Camera.
        private Camera mCamera;

        // Parent Context.
        private Context mContext;

        // Camera Sizing (For rotation, orientation changes)
        private Camera.Size mPreviewSize;

        // List of supported preview sizes
        private List<Camera.Size> mSupportedPreviewSizes;

        // Flash modes supported by this camera
        private List<String> mSupportedFlashModes;

        // View holding this camera.
        private View mCameraView;

        public CameraPreview(Context context, Camera camera, View cameraView) {
            super(context);
            mCameraView = cameraView;
            mContext = context;
            setCamera(camera);
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setKeepScreenOn(true);
        }

        /**
         * Begin the preview of the camera input.
         */
        public void startCameraPreview() {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Extract supported preview and flash modes from the camera.
         *
         * @param camera
         */
        private void setCamera(Camera camera) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            mCamera = camera;
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mSupportedFlashModes = mCamera.getParameters().getSupportedFlashModes();

            // Set the camera to Auto Flash mode.
            if (mSupportedFlashModes != null && mSupportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                mCamera.setParameters(parameters);
            }
            requestLayout();
        }

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         *
         * @param holder
         */
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Dispose of the camera preview.
         *
         * @param holder
         */
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }

        /**
         * React to surface changed events
         *
         * @param holder
         * @param format
         * @param w
         * @param h
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }
            // stop preview before making changes
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                // Set the auto-focus mode to "continuous"
                List<String> focusModes = parameters.getSupportedFocusModes();

                // Preview size must exist.
                if (mPreviewSize != null) {
                    Camera.Size previewSize = mPreviewSize;
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                }
                boolean manualAutoFocus = false;

                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                else {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    manualAutoFocus = true;
                }
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                if (manualAutoFocus) {
                    cameraAutoFocus();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void cameraAutoFocus() {
            if (mCamera != null) {
                mCamera.cancelAutoFocus();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cameraAutoFocus();
                                Toast.makeText(getContext(), "Autofocus", Toast.LENGTH_SHORT).show();
                            }
                        }, 300);
                    }
                });
            }
        }

        /**
         * Calculate the measurements of the layout
         *
         * @param widthMeasureSpec
         * @param heightMeasureSpec
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(width, height);

            if (mSupportedPreviewSizes != null) {
                Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                boolean portrait = true;
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        portrait = true;
                        break;
                    case Surface.ROTATION_90:
                        portrait = false;
                        break;
                    case Surface.ROTATION_180:
                        portrait = true;
                        break;
                    case Surface.ROTATION_270:
                        portrait = false;
                        break;
                }
                if (portrait) {
                    mPreviewSize = getOptimalSize(mSupportedPreviewSizes, width, height);
                } else {
                    mPreviewSize = getOptimalSize(mSupportedPreviewSizes, height, width);

                }
            }
        }

        /**
         * Update the layout based on rotation and orientation changes.
         *
         * @param changed
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            if (changed) {
                final int width = right - left;
                final int height = bottom - top;

                int previewWidth = width;
                int previewHeight = height;

                if (mPreviewSize != null) {
                    Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    setCameraDisplayOrientation(display.getRotation(), 0, mCamera);
                    switch (display.getRotation()) {
                        case Surface.ROTATION_0:
                            previewWidth = mPreviewSize.height;
                            previewHeight = mPreviewSize.width;
                            break;
                        case Surface.ROTATION_90:
                            previewWidth = mPreviewSize.width;
                            previewHeight = mPreviewSize.height;
                            break;
                        case Surface.ROTATION_180:
                            previewWidth = mPreviewSize.height;
                            previewHeight = mPreviewSize.width;
                            break;
                        case Surface.ROTATION_270:
                            previewWidth = mPreviewSize.width;
                            previewHeight = mPreviewSize.height;
                            break;
                    }
                }


                final int scaledChildHeight = previewHeight * width / previewWidth;
                Log.d(NativeCameraFragment.class.getSimpleName(), "ScaledChildHeight " + scaledChildHeight);

                Log.d(NativeCameraFragment.class.getSimpleName(), "l Left position, relative to parent\n" + 0 +
                        "     * @param t Top position, relative to parent\n" + (height - scaledChildHeight) +
                        "     * @param r Right position, relative to parent\n" + width +
                        "     * @param b Bottom position, relative to parent" + height);
                mCameraView.layout(0, height - scaledChildHeight, width, height);
            }
        }

        public void setCameraDisplayOrientation(int rotation,
                                                int cameraId, android.hardware.Camera camera) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
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

        /**
         * Calculate the optimal size of camera preview
         *
         * @param sizes
         * @param w
         * @param h
         * @return
         */
        private Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {


            final double ASPECT_TOLERANCE = 0.2;
            double targetRatio = (double) w / h;
            if (sizes == null)
                return null;
            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;
            int targetHeight = h;
            // Try to find an size match aspect ratio and size
            for (Camera.Size size : sizes) {
//          Log.d("CameraActivity", "Checking size " + size.width + "w " + size.height + "h");
                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
            // Cannot find the one match the aspect ratio, ignore the requirement

            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }


//      Log.d("CameraActivity", "Using size: " + optimalSize.width + "w " + optimalSize.height + "h");
            return optimalSize;
        }

    }

    Handler handler = new Handler();
    /**
     * Picture Callback for handling a picture capture and saving it out to a file.
     */
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Clockwise rotation needed to align the window display to the natural position
                    int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                    int degrees = 0;

                    switch (rotation) {
                        case Surface.ROTATION_0: {
                            degrees = 0;
                            break;
                        }
                        case Surface.ROTATION_90: {
                            degrees = 90;
                            break;
                        }
                        case Surface.ROTATION_180: {
                            degrees = 180;
                            break;
                        }
                        case Surface.ROTATION_270: {
                            degrees = 270;
                            break;
                        }
                    }
                    Bitmap bitmap = ImageUtility.rotatePicture(getContext(), degrees, data);
                    ImageUtility.savePicture(getContext(), bitmap);
                }
            });
            camera.startPreview();
        }
    };


}