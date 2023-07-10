package com.firstcode.qrscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private MyImageAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_View);
        this.getWindow().setFlags(1024, 1024);

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        analyzer = new MyImageAnalyzer(getSupportFragmentManager());

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                    } else {
                        ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                        bindPreview(processCameraProvider);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                    bindPreview(processCameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bindPreview(ProcessCameraProvider processCameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, analyzer);
        processCameraProvider.unbindAll();
        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }
    public class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private final FragmentManager fragmentManager;
        private final BottomDialog bd;

        public MyImageAnalyzer(FragmentManager fragmentManager) {
            this.fragmentManager = fragmentManager;
            bd = new BottomDialog();
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            scanBarCode(image);
        }

        private void scanBarCode(ImageProxy image) {
            @SuppressLint("UnsafeOptInUsageError")
            Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                BarcodeScannerOptions options =
                        new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                        Barcode.FORMAT_QR_CODE,
                                        Barcode.FORMAT_ALL_FORMATS,
                                        Barcode.FORMAT_AZTEC)
                                .build();
                BarcodeScanner scanner = BarcodeScanning.getClient(options);
                Task<List<Barcode>> result = scanner.process(inputImage)
                        .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes) {
                                readerBarcodeData(barcodes);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Failed to read the QR code", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Barcode>> task) {
                                image.close();
                            }
                        });
            }
        }

        private void readerBarcodeData(List<Barcode> barcodes) {
            for (Barcode barcode : barcodes) {
                int valueType = barcode.getValueType();
                switch (valueType) {
                    case Barcode.TYPE_WIFI:
                        String ssid = Objects.requireNonNull(barcode.getWifi()).getSsid();
                        String password = barcode.getWifi().getPassword();
                        int type = barcode.getWifi().getEncryptionType();
                        String wifiInfo = "SSID: " + ssid + "\nPassword: " + password;
                        bd.setWifiInfo(wifiInfo);
                        if (!bd.isAdded()) {
                            bd.show(fragmentManager, "");
                        }
                        break;
                    case Barcode.TYPE_URL:
                        String url = Objects.requireNonNull(barcode.getUrl()).getUrl();
                        bd.setFetchUrl(url);
                        if (!bd.isAdded()) {
                            bd.show(fragmentManager, "");
                        }
                        break;
                }
            }
        }
    }
}