package com.anprsystemsltd.anpr.android.demo.so.ol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;


public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    static String libraryName = "_HUN_Hungary"; // if the ANPR so library file name is 'lib_HUN_Hungary.so'; lib prefix is necessary!
    static String classString = "com/anprsystemsltd/anpr/android/demo/so/ol/MainActivity;;";    // your activity package

    Context context = this;

    SurfaceHolder mHolder;
    Camera camera;
    List<Camera.Size> supportedPreviewSizes;
    int resolution;
    Camera.Size previewSize;

    String lastRecognitedString = "";
    int recCount = 0;

    String result = "";

    boolean cameraWorking = false;
    boolean cameraEnable = false;

    int previewFormat;

    Vector<Bundle> results;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean error = false;
        try
        {
            File file = new File("/sdcard/class.txt");	// .so lib read class name from this file !!!
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(classString.getBytes());
            fos.close();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(context,"Application requires SD card!", Toast.LENGTH_LONG);
            toast.show();
            finish();
            System.exit(0);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        results = new Vector<Bundle>();

        SurfaceView surface = (SurfaceView) findViewById(R.id.Surface_Main_SurfaceCamera);
        mHolder = surface.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Handler handlerDal = new Handler() {
            public void handleMessage(Message msg) {
                String err = (String)msg.obj;
                if (!err.equals("")) {
                    Toast toast = Toast.makeText(context, "Download licence error!  " + err, Toast.LENGTH_LONG);
                    toast.show();
                }
                LoadAnprModule();
            }
        };

        DownloadAnprLicence dal = new DownloadAnprLicence(context, handlerDal);
        if (dal.CheckLicens() == 1) {	// Valid licence file is present
            LoadAnprModule();
        }
        else {						// download it, if not present
            Thread thread = new Thread(dal);
            thread.start();
        }
    }


    private void LoadAnprModule() {
        System.loadLibrary(libraryName);
        InitCaller(2);
        cameraEnable = true;
    }





    @Override
    public void onPreviewFrame(byte[] aData, Camera camera) {
        if (cameraEnable == false) {
            return;
        }
        if (cameraWorking == false) {
            cameraWorking = true;

            int pw = previewSize.width;
            int ph = previewSize.height;

            int pos = 0;
            byte[] imageData = new byte[ph * pw];
            for (int y = 0; y < ph; y++) {
                for (int x = 0; x < pw; x++) {
                    byte b = aData[y * pw + x];
                    imageData[pos] = b;
                    pos++;
                }
            }

            byte[] ret = ProcessImageCaller(imageData, pw, ph, 1, 1, 0, 1, 1);

            if ((ret[0] == -24) & (ret[1] == 0) & (ret[2] == 0) & (ret[3] == 0) & (ret[20] <= 12)) {
                if (ret[20] > 0) {
                    String recString = "";
                    for (int i = 0; i < ret[20]; i++) {
                        int ca = ret[8 + i];
                        recString = recString + (char) ca;
                    }

                    if (recString.equals(lastRecognitedString)) {
                        recCount++;
                        if (recCount >= 2) {
                            String result = recString + " - ";

                            for (int i = 208; i < 208 + 8; i++) {
                                if (ret[i] != 0) {
                                    result = result + (char)ret[i];
                                }
                                else {
                                    i = 208 + 9;
                                }
                            }

                            long now = System.currentTimeMillis();
                            boolean need = true;
                            for (int i = 0; i < results.size(); i++) {
                                if (results.elementAt(i).getLong("time") < now) {
                                    results.remove(i);
                                }
                                if (i < results.size()) {
                                    if (results.elementAt(i).getString("np").equals(result)) {
                                        need = false;
                                    }
                                }
                            }

                            if (need == true) {
                                Bundle res = new Bundle();
                                res.putString("np", result);
                                res.putLong("time", System.currentTimeMillis() + 5 * 1000);
                                results.add(res);

                                try {
                                    YuvImage yimage = new YuvImage(aData, previewFormat, previewSize.width, previewSize.height, null);
                                    FileOutputStream fos = new FileOutputStream(new File("/sdcard/" + result + "_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                                    yimage.compressToJpeg(new Rect(0, 0, yimage.getWidth(), yimage.getHeight()), 100, fos);
                                    fos.close();
                                }
                                catch (Exception e) {

                                }
                                setTitle(result);

                            }

                            lastRecognitedString = "";
                        }
                    }
                    else {
                        lastRecognitedString = recString;
                        recCount = 0;
                    }
                }

            }
            else {
                camera.stopPreview();
                setTitle("Resolution error!");
            }


            cameraWorking = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        boolean error = false;
        try {
            camera = Camera.open();

            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();

                supportedPreviewSizes = parameters.getSupportedPreviewSizes();

                for (int i = 0; i < supportedPreviewSizes.size(); i++) {
                    Camera.Size si = supportedPreviewSizes.get(i);
                    if (si.width == 640) {
                        resolution = i;
                        i = supportedPreviewSizes.size() + 1;
                    }
                }

                previewSize = supportedPreviewSizes.get(resolution);
                parameters.setPreviewSize(previewSize.width, previewSize.height);

                previewFormat = parameters.getPreviewFormat();

                camera.setParameters(parameters);

                camera.setPreviewCallback((Camera.PreviewCallback) this);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
            else {
                error = true;
            }
        }
        catch (IOException exception) {
            error = true;
        }

        if (error == true) {
            Toast toast = Toast.makeText(context, "Camera error !!!", Toast.LENGTH_LONG);
            toast.show();
            finish();
            System.exit(0);
        }


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Camera.Parameters parameters = camera.getParameters();

        previewSize = supportedPreviewSizes.get(resolution);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(parameters);

        camera.startPreview();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }


    @Override
    public void onBackPressed() {
        finish();
        System.exit(0);
    }


    public native int  InitCaller(int v);

    public native byte[]  ProcessImageCaller(byte[] buffer, int width, int height,
                                             int reserved1, int reserved2,
                                             int useLightCorrection, int detectSquarePlates, int detectWhiteOnBlack);




}
