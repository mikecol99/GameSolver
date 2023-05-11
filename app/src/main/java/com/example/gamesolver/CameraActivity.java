package com.example.gamesolver;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executor;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private ListenableFuture<ProcessCameraProvider> provider;
    private static final int PERMISSION_REQUEST_CODE = 200;
    Button takePicture;
    private PreviewView cameraView;
    private ImageCapture imageCapt;

    private int game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        Bundle extras = getIntent().getExtras();
        game = extras.getInt("game");

        if (! checkPermission())
            requestPermission();

        takePicture = findViewById(R.id.takePicture);
        cameraView = findViewById(R.id.cameraView);

        takePicture.setOnClickListener(this);

        provider = ProcessCameraProvider.getInstance(this);
        provider.addListener( () ->
        {
            try{
                ProcessCameraProvider cameraProvider = provider.get();
                startCamera(cameraProvider);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(CameraActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector camSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());

        imageCapt = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
        cameraProvider.bindToLifecycle(this, camSelector, preview, imageCapt);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @Override
    public void onClick(View view)
    {
        capturePhoto();
    }
    public void capturePhoto() {
        imageCapt.takePicture(
                getExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy  image) {
                        try {
                            Bitmap bitmapImage = cameraView.getBitmap();

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                            byte[] pictureByteArray = stream.toByteArray();
                            Intent myIntent = new Intent(CameraActivity.this, ImageGallery.class);
                            myIntent.putExtra("picture", pictureByteArray);
                            myIntent.putExtra("game", game);
                            CameraActivity.this.startActivity(myIntent);
                            image.close();

                            Toast.makeText(getApplicationContext(), "Picture Taken", Toast.LENGTH_SHORT).show();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}