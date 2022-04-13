package com.dermalog.hardware;


import com.dermalog.android.vision.ScanFragmentActivity;
import com.dermalog.android.vision.ScannerFactory;
import com.dermalog.barcode.common.BarcodeReader;
import com.dermalog.barcode.common.BarcodeReaderFactory;

import java.util.Arrays;

public class BarcodeScanActivity extends ScanFragmentActivity<BarcodeReader> {

    private BarcodeReaderFactory factory = new BarcodeReaderFactory();

    @Override
    protected ScannerFactory<BarcodeReader> getScannerFactory() {
        return factory;
    }

    @Override
    protected void onScannerInitialized(BarcodeReader scanner) {
        scanner.setRelativeScanRect(getViewFinder());
        scanner.enableSymbologies(Arrays.asList(BarcodeReader.Symbology.values()));
    }

    @Override
    protected String getSdk() {
        return "zxing";
    }
}
