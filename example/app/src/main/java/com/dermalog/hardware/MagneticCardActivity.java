package com.dermalog.hardware;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.dermalog.hardware.magneticcard.MagneticCardException;
import com.dermalog.hardware.magneticcard.MagneticCardReader;
import com.dermalog.hardware.magneticcard.MagneticCardResult;

public class MagneticCardActivity extends AppCompatActivity {

    MagneticCardReader reader;

    TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic_card);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_powermanager_title);
        }

        txt = findViewById(R.id.txtLines);

        try {
            reader = DeviceManager.getDevice(this).getMagneticCardReader();
            reader.open();

            reader.read(new MagneticCardReader.Callback() {
                @Override
                public void onRead(final MagneticCardResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result.error == MagneticCardResult.SUCCESS) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < result.tracs.length; i++) {
                                    if (result.tracs[i] != null)
                                    {
                                        sb.append("Track "+(i+1)+":\n");
                                        sb.append(result.tracs[i]).append("\n\n");
                                    }
                                }
                                txt.setText(sb.toString());
                            } else {
                                txt.setText("Error: " + result.error + " " + (result.throwable != null ? result.throwable.getMessage() : "") );
                            }
                        }
                    });
                }
            }, MagneticCardReader.TIMEOUT_INFINITE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reader != null) {
            try {
                reader.close();
            } catch (MagneticCardException e) {
                e.printStackTrace();
            }
        }
    }
}
