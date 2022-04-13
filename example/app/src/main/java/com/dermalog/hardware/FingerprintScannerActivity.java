package com.dermalog.hardware;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dermalog.android.embeddedbiometrics.DEBCaptureInfo;
import com.dermalog.android.embeddedbiometrics.DEBDeviceEntry;
import com.dermalog.android.embeddedbiometrics.DEBImageSize;
import com.dermalog.android.embeddedbiometrics.DEBLed;
import com.dermalog.android.embeddedbiometrics.DEBLedColor;
import com.dermalog.android.embeddedbiometrics.DEBProperty;
import com.dermalog.android.embeddedbiometrics.DEBTemplateFormat;
import com.dermalog.android.embeddedbiometrics.EmbeddedBiometricsException;
import com.dermalog.android.embeddedbiometrics.EmbeddedBiometricsHandle;
import com.dermalog.android.embeddedbiometrics.EmbeddedBiometricsSDK;
import com.dermalog.android.imageio.ImageIOException;
import com.dermalog.android.imageio.ImageIOFormat;
import com.dermalog.android.imageio.ImageIOSDK;
import com.dermalog.android.usb.IUSBPermission;
import com.dermalog.android.util.DermalogBitmap;

import java.nio.ByteBuffer;

public class FingerprintScannerActivity extends AppCompatActivity {
    static final int SIDE_LEFT = 0;
    static final int SIDE_RIGHT = 1;
    private EmbeddedBiometricsSDK biometricsSdk;
    private ImageIOSDK imageIoSdk;
    private byte[] templateRight;
    private byte[] templateLeft;
    private byte[] wsqRight;
    private byte[] wsqLeft;

    private EmbeddedBiometricsHandle handle;
    private DEBDeviceEntry deviceEntry;
    private AutoCaptureTask autoCaptureTask;

    private Button btnCaptureLeft;
    private Button btnCaptureRight;

