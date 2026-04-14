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

        btnScan.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ScanActivity.class)));

        btnFlashcards.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FlashcardsActivity.class)));
    }
}