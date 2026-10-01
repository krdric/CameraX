package com.rgpt.camerax;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.interfaces.Detector;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "StudyCameraX";
    private static final long WARNING_HYSTERESIS_MS = 800; // 800ms threshold before playing sound

    private PreviewView previewView;
    private OverlayView overlayView;
    private View statusIndicator;
    private TextView tvStatusTitle;
    private TextView tvStatusMessage;
    private MaterialButton btnToggleSound;
    private MaterialButton btnSwitchCamera;

    private ExecutorService cameraExecutor;
    private ToneGenerator toneGenerator;

    private boolean isSoundEnabled = true;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    private FaceDetector faceDetector;
    private ObjectDetector objectDetector;
    private ProcessCameraProvider cameraProvider;

    private long warningStartTime = 0;
    private long lastToneTime = 0;
    private OverlayView.Status currentStatus = OverlayView.Status.NO_FACE;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required for study monitoring", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        statusIndicator = findViewById(R.id.statusIndicator);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnToggleSound = findViewById(R.id.btnToggleSound);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e);
        }

        setupDetectors();
        setupControls();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupDetectors() {
        FaceDetectorOptions faceOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();
        faceDetector = FaceDetection.getClient(faceOptions);

        ObjectDetectorOptions objectOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        objectDetector = ObjectDetection.getClient(objectOptions);
    }

    private void setupControls() {
        btnToggleSound.setOnClickListener(v -> {
            isSoundEnabled = !isSoundEnabled;
            if (isSoundEnabled) {
                btnToggleSound.setText("Sound: ON");
                btnToggleSound.setIconResource(android.R.drawable.ic_lock_silent_mode_off);
            } else {
                btnToggleSound.setText("Sound: OFF");
                btnToggleSound.setIconResource(android.R.drawable.ic_lock_silent_mode);
            }
        });

        btnSwitchCamera.setOnClickListener(v -> {
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }
            startCamera();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error binding camera use cases", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        List<Detector<?>> detectors = Arrays.asList(faceDetector, objectDetector);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new MlKitAnalyzer(detectors, ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        ContextCompat.getMainExecutor(this), this::processAnalysisResult));

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void processAnalysisResult(MlKitAnalyzer.Result result) {
        if (result == null) return;

        List<Face> faces = result.getValue(faceDetector);
        List<DetectedObject> objects = result.getValue(objectDetector);

        int imgWidth = 480;
        int imgHeight = 640;

        List<RectF> objectRects = new ArrayList<>();
        if (objects != null) {
            for (DetectedObject obj : objects) {
                objectRects.add(new RectF(obj.getBoundingBox()));
            }
        }

        OverlayView.Status newStatus = evaluateStatus(faces, objects);

        // Hysteresis logic to prevent false alarm triggers from brief flickers
        long currentTime = System.currentTimeMillis();
        if (newStatus != OverlayView.Status.STUDYING_OK) {
            if (warningStartTime == 0) {
                warningStartTime = currentTime;
            } else if (currentTime - warningStartTime >= WARNING_HYSTERESIS_MS) {
                triggerSoundAlert();
            }
        } else {
            warningStartTime = 0;
        }

        currentStatus = newStatus;
        updateUiState(newStatus);

        boolean isFront = (lensFacing == CameraSelector.LENS_FACING_FRONT);
        overlayView.updateResults(faces, objectRects, imgWidth, imgHeight, isFront, newStatus);
    }

    private OverlayView.Status evaluateStatus(List<Face> faces, List<DetectedObject> objects) {
        // Condition 1: If user moves away from camera (no face detected)
        if (faces == null || faces.isEmpty()) {
            return OverlayView.Status.NO_FACE;
        }

        Face primaryFace = faces.get(0);
        Rect faceBox = primaryFace.getBoundingBox();

        // Condition 2: Face is obstructed / book brought in front of face
        // Check if key landmarks (eyes, nose, mouth) are missing or obscured
        boolean isLandmarkMissing = false;
        if (primaryFace.getLandmark(FaceLandmark.NOSE_BASE) == null
                && primaryFace.getLandmark(FaceLandmark.MOUTH_BOTTOM) == null) {
            isLandmarkMissing = true;
        }

        // Check if an object (e.g. book/hand) overlaps with the face bounding box
        boolean isFaceObstructedByObject = false;
        if (objects != null && !objects.isEmpty()) {
            for (DetectedObject obj : objects) {
                Rect objBox = obj.getBoundingBox();
                Rect intersection = new Rect();
                if (intersection.setIntersect(faceBox, objBox)) {
                    float overlapArea = (float) (intersection.width() * intersection.height());
                    float faceArea = (float) (faceBox.width() * faceBox.height());
                    if (faceArea > 0 && (overlapArea / faceArea) > 0.15f) {
                        isFaceObstructedByObject = true;
                        break;
                    }
                }
            }
        }

        if (isLandmarkMissing || isFaceObstructedByObject) {
            return OverlayView.Status.FACE_OBSTRUCTED;
        }

        return OverlayView.Status.STUDYING_OK;
    }

    private void triggerSoundAlert() {
        if (!isSoundEnabled) return;

        long now = System.currentTimeMillis();
        if (now - lastToneTime > 500) { // Play beep tone every 500ms while alert is active
            lastToneTime = now;
            if (toneGenerator != null) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 250);
                } catch (Exception e) {
                    Log.e(TAG, "Error playing tone", e);
                }
            }
        }
    }

    private void updateUiState(OverlayView.Status status) {
        switch (status) {
            case STUDYING_OK:
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_green);
                tvStatusTitle.setText("STUDYING - FACE OK");
                tvStatusTitle.setTextColor(0xFF4CAF50);
                tvStatusMessage.setText("Face visible and unobscured. Keep studying!");
                break;

            case NO_FACE:
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_red);
                tvStatusTitle.setText("ALERT: NO FACE DETECTED");
                tvStatusTitle.setTextColor(0xFFF44336);
                tvStatusMessage.setText("You moved away from the camera. Return to study!");
                break;

            case FACE_OBSTRUCTED:
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_yellow);
                tvStatusTitle.setText("ALERT: FACE OBSTRUCTED");
                tvStatusTitle.setTextColor(0xFFFFC107);
                tvStatusMessage.setText("Book or object blocking your face. Keep your face visible!");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}
