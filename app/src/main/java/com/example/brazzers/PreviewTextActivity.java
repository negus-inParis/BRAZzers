package com.example.brazzers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


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
    private View btnGenerateFlashcards;
    private TextView btnModeIdentify, btnModeMC;

    private boolean isMCMode = false;
    private static final int MAX_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_text);

        etExtractedText = findViewById(R.id.etExtractedText);
        tvCharCount = findViewById(R.id.tvCharCount);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnGenerateFlashcards = findViewById(R.id.btnGenerateFlashcards);
        btnModeIdentify = findViewById(R.id.btnModeIdentify);
        btnModeMC = findViewById(R.id.btnModeMC);

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

        // Mode toggle
        btnModeIdentify.setOnClickListener(v -> setMode(false));
        btnModeMC.setOnClickListener(v -> setMode(true));

        btnGenerateFlashcards.setOnClickListener(v -> {
            String inputText = etExtractedText.getText().toString().trim();
            if (inputText.isEmpty()) {
                CustomToast.show(this, "Text is empty", CustomToast.INFO);
                return;
            }
            showLoading(true);
            generateFlashcardsFromAI(inputText, 0);
        });
    }

    private void setMode(boolean mcMode) {
        isMCMode = mcMode;
        if (mcMode) {
            btnModeMC.setBackgroundResource(R.drawable.bg_mode_selected);
            btnModeMC.setTextColor(getResources().getColor(R.color.text_on_primary));
            btnModeMC.setTypeface(null, android.graphics.Typeface.BOLD);
            btnModeIdentify.setBackgroundResource(R.drawable.bg_mode_unselected);
            btnModeIdentify.setTextColor(getResources().getColor(R.color.text_muted));
            btnModeIdentify.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            btnModeIdentify.setBackgroundResource(R.drawable.bg_mode_selected);
            btnModeIdentify.setTextColor(getResources().getColor(R.color.text_on_primary));
            btnModeIdentify.setTypeface(null, android.graphics.Typeface.BOLD);
            btnModeMC.setBackgroundResource(R.drawable.bg_mode_unselected);
            btnModeMC.setTextColor(getResources().getColor(R.color.text_muted));
            btnModeMC.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void updateCharCount(int length) {
        tvCharCount.setText(String.format(getString(R.string.char_count), length));
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGenerateFlashcards.setClickable(!show);
        btnGenerateFlashcards.setAlpha(show ? 0.5f : 1.0f);
        etExtractedText.setEnabled(!show);
    }

    private void generateFlashcardsFromAI(String text, int retryCount) {
        new Thread(() -> {
            try {
                String systemPrompt;
                if (isMCMode) {
                    systemPrompt = "You are an expert educational assistant. " +
                            "The following text was extracted via OCR and may contain spelling mistakes or misread characters. " +
                            "First, mentally correct any obvious OCR errors based on context. " +
                            "Then generate multiple choice questions from the corrected text. " +
                            "IMPORTANT: Generate AS MANY questions as the content supports. " +
                            "Aim for at least 10-15 questions. Cover EVERY key concept, definition, term, fact, and detail in the text. " +
                            "Do NOT limit yourself to just 5 questions. The more content provided, the more questions you should generate. " +
                            "Return ONLY a JSON object with a single key 'flashcards' containing an array of objects. " +
                            "Each object must have: " +
                            "'front' (the question), " +
                            "'back' (the correct answer text), " +
                            "'choices' (array of exactly 4 answer options as strings, one of which must be the correct answer, randomly positioned). " +
                            "Do NOT prefix choices with A/B/C/D letters. Make wrong answers plausible but clearly incorrect. " +
                            "Use the corrected spelling in your output.";
                } else {
                    systemPrompt = "You are an expert educational assistant. " +
                            "The following text was extracted via OCR and may contain spelling mistakes, " +
                            "misread characters, or garbled words. First, mentally correct any obvious OCR errors " +
                            "based on context (e.g., 'rnachine' should be 'machine', 'cl0se' should be 'close', " +
                            "'irnportant' should be 'important'). " +
                            "Then extract key concepts and create flashcards using the CORRECTED text. " +
                            "IMPORTANT: Generate AS MANY flashcards as the content supports. " +
                            "Aim for at least 10-15 flashcards. Cover EVERY key term, definition, concept, and fact in the text. " +
                            "Do NOT limit yourself to just 5 flashcards. The more content provided, the more flashcards you should create. " +
                            "Return ONLY a JSON object with a single key 'flashcards' containing an array of objects. " +
                            "Each object must have a 'front' (term/question) and 'back' (definition/answer) property. " +
                            "Only use information from the provided text. Use the corrected spelling in your output.";
                }

                // Safely escape text for JSON
                String safeSystem = systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                String safeUser = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

                // Build OpenAI-compatible JSON payload (works with Groq)
                String jsonInput = "{" +
                        "\"model\": \"" + ApiConfig.MODEL + "\"," +
                        "\"messages\": [" +
                        "  {\"role\": \"system\", \"content\": \"" + safeSystem + "\"}," +
                        "  {\"role\": \"user\", \"content\": \"" + safeUser + "\"}" +
                        "]," +
                        "\"response_format\": {\"type\": \"json_object\"}," +
                        "\"max_tokens\": 4096," +
                        "\"temperature\": 0.3" +
                        "}";

                URL url = new URL(ApiConfig.getApiUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + ApiConfig.API_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.getBytes("UTF-8"));
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

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray choices = jsonObject.getJSONArray("choices");
                    String generatedText = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    runOnUiThread(() -> openFlashcardsScreen(generatedText));
                } else if (responseCode == 429 && retryCount < MAX_RETRIES) {
                    StringBuilder errorBody = new StringBuilder();
                    if (conn.getErrorStream() != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                errorBody.append(line);
                            }
                        }
                    }
                    android.util.Log.w("GroqAPI", "429 hit, retry " + (retryCount + 1) + "/" + MAX_RETRIES + ": " + errorBody);

                    long waitMs = (long) (5000 * Math.pow(2, retryCount));
                    final long finalWaitMs = waitMs;
                    final int nextRetry = retryCount + 1;

                    runOnUiThread(() -> CustomToast.show(this,
                            "Rate limited. Retrying in " + (finalWaitMs / 1000) + "s... (" + nextRetry + "/" + MAX_RETRIES + ")",
                            CustomToast.INFO));

                    Thread.sleep(waitMs);
                    generateFlashcardsFromAI(text, nextRetry);
                } else {
                    // Non-429 error — auto-retry once silently before showing error
                    StringBuilder errorResponse = new StringBuilder();
                    if (conn.getErrorStream() != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                errorResponse.append(line);
                            }
                        }
                    }
                    android.util.Log.e("GroqAPI", "Error Code: " + responseCode + " \nBody: " + errorResponse);

                    if (retryCount == 0) {
                        // Silent auto-retry on first failure
                        android.util.Log.w("GroqAPI", "Auto-retrying after first failure...");
                        Thread.sleep(1500);
                        generateFlashcardsFromAI(text, 1);
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            CustomToast.show(this, getString(R.string.api_error) + " (Code: " + responseCode + ")", CustomToast.ERROR);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false);
                    CustomToast.show(this, getString(R.string.api_error), CustomToast.ERROR);
                });
            }
        }).start();
    }

    private void openFlashcardsScreen(String rawResponse) {
        showLoading(false);
        ArrayList<String> flashcards = parseFlashcards(rawResponse);

        if (flashcards.isEmpty()) {
            CustomToast.show(this, "Could not generate valid flashcards.", CustomToast.ERROR);
            return;
        }

        Intent intent = new Intent(this, FlashcardsActivity.class);
        intent.putStringArrayListExtra("flashcards", flashcards);
        intent.putExtra("from_generation", true);
        intent.putExtra("quiz_mode", isMCMode ? "multiple_choice" : "identification");
        startActivity(intent);
        finish();
    }

    private ArrayList<String> parseFlashcards(String text) {
        ArrayList<String> list = new ArrayList<>();
        try {
            // Clean up possible markdown ticks
            String cleanText = text.trim();
            if (cleanText.startsWith("```json")) cleanText = cleanText.substring(7);
            else if (cleanText.startsWith("```")) cleanText = cleanText.substring(3);
            if (cleanText.endsWith("```")) cleanText = cleanText.substring(0, cleanText.length() - 3);
            cleanText = cleanText.trim();

            JSONArray array;
            if (cleanText.startsWith("{")) {
                JSONObject wrapper = new JSONObject(cleanText);
                if (wrapper.has("flashcards")) {
                    array = wrapper.getJSONArray("flashcards");
                } else if (wrapper.has("cards")) {
                    array = wrapper.getJSONArray("cards");
                } else {
                    String firstKey = wrapper.keys().next();
                    array = wrapper.getJSONArray(firstKey);
                }
            } else {
                array = new JSONArray(cleanText);
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String front = obj.optString("front", "").trim();
                String back = obj.optString("back", "").trim();

                if (front.isEmpty() || back.isEmpty()) continue;

                if (isMCMode) {
                    // MC format: "question|||correct|||choice1|||choice2|||choice3|||choice4"
                    JSONArray choices = obj.optJSONArray("choices");
                    if (choices != null && choices.length() >= 4) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(front).append("|||").append(back);
                        for (int j = 0; j < choices.length(); j++) {
                            sb.append("|||").append(choices.getString(j).trim());
                        }
                        list.add(sb.toString());
                    }
                } else {
                    // Identification format: "Front: Back"
                    list.add(front + ": " + back);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ParseError", "Failed to parse JSON, falling back to basic split", e);
            if (!isMCMode) {
                return parseFallback(text);
            }
        }
        return list;
    }

    private ArrayList<String> parseFallback(String text) {
        ArrayList<String> list = new ArrayList<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- ")) line = line.substring(2).trim();
            if (line.startsWith("* ")) line = line.substring(2).trim();
            if (line.matches("^\\d+\\.\\s+.*")) line = line.replaceFirst("^\\d+\\.\\s+", "");

            if (line.contains(":") && line.length() > 5) {
                list.add(line);
            }
        }
        return list;
    }
}