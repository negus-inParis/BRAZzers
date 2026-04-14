package com.example.brazzers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PreviewTextActivity extends AppCompatActivity {

    private EditText etExtractedText;
    private TextView tvCharCount;
    private LinearLayout loadingOverlay;
    private Button btnGenerateFlashcards;

    // Gemini API Key from the user
    private static final String API_KEY = "AIzaSyCB5TdSVk6WTyIGqYanUHpQCI9M7-BdPXg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_text);

        etExtractedText = findViewById(R.id.etExtractedText);
        tvCharCount = findViewById(R.id.tvCharCount);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnGenerateFlashcards = findViewById(R.id.btnGenerateFlashcards);

        String ocrText = getIntent().getStringExtra("ocr_text");
        if (ocrText != null) {
            etExtractedText.setText(ocrText);
            updateCharCount(ocrText.length());
        }

        etExtractedText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharCount(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnGenerateFlashcards.setOnClickListener(v -> {
            String inputText = etExtractedText.getText().toString().trim();
            if (inputText.isEmpty()) {
                Toast.makeText(this, "Text is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true);
            generateFlashcardsFromAI(inputText);
        });
    }

    private void updateCharCount(int length) {
        tvCharCount.setText(String.format(getString(R.string.char_count), length));
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGenerateFlashcards.setEnabled(!show);
        etExtractedText.setEnabled(!show);
    }

    private void generateFlashcardsFromAI(String text) {
        new Thread(() -> {
            try {
                String prompt = "Convert the following text into flashcards. " +
                        "Format strictly as:\nTerm: Definition\n\nReturn ONLY the flashcards, one per line. No intro, no markdown.\n\nText:\n" + text;

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Safely escape JSON strings
                String safeText = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                String jsonInput = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + safeText + "\" }] }] }";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    // Parse Gemini JSON
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray candidates = jsonObject.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String generatedText = parts.getJSONObject(0).getString("text");

                    runOnUiThread(() -> openFlashcardsScreen(generatedText));
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(this, getString(R.string.api_error) + " (Code: " + responseCode + ")", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.api_error), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void openFlashcardsScreen(String rawResponse) {
        showLoading(false);
        ArrayList<String> flashcards = parseFlashcards(rawResponse);

        if (flashcards.isEmpty()) {
            Toast.makeText(this, "Could not generate valid flashcards.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, FlashcardsActivity.class);
        intent.putStringArrayListExtra("flashcards", flashcards);
        // Indicate this came from Generation so Save button can be shown
        intent.putExtra("from_generation", true); 
        startActivity(intent);
        finish(); // Optionally finish preview to prevent stacking
    }

    private ArrayList<String> parseFlashcards(String text) {
        ArrayList<String> list = new ArrayList<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
            line = line.trim();
            // Remove markdown lists if gemini added them
            if (line.startsWith("- ")) line = line.substring(2).trim();
            if (line.startsWith("* ")) line = line.substring(2).trim();
            
            if (line.contains(":") && line.length() > 5) {
                list.add(line);
            }
        }
        return list;
    }
}