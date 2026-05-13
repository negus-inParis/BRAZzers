package com.example.brazzers;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Immersive fullscreen — hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Load nickname
        SharedPreferences prefs = getSharedPreferences("quizzers_prefs", MODE_PRIVATE);
        String nickname = prefs.getString("user_nickname", "");

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        TextView tvUserName = findViewById(R.id.tvUserName);

        if (nickname != null && !nickname.isEmpty()) {
            tvGreeting.setText("Welcome back 👋");
            tvUserName.setText(nickname + "!");
        } else {
            tvGreeting.setText("Hey there 👋");
            tvUserName.setText("Bestie!");
        }

        // Click handlers
        findViewById(R.id.btnScanWrapper).setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));

        findViewById(R.id.btnFlashcardsWrapper).setOnClickListener(v ->
                startActivity(new Intent(this, SavedDecksActivity.class)));

        // Animate orbs
        animateOrb(findViewById(R.id.orbTop), 18f, 12f, 8000);
        animateOrb(findViewById(R.id.orbBottom), -14f, -10f, 10000);
    }

    private void animateOrb(View orb, float dx, float dy, long duration) {
        if (orb == null) return;
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, dx * 3, 0f, -dx * 2, 0f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, dy * 2, 0f, -dy * 3, 0f);
        PropertyValuesHolder pvhScale = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f, 0.95f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f, 1.1f, 1f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(orb, pvhX, pvhY, pvhScale, pvhScaleY);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }
}