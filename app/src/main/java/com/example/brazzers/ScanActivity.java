package com.example.brazzers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanActivity extends AppCompatActivity {

    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> documentLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    private GmsDocumentScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        Button btnDocument = findViewById(R.id.btnDocument);
        Button btnCancel = findViewById(R.id.btnCancel);

        GmsDocumentScannerOptions options =
                new GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(10)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build();

        scanner = GmsDocumentScanning.getClient(options);

        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        GmsDocumentScanningResult scanningResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.getData());

                        if (scanningResult != null && scanningResult.getPages() != null
                                && !scanningResult.getPages().isEmpty()) {

                            List<Uri> uris = new ArrayList<>();
                            for (GmsDocumentScanningResult.Page page : scanningResult.getPages()) {
                                uris.add(page.getImageUri());
                            }

                            processMultipleImages(uris);
                        } else {
                            Toast.makeText(this, "No scanned pages found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            processUriWithOCR(imageUri);
                        } else {
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            processUriWithOCR(fileUri);
                        } else {
                            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean grantedImage = result.getOrDefault(getReadMediaImagesPermission(), false);
                    if (Boolean.TRUE.equals(grantedImage)) {
                        openGalleryIntent();
                    } else {
                        Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 300);
            } else {
                openDocumentScanner();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (needsReadMediaPermission()) {
                permissionLauncher.launch(new String[]{getReadMediaImagesPermission()});
            } else {
                openGalleryIntent();
            }
        });

        btnDocument.setOnClickListener(v -> openDocumentIntent());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void openDocumentScanner() {
        Task<android.content.IntentSender> task = scanner.getStartScanIntent(this);
        task.addOnSuccessListener(intentSender ->
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build())
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Scanner failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private boolean needsReadMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private String getReadMediaImagesPermission() {
        return Manifest.permission.READ_MEDIA_IMAGES;
    }

    private void openGalleryIntent() {
        Intent pick = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pick);
    }

    private void openDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        documentLauncher.launch(intent);
    }

    private void processMultipleImages(List<Uri> uris) {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        StringBuilder sb = new StringBuilder();
        AtomicInteger done = new AtomicInteger(0);

        com.google.mlkit.vision.text.TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        for (Uri uri : uris) {
            try {
                InputImage image = InputImage.fromFilePath(this, uri);
                recognizer.process(image)
                        .addOnSuccessListener(res -> {
                            sb.append(cleanExtractedText(res.getText())).append("\n\n");
                            if (done.incrementAndGet() == uris.size()) {
                                openPreviewScreen(sb.toString());
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (done.incrementAndGet() == uris.size()) {
                                openPreviewScreen(sb.toString());
                            }
                        });
            } catch (Exception e) {
                if (done.incrementAndGet() == uris.size()) {
                    openPreviewScreen(sb.toString());
                }
            }
        }
    }

    private void processUriWithOCR(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);

            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(res -> openPreviewScreen(cleanExtractedText(res.getText())))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

        } catch (IOException e) {
            Toast.makeText(this, "Failed to read file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String cleanExtractedText(String text) {
        return text
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void openPreviewScreen(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ScanActivity.this, PreviewTextActivity.class);
        intent.putExtra("ocr_text", extractedText);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 300) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openDocumentScanner();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}