    private Button btnCancelLeft;
    private Button btnCancelRight;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_fingerprint_scanner);
        try {
            biometricsSdk = new EmbeddedBiometricsSDK(this);
            biometricsSdk.initialize();
            imageIoSdk = new ImageIOSDK(this);
        } catch (Exception e) {
            Toast.makeText(this, "Error creating SDKs", Toast.LENGTH_LONG).show();
            e.printStackTrace();

            if (biometricsSdk != null) {
                try {
                    biometricsSdk.uninitialize();
                } catch (EmbeddedBiometricsException e1) {
                }
            }
            biometricsSdk = null;
            imageIoSdk = null;
        }

        btnCaptureLeft = (Button) findViewById(R.id.btnStartCaptureLeft);
        btnCaptureRight = (Button) findViewById(R.id.btnStartCaptureRight);
        btnCancelLeft = (Button) findViewById(R.id.btnCancelLeft);
        btnCancelRight = (Button) findViewById(R.id.btnCancelRight);

        btnCaptureRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutoCapture(SIDE_RIGHT);
            }
        });
        btnCaptureLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutoCapture(SIDE_LEFT);
            }
        });


        View.OnClickListener cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCaptureTask.cancel();
            }
        };

        btnCancelLeft.setOnClickListener(cancelListener);
        btnCancelRight.setOnClickListener(cancelListener);

        switchPowerOn();
    }

    void getPermissions() {
        try {
            biometricsSdk.requestUSBPermissions(new IUSBPermission() {
                @Override
                public void permissionsResult(int permission) {
                    switch (permission) {
                        case IUSBPermission.ERR_SUCCESS:
                            try {
                                openScanner();
                            } catch (Exception e) {
                                Toast.makeText(FingerprintScannerActivity.this, "Error opening scanner", Toast.LENGTH_LONG).show();
                                btnCaptureLeft.setEnabled(false);
                                btnCaptureRight.setEnabled(false);
                            }
                            break;

                        case IUSBPermission.ERR_HOST_USB_NOT_SUPPORTED:
                            Toast.makeText(FingerprintScannerActivity.this, "Host USB not supported by device", Toast.LENGTH_LONG).show();
                            break;

                        case IUSBPermission.ERR_PERMISSION:
                            Toast.makeText(FingerprintScannerActivity.this, "No usb permission", Toast.LENGTH_LONG).show();
                            break;

                        case IUSBPermission.ERR_NO_DEVICE:
                            Toast.makeText(FingerprintScannerActivity.this, "No scanner found", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            });
        } catch (EmbeddedBiometricsException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autoCaptureTask != null && !autoCaptureTask.isCancelled()) {
            autoCaptureTask.cancel();
            autoCaptureTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uninitializeSDK();
        switchPowerOff();
    }

    void switchPowerOn() {
        PowerManager powerManager = DeviceManager.getDevice(this).getPowerManager();
        if (powerManager != null && powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_FINGERPRINT_SCANNER)) {
            powerManager.open();
        }
        LocalPowerSwitcherTask task = new LocalPowerSwitcherTask(this, PowerManager.PowerType.USB_FINGERPRINT_SCANNER);
        task.execute(true);
    }

    void switchPowerOff() {
        PowerManager powerManager = DeviceManager.getDevice(this).getPowerManager();
        if (powerManager != null && powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_FINGERPRINT_SCANNER)) {
            powerManager.power(PowerManager.PowerType.USB_FINGERPRINT_SCANNER, false);
            powerManager.close();
        }
    }

    private void uninitializeSDK() {
        closeScanner();


        if (biometricsSdk != null) {
            try {
                biometricsSdk.uninitialize();
            } catch (EmbeddedBiometricsException e) {
                e.printStackTrace();
            }
            biometricsSdk = null;
        }


        if (imageIoSdk != null) {
            try {
                imageIoSdk.uninitialize();
            } catch (ImageIOException e) {
                e.printStackTrace();
            }
            imageIoSdk = null;
        }
    }

    private void openScanner() throws EmbeddedBiometricsException, ImageIOException {
        closeScanner();

        // search scanner and open first
        DEBDeviceEntry[] devices = biometricsSdk.queryDevices();
        if (devices.length > 0) {
            deviceEntry = devices[0];
            handle = biometricsSdk.open(deviceEntry);

            HALDevice halDevice = DeviceManager.getDevice(this);
            if(halDevice.getProperties().getBoolean("scanner.flipVertical", false))
            {
                handle.setProperty(DEBProperty.SCANNER_SET_FLIP_IMAGE, 1);
            }

            if(halDevice.getProperties().getBoolean("scanner.fastCapture", false))
            {
                handle.setProperty(DEBProperty.SCANNER_SET_CAPTURE_MODE, 1);
            }

            if (!imageIoSdk.isInitialized()) {
                imageIoSdk.initialize();
            }
        }
    }

    private void closeScanner() {
        if (handle != null) {
            try {
                handle.dispose();
            } catch (EmbeddedBiometricsException e) {
                e.printStackTrace();
            } finally {
                handle = null;
            }
        }
    }

    private void startAutoCapture(int side) {

        if (biometricsSdk == null) {
            Toast.makeText(FingerprintScannerActivity.this, "SDK is null", Toast.LENGTH_LONG).show();
            return;
        }

        if (!biometricsSdk.isInitialized()) {
            Toast.makeText(FingerprintScannerActivity.this, "SDK not initialized", Toast.LENGTH_LONG).show();
            return;
        }

        if (deviceEntry == null || handle == null) {
            Toast.makeText(FingerprintScannerActivity.this, "Scanner not opened", Toast.LENGTH_LONG).show();
            return;
        }

        if (autoCaptureTask != null) {
            throw new IllegalStateException("trying to start auto capture while another auto capture task is running");
        }
        btnCaptureLeft.setEnabled(false);
        btnCaptureRight.setEnabled(false);

        if (side == SIDE_LEFT) {
            btnCancelLeft.setVisibility(View.VISIBLE);
            btnCaptureLeft.setVisibility(View.GONE);
        } else {
            btnCancelRight.setVisibility(View.VISIBLE);
            btnCaptureRight.setVisibility(View.GONE);
        }

        autoCaptureTask = new AutoCaptureTask(side);
        autoCaptureTask.execute();
    }

    private void onAutoCaptureFinished() {
        btnCaptureLeft.setEnabled(true);
        btnCaptureRight.setEnabled(true);
        btnCaptureLeft.setVisibility(View.VISIBLE);
        btnCaptureRight.setVisibility(View.VISIBLE);

        btnCancelLeft.setVisibility(View.GONE);
        btnCancelRight.setVisibility(View.GONE);
    }

    void setStatusLed(int color) throws EmbeddedBiometricsException {
        if (deviceEntry != null && deviceEntry.getType() == DEBDeviceEntry.TYPE_ZF1) {
            handle.scannerSetLed(DEBLed.STATUS, color);
        }
    }

    void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    class AutoCaptureTask extends AsyncTask<Void, Void, Void> {
        final int side;
        DEBCaptureInfo info = new DEBCaptureInfo();
        Bitmap androidBmp;
        double score;
        int nistQuality;

        StringBuffer sbMessage = new StringBuffer();

        long tStart;

        AutoCaptureTask(int side) {
            this.side = side;
        }

        void startTime()
        {
            tStart = System.currentTimeMillis();
        }

        void logTime(String function)
        {
            sbMessage.append(function).append(": ").append(System.currentTimeMillis() - tStart).append("ms\n");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                StringBuffer sb = new StringBuffer();

                setStatusLed(DEBLedColor.GREEN);
                // allocate buffer for capture image
                DEBImageSize imgSize = handle.scannerGetImageSize();
                ByteBuffer imgBuffer = ByteBuffer.allocateDirect(imgSize.getSizeBIH());

                // start auto capture without timeout
                startTime();
                handle.scannerAutoCapture(0, imgBuffer.array(), info);
                logTime("scannerAutoCapture");

                setStatusLed(DEBLedColor.RED);

                if (!info.isDetect()) {
                    return null;
                }

                androidBmp = DermalogBitmap.toBitmap(imgBuffer.array(), Bitmap.Config.RGB_565);

                startTime();
                nistQuality = handle.imageNISTQuality(imgBuffer.array());
                logTime("imageNISTQuality");


                startTime();
                byte[] template = handle.templateExtract(imgBuffer.array(), DEBTemplateFormat.DERMALOG);
                logTime("templateExtract");

                startTime();
                byte[] wsq = imageIoSdk.convertImage(imgBuffer.array(), ImageIOFormat.WSQ, 5);
                logTime("convertImage");

                if (side == SIDE_LEFT) {
                    templateLeft = template;
                    wsqLeft = wsq;
                } else {
                    templateRight = template;
                    wsqRight = wsq;
                }

                if (templateRight != null && templateLeft != null) {
                    startTime();
                    score = handle.templateVerify(DEBTemplateFormat.DERMALOG, templateLeft, DEBTemplateFormat.DERMALOG, templateRight);
                    logTime("templateVerify");
                }

                setStatusLed(DEBLedColor.OFF);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        boolean cancel() {
            try {
                handle.scannerAutoCaptureCancel();
                setStatusLed(DEBLedColor.OFF);
                autoCaptureTask = null;
            } catch (EmbeddedBiometricsException e) {
                e.printStackTrace();
            }
            return cancel(false);
        }

        @Override
        protected void onCancelled() {
            onAutoCaptureFinished();
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            Log.i("Timings",sbMessage.toString());
            //Toast.makeText(FingerprintScannerActivity.this, sbMessage.toString(), Toast.LENGTH_LONG).show();

            if (side == SIDE_LEFT) {
                ((ImageView) findViewById(R.id.imgPrintLeft)).setImageBitmap(androidBmp);
                ((TextView) findViewById(R.id.nfiqLeft)).setText(String.format(getString(R.string.nfiq), String.valueOf(nistQuality)));
                if (wsqLeft != null) {
                    ((TextView) findViewById(R.id.wsqLeft)).setText(String.format(getString(R.string.wsq), String.valueOf(wsqLeft.length)));
                }
            } else {
                ((ImageView) findViewById(R.id.imgPrintRight)).setImageBitmap(androidBmp);
                ((TextView) findViewById(R.id.nfiqRight)).setText(String.format(getString(R.string.nfiq), String.valueOf(nistQuality)));
                if (wsqRight != null) {
                    ((TextView) findViewById(R.id.wsqRight)).setText(String.format(getString(R.string.wsq), String.valueOf(wsqRight.length)));
                }
            }
            ((TextView) findViewById(R.id.score)).setText(String.valueOf((int) score));
            autoCaptureTask = null;
            onAutoCaptureFinished();
        }
    }

    class LocalPowerSwitcherTask extends PowerSwitcherTask {
        LocalPowerSwitcherTask(Context context, PowerManager.PowerType powerType) {
            super(context, powerType);
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog("Switching on USB power");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            hideProgressDialog();
            getPermissions();
        }
    }
}
