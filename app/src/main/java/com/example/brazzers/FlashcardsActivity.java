package com.example.brazzers;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardCounter;
    private TextView tvTerm, tvDefinition;
    private CardView cardFront, cardBack;
    private View cardContainer;
    private Button btnNext, btnPrev, btnSave;

    private ArrayList<String> flashcards;
    private int currentIndex = 0;
    private boolean isFront = true;

    private AnimatorSet frontAnim;
    private AnimatorSet backAnim;
    
    private SharedPreferences sharedPrefs;
    private static final String PREF_NAME = "QuizzersPrefs";
    private static final String KEY_FLASHCARDS = "saved_flashcards";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        sharedPrefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        tvCardCounter = findViewById(R.id.tvCardCounter);
        tvTerm = findViewById(R.id.tvTerm);
        tvDefinition = findViewById(R.id.tvDefinition);
        cardFront = findViewById(R.id.cardFront);
        cardBack = findViewById(R.id.cardBack);
        cardContainer = findViewById(R.id.cardContainer);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnSave = findViewById(R.id.btnSave);

        // Load cameras/distance logic for flip animation
        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        cardFront.setCameraDistance(8000 * scale);
        cardBack.setCameraDistance(8000 * scale);

        // Try to get from Intent
        flashcards = getIntent().getStringArrayListExtra("flashcards");
        boolean fromGeneration = getIntent().getBooleanExtra("from_generation", false);

        if (flashcards == null || flashcards.isEmpty()) {
            // Load from shared preferences
            loadFlashcardsFromLocal();
        }

        if (flashcards == null || flashcards.isEmpty()) {
            tvTerm.setText("");
            tvDefinition.setText(getString(R.string.no_saved_flashcards));
            tvCardCounter.setText("");
            btnNext.setEnabled(false);
            btnPrev.setEnabled(false);
            btnSave.setVisibility(View.GONE);
            return;
        }

        if (fromGeneration) {
            btnSave.setVisibility(View.VISIBLE);
        } else {
            btnSave.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(v -> saveFlashcardsLocally());

        showCurrentCard(false);

        cardContainer.setOnClickListener(v -> flipCard());

        btnNext.setOnClickListener(v -> {
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                showCurrentCard(true);
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showCurrentCard(true);
            }
        });
    }

    private void showCurrentCard(boolean resetToFront) {
        if (flashcards == null || flashcards.isEmpty()) return;

        String card = flashcards.get(currentIndex);
        String term = card;
        String definition = "";

        if (card.contains(":")) {
            String[] parts = card.split(":", 2);
            term = parts[0].trim();
            definition = parts[1].trim();
        }

        tvTerm.setText(term);
        tvDefinition.setText(definition);
        
        tvCardCounter.setText(String.format(getString(R.string.card_counter), currentIndex + 1, flashcards.size()));

        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < flashcards.size() - 1);

        if (resetToFront && !isFront) {
            // Instantly flip back to front without animation when changing cards
            cardFront.setRotationY(0f);
            cardFront.setAlpha(1f);
            cardFront.setVisibility(View.VISIBLE);
            
            cardBack.setRotationY(-180f);
            cardBack.setAlpha(0f);
            cardBack.setVisibility(View.GONE);
            
            isFront = true;
        }
    }

    private void flipCard() {
        if (isFront) {
            cardFront.animate().rotationY(180f).setDuration(300).withEndAction(() -> {
                cardFront.setVisibility(View.GONE);
            }).start();
            
            cardBack.setVisibility(View.VISIBLE);
            cardBack.setRotationY(-180f);
            cardBack.animate().rotationY(0f).setDuration(300).start();
            
            isFront = false;
        } else {
            cardBack.animate().rotationY(180f).setDuration(300).withEndAction(() -> {
                cardBack.setVisibility(View.GONE);
            }).start();
            
            cardFront.setVisibility(View.VISIBLE);
            cardFront.setRotationY(-180f);
            cardFront.animate().rotationY(0f).setDuration(300).start();
            
            isFront = true;
        }
    }

    private void saveFlashcardsLocally() {
        if (flashcards == null || flashcards.isEmpty()) return;
        
        Gson gson = new Gson();
        String json = gson.toJson(flashcards);
        
        sharedPrefs.edit().putString(KEY_FLASHCARDS, json).apply();
        Toast.makeText(this, getString(R.string.flashcards_saved), Toast.LENGTH_SHORT).show();
        btnSave.setVisibility(View.GONE); // Hide after saving
    }

    private void loadFlashcardsFromLocal() {
        String json = sharedPrefs.getString(KEY_FLASHCARDS, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            flashcards = gson.fromJson(json, type);
        } else {
            flashcards = new ArrayList<>();
        }
    }
}