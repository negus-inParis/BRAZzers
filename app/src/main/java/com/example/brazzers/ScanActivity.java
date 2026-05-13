package com.example.brazzers;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.PickVisualMediaRequest;
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
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanActivity extends AppCompatActivity {

    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> documentLauncher;

    private GmsDocumentScanner scanner;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        View cardCamera = findViewById(R.id.cardCamera);
        View cardGallery = findViewById(R.id.cardGallery);
        View cardDocument = findViewById(R.id.cardDocument);
        Button btnCancel = findViewById(R.id.btnCancel);

        GmsDocumentScannerOptions options =
                new GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(false)  // Disabled — gallery access is via the separate "Choose Image" button
                        .setPageLimit(10)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build();

        scanner = GmsDocumentScanning.getClient(options);

        // Document Scanner launcher (camera)
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
                            CustomToast.show(ScanActivity.this, "No scanned pages found", CustomToast.INFO);
                        }
                    }
                });

        // Modern photo picker for Gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        processImageUri(uri);
                    } else {
                        CustomToast.show(ScanActivity.this, "No image selected", CustomToast.INFO);
                    }
                });

        // Document picker (images + PDFs)
        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleDocumentUri(fileUri);
                        } else {
                            CustomToast.show(ScanActivity.this, "No file selected", CustomToast.INFO);
                        }
                    }
                });

        // Debounced click handlers — prevents lag/stacking on rapid taps
        cardCamera.setOnClickListener(v -> {
            if (isProcessing) return;
            isProcessing = true;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 300);
                isProcessing = false;
            } else {
                openDocumentScanner();
            }
        });

        cardGallery.setOnClickListener(v -> {
            if (isProcessing) return;
            isProcessing = true;
            galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        cardDocument.setOnClickListener(v -> {
            if (isProcessing) return;
            isProcessing = true;    
            openDocumentIntent();
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isProcessing = false;
    }

    private void openDocumentScanner() {
        Task<android.content.IntentSender> task = scanner.getStartScanIntent(this);
        task.addOnSuccessListener(intentSender ->
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build())
        ).addOnFailureListener(e ->
                CustomToast.show(this, "Scanner failed: " + e.getMessage(), CustomToast.ERROR)
        );
    }

    private void openDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        documentLauncher.launch(intent);
    }

    /**
     * Route a document Uri based on its MIME type.
     */
    private void handleDocumentUri(Uri uri) {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);

        if (mimeType != null && mimeType.equals("application/pdf")) {
            processPdf(uri);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            processImageUri(uri);
        } else {
            // Fallback: try as image
            processImageUri(uri);
        }
    }

    /**
     * Safely process a single image Uri by reading it via ContentResolver into a Bitmap,
     * then running OCR. Works for any content:// Uri.
     */
    private void processImageUri(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(res -> openPreviewScreen(cleanExtractedText(res.getText())))
                    .addOnFailureListener(e ->
                            CustomToast.show(this, "OCR failed: " + e.getMessage(), CustomToast.ERROR)
                    );
        } catch (IOException e) {
            CustomToast.show(this, "Failed to read image: " + e.getMessage(), CustomToast.ERROR);
        }
    }

    /**
     * Render each PDF page to a Bitmap using PdfRenderer, then OCR each page.
     */
    private void processPdf(Uri uri) {
        new Thread(() -> {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                if (pfd == null) {
                    runOnUiThread(() -> CustomToast.show(this, "Could not open PDF", CustomToast.ERROR));
                    return;
                }

                PdfRenderer renderer = new PdfRenderer(pfd);
                int pageCount = renderer.getPageCount();

                if (pageCount == 0) {
                    renderer.close();
                    pfd.close();
                    runOnUiThread(() -> CustomToast.show(this, "PDF has no pages", CustomToast.INFO));
                    return;
                }

                // Render all pages to bitmaps first (PdfRenderer requires sequential access)
                List<Bitmap> bitmaps = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    // Render at 2x for better OCR quality
                    int width = page.getWidth() * 2;
                    int height = page.getHeight() * 2;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(0xFFFFFFFF); // white background
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    bitmaps.add(bitmap);
                }
                renderer.close();
                pfd.close();

                // Now OCR each bitmap on the main thread (ML Kit needs it)
                runOnUiThread(() -> ocrBitmaps(bitmaps));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        CustomToast.show(this, "PDF processing failed: " + e.getMessage(), CustomToast.ERROR)
                );
            }
        }).start();
    }

    /**
     * OCR a list of Bitmaps sequentially and combine results.
     */
    private void ocrBitmaps(List<Bitmap> bitmaps) {
        StringBuilder sb = new StringBuilder();
        AtomicInteger done = new AtomicInteger(0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Use an array to maintain page order
        String[] results = new String[bitmaps.size()];

        for (int i = 0; i < bitmaps.size(); i++) {
            final int index = i;
            InputImage image = InputImage.fromBitmap(bitmaps.get(i), 0);
            recognizer.process(image)
                    .addOnSuccessListener(res -> {
                        results[index] = cleanExtractedText(res.getText());
                        if (done.incrementAndGet() == bitmaps.size()) {
                            combinePdfResults(results);
                        }
                    })
                    .addOnFailureListener(e -> {
                        results[index] = "";
                        if (done.incrementAndGet() == bitmaps.size()) {
                            combinePdfResults(results);
                        }
                    });
        }
    }

    private void combinePdfResults(String[] results) {
        StringBuilder sb = new StringBuilder();
        for (String page : results) {
            if (page != null && !page.isEmpty()) {
                sb.append(page).append("\n\n");
            }
        }
        openPreviewScreen(sb.toString());
    }

    private void processMultipleImages(List<Uri> uris) {
        StringBuilder sb = new StringBuilder();
        AtomicInteger done = new AtomicInteger(0);
        String[] results = new String[uris.size()];

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        for (int i = 0; i < uris.size(); i++) {
            final int index = i;
            try {
                InputImage image = InputImage.fromFilePath(this, uris.get(i));
                recognizer.process(image)
                        .addOnSuccessListener(res -> {
                            results[index] = cleanExtractedText(res.getText());
                            if (done.incrementAndGet() == uris.size()) {
                                combineResults(results);
                            }
                        })
                        .addOnFailureListener(e -> {
                            results[index] = "";
                            if (done.incrementAndGet() == uris.size()) {
                                combineResults(results);
                            }
                        });
            } catch (Exception e) {
                results[index] = "";
                if (done.incrementAndGet() == uris.size()) {
                    combineResults(results);
                }
            }
        }
    }

    private void combineResults(String[] results) {
        StringBuilder sb = new StringBuilder();
        for (String page : results) {
            if (page != null && !page.isEmpty()) {
                sb.append(page).append("\n\n");
            }
        }
        openPreviewScreen(sb.toString());
    }

    private String cleanExtractedText(String text) {
        return text
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void openPreviewScreen(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            CustomToast.show(this, "No text found", CustomToast.INFO);
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
                CustomToast.show(this, "Camera permission denied", CustomToast.ERROR);
            }
        }
    }
}