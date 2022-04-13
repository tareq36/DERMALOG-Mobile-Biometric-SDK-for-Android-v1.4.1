package com.dermalog.hardware;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.dermalog.hardware.exceptions.PrinterException;

public class PrinterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer);
        findViewById(R.id.btnPrint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintTask task = new PrintTask();
                task.execute();
            }
        });
    }

    class PrintTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            HALDevice device = DeviceManager.getDevice(PrinterActivity.this);
            PowerManager powerManager = device.getPowerManager();
            if (powerManager != null && powerManager.isPowerTypeSupported(PowerManager.PowerType.THERMAL_PRINTER)) {
                powerManager.open();
                powerManager.power(PowerManager.PowerType.THERMAL_PRINTER, true);
            }

            ThermalPrinter printer = device.getThermalPrinter();
            if (printer == null) {
                return null;
            }
            try {
                printer.claim()
                        .setAlignment(ThermalPrinter.Alignment.Left)
                        .addString(device.getDeviceType().toString())
                        .setAlignment(ThermalPrinter.Alignment.Center)
                        .addString("Printer")
                        .setAlignment(ThermalPrinter.Alignment.Right)
                        .addString("Test")
                        .print()
                        .feedLine(10)
                        .release();
            } catch (PrinterException pe) {
                pe.printStackTrace();
            }

            if (powerManager != null && powerManager.isPowerTypeSupported(PowerManager.PowerType.THERMAL_PRINTER)) {
                powerManager.power(PowerManager.PowerType.THERMAL_PRINTER, false);
                powerManager.close();
            }
            return null;
        }
    }
}
