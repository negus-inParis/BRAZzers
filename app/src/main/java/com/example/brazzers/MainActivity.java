package com.example.brazzers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnScan = findViewById(R.id.btnScan);
        Button btnFlashcards = findViewById(R.id.btnFlashcards);

        btnScan.setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));

        // Opens the saved decks list, not FlashcardsActivity directly
        btnFlashcards.setOnClickListener(v -> startActivity(new Intent(this, SavedDecksActivity.class)));
    }
}