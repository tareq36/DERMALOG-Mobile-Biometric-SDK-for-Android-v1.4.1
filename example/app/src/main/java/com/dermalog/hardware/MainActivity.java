package com.dermalog.hardware;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dermalog.android.vision.ScanFragmentActivity;
import com.dermalog.barcode.common.BarcodeResult;

public class MainActivity extends AppCompatActivity {
    private static final int BARCODE_REQUEST_CODE = 721;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HALDevice device = DeviceManager.getDevice(this);

        findViewById(R.id.btnGoToFingerPrintExample).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FingerprintScannerActivity.class);
                startActivity(intent);
            }
        });

        Button btn = (Button) findViewById(R.id.btnGoToPowerManagerExample);
        if (device.getPowerManager() != null && device.getPowerManager().supportedPowerTypes().size() > 0) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PowerManagerActivity.class);
                    startActivity(intent);
                }
            });
            btn.setEnabled(true);
        }else{
            btn.setVisibility(View.GONE);
        }

        //Thermal printer
        btn = (Button) findViewById(R.id.btnPrinter);
        if (device.getThermalPrinter() != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PrinterActivity.class);
                    startActivity(intent);
                }
            });
            btn.setEnabled(true);
        }else{
            btn.setVisibility(View.GONE);
        }

        //Magnetic card
        btn = (Button) findViewById(R.id.btnMagneticCard);
        if (device.getMagneticCardReader() != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, MagneticCardActivity.class);
                    startActivity(intent);
                }
            });
            btn.setEnabled(true);
        }else{
            btn.setVisibility(View.GONE);
        }

        //card reader
        findViewById(R.id.btnGoToCardReaderManagerExample).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CardReaderManagerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnBarcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BarcodeScanActivity.class);
                intent.putExtra(ScanFragmentActivity.EXTRA_VIEWFINDER_TOP, 20);
                intent.putExtra(ScanFragmentActivity.EXTRA_VIEWFINDER_BOTTOM, 80);
                intent.putExtra(ScanFragmentActivity.EXTRA_VIEWFINDER_LEFT, 0);
                intent.putExtra(ScanFragmentActivity.EXTRA_VIEWFINDER_RIGHT, 100);
                startActivityForResult(intent, BARCODE_REQUEST_CODE);
            }
        });

        String version = com.dermalog.hardware.common.BuildConfig.VERSION_NAME + "\n" + device.getVersionName();

        ((TextView) findViewById(R.id.deviceName)).setText("HAL\n" + device.getDeviceType().name());
        ((TextView) findViewById(R.id.version)).setText(version);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            BarcodeResult result = data.getParcelableExtra("result");
            Toast.makeText(this, result.get(0).text, Toast.LENGTH_LONG).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
