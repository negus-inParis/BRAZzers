package com.example.brazzers;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_splash);

        View splashOrb = findViewById(R.id.splashOrb);
        TextView splashQUI = findViewById(R.id.splashQUI);
        TextView splashZZ = findViewById(R.id.splashZZ);
        TextView splashERS = findViewById(R.id.splashERS);
        TextView splashTagline = findViewById(R.id.splashTagline);

        // QUI slides from left
        ObjectAnimator quiFade = ObjectAnimator.ofFloat(splashQUI, "alpha", 0f, 1f);
        ObjectAnimator quiSlide = ObjectAnimator.ofFloat(splashQUI, "translationX", -40f, 0f);
        quiFade.setDuration(400); quiSlide.setDuration(400);
        quiFade.setStartDelay(200); quiSlide.setStartDelay(200);
        quiFade.setInterpolator(new DecelerateInterpolator());
        quiSlide.setInterpolator(new DecelerateInterpolator());

        // ZZ pops with scale
        ObjectAnimator zzFade = ObjectAnimator.ofFloat(splashZZ, "alpha", 0f, 1f);
        ObjectAnimator zzScaleX = ObjectAnimator.ofFloat(splashZZ, "scaleX", 0.6f, 1f);
        ObjectAnimator zzScaleY = ObjectAnimator.ofFloat(splashZZ, "scaleY", 0.6f, 1f);
        zzFade.setDuration(450); zzScaleX.setDuration(450); zzScaleY.setDuration(450);
        zzFade.setStartDelay(400); zzScaleX.setStartDelay(400); zzScaleY.setStartDelay(400);
        zzFade.setInterpolator(new DecelerateInterpolator());

        // ERS slides from right
        ObjectAnimator ersFade = ObjectAnimator.ofFloat(splashERS, "alpha", 0f, 1f);
        ObjectAnimator ersSlide = ObjectAnimator.ofFloat(splashERS, "translationX", 40f, 0f);
        ersFade.setDuration(400); ersSlide.setDuration(400);
        ersFade.setStartDelay(600); ersSlide.setStartDelay(600);
        ersFade.setInterpolator(new DecelerateInterpolator());
        ersSlide.setInterpolator(new DecelerateInterpolator());

        // Orb glow
        ObjectAnimator orbGlow = ObjectAnimator.ofFloat(splashOrb, "alpha", 0f, 0.25f, 0f);
        orbGlow.setDuration(1200); orbGlow.setStartDelay(500);
        orbGlow.setInterpolator(new AccelerateDecelerateInterpolator());

        // Tagline
        ObjectAnimator tagFade = ObjectAnimator.ofFloat(splashTagline, "alpha", 0f, 1f);
        tagFade.setDuration(500); tagFade.setStartDelay(900);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(quiFade, quiSlide, zzFade, zzScaleX, zzScaleY,
                ersFade, ersSlide, orbGlow, tagFade);
        set.start();

        // Route: Onboarding or Main Menu
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("quizzers_prefs", MODE_PRIVATE);
            boolean onboardingDone = prefs.getBoolean("onboarding_complete", false);

            Intent target;
            if (onboardingDone) {
                target = new Intent(this, MainActivity.class);
            } else {
                target = new Intent(this, OnboardingActivity.class);
            }
            startActivity(target);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2200);
    }
}
