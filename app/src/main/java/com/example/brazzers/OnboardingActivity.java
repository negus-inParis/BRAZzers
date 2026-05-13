package com.example.brazzers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS = "quizzers_prefs";
    private static final String KEY_ONBOARDING = "onboarding_complete";
    private static final String KEY_NICKNAME = "user_nickname";

    private FrameLayout slideContainer;
    private LinearLayout dotsContainer;
    private TextView btnNext;
    private EditText etNickname;

    private int currentSlide = 0;
    private final int TOTAL_SLIDES = 5;

    private final int[] slideImages = {
            R.drawable.onboard_jordi,
            R.drawable.onboard_johnny_book,
            R.drawable.onboard_gattouz,
            0,
            R.drawable.onboard_johnny_grad
    };

    private final String[] slideTitles = {
            "Wait... is this a\nSTUDY APP?! 😳",
            "Professor Sins\nwill see you now 📚",
            "Study anywhere.\nLook good doing it 📱",
            "Before we get\nstarted... 😏",
            "If HE graduated...\nso can you 🎓"
    };

    private final String[] slideDescs = {
            "Jordi just discovered Quizzers and his jaw DROPPED.\nThis isn't your boring flashcard app...\nit hits different 😏",
            "This man has mastered every profession.\nNow he's making sure YOU master every subject.\nStudying never looked this good fr fr 💀",
            "Quizzers lets you grind flashcards anywhere —\nthe couch, the bus, even at the party.\nMain character study arc activated 👑",
            "What should we call you?\nWho was in Paris?",
            "From plumber to doctor to astronaut...\nQuizzers helped him pass and it'll help YOU too.\nYour academic glow-up starts NOW ✨"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Immersive fullscreen — hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        slideContainer = findViewById(R.id.slideContainer);
        dotsContainer = findViewById(R.id.dotsContainer);
        btnNext = findViewById(R.id.btnNext);

        setupDots();
        showSlide(0, false);

        btnNext.setOnClickListener(v -> onNextClicked());
    }

    private void onNextClicked() {
        if (currentSlide == 3) {
            if (etNickname == null || etNickname.getText().toString().trim().isEmpty()) {
                if (etNickname != null) {
                    etNickname.animate().translationX(-10).setDuration(50)
                            .withEndAction(() -> etNickname.animate().translationX(10).setDuration(50)
                                    .withEndAction(() -> etNickname.animate().translationX(-6).setDuration(50)
                                            .withEndAction(() -> etNickname.animate().translationX(0).setDuration(50).start())
                                            .start()).start()).start();
                    etNickname.setHintTextColor(Color.parseColor("#FF5252"));
                    etNickname.setHint("Bro... we need a name! 😤");
                }
                return;
            }
        }

        if (currentSlide < TOTAL_SLIDES - 1) {
            currentSlide++;
            showSlide(currentSlide, true);
        } else {
            String nickname = "";
            if (etNickname != null) nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) nickname = "Bestie";

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_ONBOARDING, true)
                    .putString(KEY_NICKNAME, nickname)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    private void showSlide(int index, boolean animate) {
        slideContainer.removeAllViews();

        View slideView;
        if (index == 3) {
            slideView = buildNicknameSlide();
        } else {
            slideView = buildHeroSlide(index);
        }

        slideContainer.addView(slideView);

        if (animate) {
            slideView.setAlpha(0f);
            slideView.animate().alpha(1f).setDuration(400)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        updateDots(index);

        if (index == TOTAL_SLIDES - 1) {
            btnNext.setText("Let's Get It! 🚀");
        } else if (index == 3) {
            btnNext.setText("Continue →");
        } else {
            btnNext.setText("Next →");
        }
    }

    /**
     * Strava-style hero slide:
     * - Full-bleed image fills the ENTIRE screen
     * - Tall gradient overlay fades from transparent → black at bottom
     * - Title + desc sit in the gradient zone at the bottom
     */
    private View buildHeroSlide(int index) {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // === Full-bleed hero image ===
        ImageView img = new ImageView(this);
        img.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        img.setImageResource(slideImages[index]);

        // Per-slide image adjustments
        if (index == 4) {
            // Johnny Grad: top-align so graduation cap + diploma are visible
            img.setScaleType(ImageView.ScaleType.MATRIX);
            img.post(() -> {
                float vw = img.getWidth();
                float dw = img.getDrawable().getIntrinsicWidth();
                float scale = vw / dw;
                Matrix matrix = new Matrix();
                matrix.setScale(scale, scale);
                img.setImageMatrix(matrix);
            });
        } else {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        frame.addView(img);

        // === Gradient overlay: transparent top → black bottom (tall — 65% of screen) ===
        View gradient = new View(this);
        FrameLayout.LayoutParams gradParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        gradient.setLayoutParams(gradParams);
        GradientDrawable gradDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        0x00000000,  // transparent top
                        0x00000000,  // still transparent at ~35%
                        0x99000000,  // starts fading
                        0xE6000000,  // dark
                        0xFF000000   // pure black bottom
                }
        );
        gradient.setBackground(gradDrawable);
        frame.addView(gradient);

        // === Text content pinned to bottom ===
        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setGravity(Gravity.START);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.BOTTOM;
        textParams.bottomMargin = dp(120); // above the bottom bar
        textBox.setLayoutParams(textParams);
        textBox.setPadding(dp(28), 0, dp(28), 0);

        // Gold accent line
        View line = new View(this);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(40), dp(3));
        lineParams.bottomMargin = dp(14);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(Color.parseColor("#F5C518"));
        textBox.addView(line);

        // Title — big bold white
        TextView tvTitle = new TextView(this);
        tvTitle.setText(slideTitles[index]);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        tvTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvTitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(12);
        tvTitle.setLayoutParams(titleParams);
        textBox.addView(tvTitle);

        // Description — silver
        TextView tvDesc = new TextView(this);
        tvDesc.setText(slideDescs[index]);
        tvDesc.setTextColor(Color.parseColor("#B0B0B0"));
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvDesc.setLineSpacing(dp(2), 1f);
        textBox.addView(tvDesc);

        frame.addView(textBox);
        return frame;
    }

    /**
     * Nickname input slide — centered, no image, premium dark design
     */
    private View buildNicknameSlide() {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        frame.setBackgroundColor(Color.BLACK);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        layout.setPadding(dp(36), dp(0), dp(36), dp(140));

        // Big emoji
        TextView emoji = new TextView(this);
        emoji.setText("👋");
        emoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        emoji.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emojiParams.bottomMargin = dp(16);
        emojiParams.gravity = Gravity.CENTER_HORIZONTAL;
        emoji.setLayoutParams(emojiParams);
        layout.addView(emoji);

        // Gold accent line
        View line = new View(this);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(40), dp(3));
        lineParams.gravity = Gravity.CENTER_HORIZONTAL;
        lineParams.bottomMargin = dp(16);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(Color.parseColor("#F5C518"));
        layout.addView(line);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(slideTitles[3]);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tvTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(8);
        tvTitle.setLayoutParams(titleParams);
        layout.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText(slideDescs[3]);
        tvSub.setTextColor(Color.parseColor("#888888"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subParams.bottomMargin = dp(32);
        tvSub.setLayoutParams(subParams);
        layout.addView(tvSub);

        // EditText
        etNickname = new EditText(this);
        etNickname.setHint("Your name...");
        etNickname.setHintTextColor(Color.parseColor("#555555"));
        etNickname.setTextColor(Color.WHITE);
        etNickname.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        etNickname.setGravity(Gravity.CENTER);
        etNickname.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etNickname.setBackgroundResource(R.drawable.bg_edittext_premium);
        etNickname.setPadding(dp(24), dp(20), dp(24), dp(20));
        etNickname.setSingleLine(true);
        etNickname.setMaxLines(1);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.bottomMargin = dp(12);
        etNickname.setLayoutParams(etParams);
        layout.addView(etNickname);

        // Required note
        TextView tvNote = new TextView(this);
        tvNote.setText("* This is required");
        tvNote.setTextColor(Color.parseColor("#444444"));
        tvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNote.setGravity(Gravity.CENTER);
        layout.addView(tvNote);

        frame.addView(layout);
        return frame;
    }

    private void setupDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TOTAL_SLIDES; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(8), dp(8));
            params.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot_accent);
            dot.setAlpha(0.3f);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int active) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            if (i == active) {
                dot.setAlpha(1f);
                dot.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).start();
            } else {
                dot.setAlpha(0.3f);
                dot.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
            }
        }
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }
}
