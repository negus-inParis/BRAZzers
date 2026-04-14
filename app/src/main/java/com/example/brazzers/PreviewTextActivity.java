package com.example.brazzers;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class PreviewTextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_text);

        EditText etExtractedText = findViewById(R.id.etExtractedText);

        String ocrText = getIntent().getStringExtra("ocr_text");
        if (ocrText != null) {
            etExtractedText.setText(ocrText);
        }
    }
}