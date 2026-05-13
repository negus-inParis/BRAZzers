package com.example.brazzers;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.brazzers.data.AppDatabase;
import com.example.brazzers.data.DeckWithCards;
import com.example.brazzers.data.FlashcardCardEntity;
import com.example.brazzers.data.FlashcardDao;
import com.example.brazzers.data.FlashcardDeckEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardCounter, tvTerm, tvDefinition, tvFlipHint, tvScore;
    private CardView cardFront, cardBack;
    private View cardContainer;
    private Button btnNext, btnPrev, btnSave;
    private TextView btnBack;
    private LinearLayout mcContainer;
    private TextView btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD;

    private List<FlashcardCardEntity> cards;
    private ArrayList<String> rawFlashcards;
    private int currentIndex = 0;
    private boolean isFront = true;
    private boolean isAnimating = false;
    private boolean deckSaved = false;

    // MC state
    private boolean isMCMode = false;
    private boolean answerSelected = false;
    private int score = 0;
    private String[] currentChoices;
    private String currentCorrectAnswer;

    private FlashcardDao dao;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        // Immersive fullscreen — hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        dao = AppDatabase.getInstance(this).flashcardDao();

        tvCardCounter = findViewById(R.id.tvCardCounter);
        tvTerm = findViewById(R.id.tvTerm);
        tvDefinition = findViewById(R.id.tvDefinition);
        tvFlipHint = findViewById(R.id.tvFlipHint);
        tvScore = findViewById(R.id.tvScore);
        cardFront = findViewById(R.id.cardFront);
        cardBack = findViewById(R.id.cardBack);
        cardContainer = findViewById(R.id.cardContainer);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        mcContainer = findViewById(R.id.mcContainer);
        btnChoiceA = findViewById(R.id.btnChoiceA);
        btnChoiceB = findViewById(R.id.btnChoiceB);
        btnChoiceC = findViewById(R.id.btnChoiceC);
        btnChoiceD = findViewById(R.id.btnChoiceD);

        float scale = getResources().getDisplayMetrics().density;
        cardFront.setCameraDistance(12000 * scale);
        cardBack.setCameraDistance(12000 * scale);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Animate orbs
        animateOrb(findViewById(R.id.orbFlash1), 15f, 10f, 9000);
        animateOrb(findViewById(R.id.orbFlash2), -12f, -8f, 11000);

        // Determine mode
        String quizMode = getIntent().getStringExtra("quiz_mode");
        boolean fromGeneration = getIntent().getBooleanExtra("from_generation", false);
        long deckId = getIntent().getLongExtra("deck_id", -1);

        if (fromGeneration) {
            rawFlashcards = getIntent().getStringArrayListExtra("flashcards");
            if (rawFlashcards == null || rawFlashcards.isEmpty()) {
                showEmpty();
                return;
            }
            isMCMode = "multiple_choice".equals(quizMode) ||
                    (rawFlashcards.size() > 0 && rawFlashcards.get(0).contains("|||"));
            cards = convertRawToCards(rawFlashcards);
            btnSave.setVisibility(View.VISIBLE);
            btnSave.setOnClickListener(v -> promptSaveDeck());
            setupCardUI();
        } else if (deckId != -1) {
            deckSaved = true;
            btnSave.setVisibility(View.GONE);
            loadDeckFromRoom(deckId);
        } else {
            showEmpty();
        }
    }

    // === ORB ANIMATION (same as MainActivity) ===
    private void animateOrb(View orb, float dx, float dy, long duration) {
        if (orb == null) return;
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, dx * 3, 0f, -dx * 2, 0f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, dy * 2, 0f, -dy * 3, 0f);
        PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f, 0.95f, 1f);
        PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f, 1.1f, 1f);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(orb, pvhX, pvhY, pvhSX, pvhSY);
        anim.setDuration(duration);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

    private void loadDeckFromRoom(long deckId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            DeckWithCards deckWithCards = dao.getDeckWithCards(deckId);
            runOnUiThread(() -> {
                if (deckWithCards == null || deckWithCards.cards == null || deckWithCards.cards.isEmpty()) {
                    showEmpty();
                    return;
                }
                deckWithCards.cards.sort((a, b) -> Integer.compare(a.position, b.position));
                cards = deckWithCards.cards;
                isMCMode = cards.get(0).backText != null && cards.get(0).backText.contains("|||");
                setupCardUI();
            });
        });
    }

    private List<FlashcardCardEntity> convertRawToCards(ArrayList<String> raw) {
        List<FlashcardCardEntity> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            FlashcardCardEntity card = new FlashcardCardEntity();
            String line = raw.get(i);
            if (isMCMode && line.contains("|||")) {
                String[] parts = line.split("\\|\\|\\|");
                card.frontText = parts[0].trim();
                StringBuilder backBuilder = new StringBuilder();
                for (int j = 1; j < parts.length; j++) {
                    if (j > 1) backBuilder.append("|||");
                    backBuilder.append(parts[j].trim());
                }
                card.backText = backBuilder.toString();
            } else if (line.contains(": ")) {
                String[] parts = line.split(": ", 2);
                card.frontText = parts[0].trim();
                card.backText = parts[1].trim();
            } else if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                card.frontText = parts[0].trim();
                card.backText = parts[1].trim();
            } else {
                card.frontText = line.trim();
                card.backText = "";
            }
            card.position = i;
            result.add(card);
        }
        return result;
    }

    private void setupCardUI() {
        if (cards == null || cards.isEmpty()) {
            showEmpty();
            return;
        }

        if (isMCMode) {
            mcContainer.setVisibility(View.VISIBLE);
            tvFlipHint.setText("Select the correct answer");
            tvScore.setVisibility(View.VISIBLE);
            tvScore.setText("Score: 0 / " + cards.size());
            cardContainer.setOnClickListener(null);
            cardBack.setVisibility(View.GONE);
            TextView tvFrontLabel = findViewById(R.id.tvFrontLabel);
            if (tvFrontLabel != null) tvFrontLabel.setText("QUESTION");
        } else {
            mcContainer.setVisibility(View.GONE);
            tvScore.setVisibility(View.GONE);
            cardContainer.setOnClickListener(v -> flipCard());
            TextView tvFrontLabel = findViewById(R.id.tvFrontLabel);
            if (tvFrontLabel != null) tvFrontLabel.setText("TERM");
        }

        resetToFront();
        showCurrentCard(false);

        btnNext.setOnClickListener(v -> {
            if (isMCMode && !answerSelected) {
                CustomToast.show(this, "Select an answer first!", CustomToast.INFO);
                return;
            }
            if (currentIndex < cards.size() - 1) {
                currentIndex++;
                answerSelected = false;
                resetToFront();
                showCurrentCard(true);
            } else {
                onFinish();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                answerSelected = false;
                resetToFront();
                showCurrentCard(true);
            }
        });
    }

    private void resetToFront() {
        cardFront.clearAnimation();
        cardBack.clearAnimation();
        cardFront.setRotationY(0f);
        cardFront.setAlpha(1f);
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setRotationY(0f);
        cardBack.setAlpha(0f);
        cardBack.setVisibility(isMCMode ? View.GONE : View.INVISIBLE);
        isFront = true;
        isAnimating = false;
    }

    private void showEmpty() {
        tvTerm.setText("");
        tvDefinition.setText(getString(R.string.no_saved_flashcards));
        tvCardCounter.setText("");
        btnNext.setEnabled(false);
        btnPrev.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
    }

    private void showCurrentCard(boolean animate) {
        if (cards == null || cards.isEmpty()) return;

        FlashcardCardEntity card = cards.get(currentIndex);

        tvCardCounter.setText(String.format(getString(R.string.card_counter), currentIndex + 1, cards.size()));

        // Hide Previous on card 1 — Next button will center/fill
        if (currentIndex > 0) {
            btnPrev.setVisibility(View.VISIBLE);
            // Restore 50/50 split
            LinearLayout.LayoutParams nextParams = (LinearLayout.LayoutParams) btnNext.getLayoutParams();
            nextParams.weight = 1;
            nextParams.setMarginStart(dp(8));
            btnNext.setLayoutParams(nextParams);
        } else {
            btnPrev.setVisibility(View.GONE);
            // Next takes full width, centered
            LinearLayout.LayoutParams nextParams = (LinearLayout.LayoutParams) btnNext.getLayoutParams();
            nextParams.weight = 1;
            nextParams.setMarginStart(0);
            btnNext.setLayoutParams(nextParams);
        }

        if (currentIndex >= cards.size() - 1) {
            btnNext.setText("🎉 Finish!");
        } else {
            btnNext.setText("Next →");
        }
        btnNext.setEnabled(true);

        if (isMCMode) {
            if (animate) {
                animateTextReveal(tvTerm, card.frontText);
            } else {
                tvTerm.setText(card.frontText);
            }
            setupMCChoices(card);
        } else {
            if (animate) {
                animateTextReveal(tvTerm, card.frontText);
            } else {
                tvTerm.setText(card.frontText);
            }
            tvDefinition.setText(card.backText);
        }
    }

    // === MULTIPLE CHOICE LOGIC ===
    private void setupMCChoices(FlashcardCardEntity card) {
        String backText = card.backText;
        if (backText == null || !backText.contains("|||")) return;

        String[] parts = backText.split("\\|\\|\\|");
        currentCorrectAnswer = parts[0].trim();

        List<String> choiceList = new ArrayList<>();
        for (int i = 1; i < parts.length && choiceList.size() < 4; i++) {
            choiceList.add(parts[i].trim());
        }

        boolean hasCorrect = false;
        for (String c : choiceList) {
            if (c.equalsIgnoreCase(currentCorrectAnswer)) {
                hasCorrect = true;
                break;
            }
        }
        if (!hasCorrect && choiceList.size() >= 4) {
            choiceList.set(new Random().nextInt(4), currentCorrectAnswer);
        } else if (!hasCorrect) {
            choiceList.add(currentCorrectAnswer);
        }

        Collections.shuffle(choiceList);

        String[] labels = {"A", "B", "C", "D"};
        TextView[] buttons = {btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD};
        currentChoices = new String[4];

        for (int i = 0; i < 4 && i < choiceList.size(); i++) {
            currentChoices[i] = choiceList.get(i);
            buttons[i].setText(labels[i] + ".  " + choiceList.get(i));
            buttons[i].setBackgroundResource(R.drawable.bg_choice_default);
            buttons[i].setTextColor(getResources().getColor(R.color.text_primary));
            buttons[i].setElevation(0);
            buttons[i].setScaleX(1f);
            buttons[i].setScaleY(1f);
            buttons[i].setClickable(true);
            buttons[i].setVisibility(View.VISIBLE);
            final int idx = i;
            buttons[i].setOnClickListener(v -> onChoiceSelected(idx));
        }

        for (int i = choiceList.size(); i < 4; i++) {
            buttons[i].setVisibility(View.GONE);
        }

        answerSelected = false;
    }

    private void onChoiceSelected(int selectedIndex) {
        if (answerSelected) return;
        answerSelected = true;

        TextView[] buttons = {btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD};
        String selected = currentChoices[selectedIndex];
        boolean isCorrect = selected.equalsIgnoreCase(currentCorrectAnswer);

        if (isCorrect) {
            score++;
            animateCorrectChoice(buttons[selectedIndex]);
        } else {
            animateWrongChoice(buttons[selectedIndex]);
            // Find and glow the correct answer
            for (int i = 0; i < currentChoices.length; i++) {
                if (currentChoices[i] != null && currentChoices[i].equalsIgnoreCase(currentCorrectAnswer)) {
                    animateCorrectChoice(buttons[i]);
                    break;
                }
            }
            // Vibrate on wrong answer
            vibrateDevice();
        }

        // Disable all buttons after selection
        for (TextView btn : buttons) {
            btn.setClickable(false);
        }

        tvScore.setText("Score: " + score + " / " + cards.size());
    }

    // === GLOW ANIMATIONS ===
    private void animateCorrectChoice(TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_choice_correct);
        btn.setTextColor(Color.parseColor("#F5C518"));

        // Glow pulse: scale up slightly + elevate
        btn.animate()
                .scaleX(1.03f).scaleY(1.03f)
                .setDuration(200)
                .withEndAction(() ->
                        btn.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                ).start();

        // Elevation glow
        ObjectAnimator glow = ObjectAnimator.ofFloat(btn, "elevation", 0f, 20f, 10f);
        glow.setDuration(500);
        glow.start();
    }

    private void animateWrongChoice(TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_choice_wrong);
        btn.setTextColor(Color.parseColor("#FF5252"));

        // Shake animation
        ObjectAnimator shake = ObjectAnimator.ofFloat(btn, "translationX",
                0, -12, 12, -10, 10, -6, 6, -3, 3, 0);
        shake.setDuration(500);
        shake.start();

        // Elevation glow
        ObjectAnimator glow = ObjectAnimator.ofFloat(btn, "elevation", 0f, 16f, 8f);
        glow.setDuration(400);
        glow.start();
    }

    private void vibrateDevice() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {}
    }

    // === FLIP ANIMATION (identification mode only) ===
    private void flipCard() {
        if (isAnimating || cards == null || cards.isEmpty()) return;
        isAnimating = true;

        if (isFront) {
            cardFront.animate().rotationY(90f).setDuration(150)
                    .withEndAction(() -> {
                        cardFront.setVisibility(View.INVISIBLE);
                        cardBack.setRotationY(-90f);
                        cardBack.setAlpha(1f);
                        cardBack.setVisibility(View.VISIBLE);
                        cardBack.animate().rotationY(0f).setDuration(150)
                                .withEndAction(() -> isAnimating = false).start();
                    }).start();
            isFront = false;
        } else {
            cardBack.animate().rotationY(90f).setDuration(150)
                    .withEndAction(() -> {
                        cardBack.setVisibility(View.INVISIBLE);
                        cardFront.setRotationY(-90f);
                        cardFront.setAlpha(1f);
                        cardFront.setVisibility(View.VISIBLE);
                        cardFront.animate().rotationY(0f).setDuration(150)
                                .withEndAction(() -> isAnimating = false).start();
                    }).start();
            isFront = true;
        }
    }

    // === TEXT SCRAMBLE/REVEAL ===
    private void animateTextReveal(TextView tv, String finalText) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        int displayLen = Math.min(finalText.length(), 30);

        tv.animate().alpha(0.2f).setDuration(100).withEndAction(() -> {
            for (int i = 0; i < 4; i++) {
                final int iter = i;
                handler.postDelayed(() -> {
                    if (iter < 3) {
                        StringBuilder scrambled = new StringBuilder();
                        for (int j = 0; j < displayLen; j++) {
                            char c = finalText.charAt(j);
                            if (c == ' ') scrambled.append(' ');
                            else scrambled.append(chars.charAt(random.nextInt(chars.length())));
                        }
                        tv.setText(scrambled.toString());
                        tv.setAlpha(0.4f + (iter * 0.15f));
                    } else {
                        tv.setText(finalText);
                        tv.animate().alpha(1f).setDuration(150).start();
                    }
                }, i * 70L);
            }
        }).start();
    }

    // === FINISH ===
    private void onFinish() {
        if (!deckSaved && btnSave.getVisibility() == View.VISIBLE) {
            String defaultTitle = (isMCMode ? "Quiz " : "Flashcards ") +
                    new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
            saveDeckToRoom(defaultTitle, true);
        } else {
            showConfettiAndExit();
        }
    }

    private void showConfettiAndExit() {
        FrameLayout confettiOverlay = findViewById(R.id.confettiOverlay);
        if (confettiOverlay == null) { finish(); return; }
        confettiOverlay.setVisibility(View.VISIBLE);

        Random random = new Random();
        int[] colors = {
                Color.parseColor("#F5C518"), Color.parseColor("#00D9A6"),
                Color.parseColor("#FF6B6B"), Color.parseColor("#FFD93D"),
                Color.parseColor("#6BCB77"), Color.parseColor("#FF9F43"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#45AAF2")
        };

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        for (int i = 0; i < 60; i++) {
            View confetti = new View(this);
            int size = 8 + random.nextInt(12);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size * 2);
            params.leftMargin = random.nextInt(screenWidth);
            params.topMargin = -20;
            confetti.setLayoutParams(params);
            confetti.setBackgroundColor(colors[random.nextInt(colors.length)]);
            confetti.setRotation(random.nextFloat() * 360);
            confettiOverlay.addView(confetti);

            confetti.animate()
                    .translationY(screenHeight + 100)
                    .rotation(confetti.getRotation() + 360 + random.nextInt(720))
                    .translationX(-50 + random.nextInt(100))
                    .setStartDelay(random.nextInt(300))
                    .setDuration(1200 + random.nextInt(800))
                    .setInterpolator(new AccelerateInterpolator(0.5f))
                    .start();
        }

        TextView emoji = new TextView(this);
        if (isMCMode) {
            emoji.setText(score + " / " + cards.size() + " 🎉");
            emoji.setTextSize(36);
        } else {
            emoji.setText("🎉");
            emoji.setTextSize(64);
        }
        emoji.setTextColor(Color.WHITE);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ep.gravity = Gravity.CENTER;
        emoji.setLayoutParams(ep);
        emoji.setAlpha(0f);
        emoji.setScaleX(0.3f);
        emoji.setScaleY(0.3f);
        confettiOverlay.addView(emoji);
        emoji.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400)
                .withEndAction(() -> emoji.animate().alpha(0f).setStartDelay(600).setDuration(300).start())
                .start();

        handler.postDelayed(this::finish, 1500);
    }

    // === SAVE DIALOG (Themed) ===
    private void promptSaveDeck() {
        String modeLabel = isMCMode ? "Quiz" : "Flashcards";
        String defaultTitle = modeLabel + " " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
        String timestamp = new SimpleDateFormat("MMMM dd, yyyy — hh:mm a", Locale.getDefault()).format(new Date());

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Root layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1A1A"));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.parseColor("#2A2A2A"));
        root.setBackground(bg);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("💾  Save Deck");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dp(16));
        root.addView(tvTitle);

        // Gold line
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
        line.setBackgroundColor(Color.parseColor("#F5C518"));
        LinearLayout.LayoutParams lineParams = (LinearLayout.LayoutParams) line.getLayoutParams();
        lineParams.bottomMargin = dp(20);
        root.addView(line);

        // EditText
        EditText input = new EditText(this);
        input.setText(defaultTitle);
        input.setSelectAllOnFocus(true);
        input.setTextColor(Color.parseColor("#F0F0F0"));
        input.setHintTextColor(Color.parseColor("#555555"));
        input.setHint("Deck name");
        input.setTextSize(16);
        input.setBackgroundResource(R.drawable.bg_edittext_premium);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.bottomMargin = dp(12);
        input.setLayoutParams(inputParams);
        root.addView(input);

        // Timestamp
        TextView tvTimestamp = new TextView(this);
        tvTimestamp.setText("📅 " + timestamp);
        tvTimestamp.setTextColor(Color.parseColor("#666666"));
        tvTimestamp.setTextSize(13);
        tvTimestamp.setPadding(dp(4), 0, 0, dp(4));
        root.addView(tvTimestamp);

        if (isMCMode) {
            TextView tvMode = new TextView(this);
            tvMode.setText("🔘 Multiple Choice · " + cards.size() + " questions");
            tvMode.setTextColor(Color.parseColor("#F5C518"));
            tvMode.setTextSize(13);
            tvMode.setPadding(dp(4), dp(4), 0, 0);
            root.addView(tvMode);
        }

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = dp(24);
        btnRow.setLayoutParams(btnRowParams);

        // Cancel button
        TextView btnCancel = new TextView(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(20), dp(12), dp(20), dp(12));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        // Save button
        TextView btnSaveDialog = new TextView(this);
        btnSaveDialog.setText("Save");
        btnSaveDialog.setTextColor(Color.parseColor("#000000"));
        btnSaveDialog.setTextSize(15);
        btnSaveDialog.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSaveDialog.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(Color.parseColor("#F5C518"));
        saveBg.setCornerRadius(dp(12));
        btnSaveDialog.setBackground(saveBg);
        btnSaveDialog.setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) title = defaultTitle;
            saveDeckToRoom(title, false);
            dialog.dismiss();
        });
        btnRow.addView(btnSaveDialog);

        root.addView(btnRow);
        dialog.setContentView(root);

        // Style the dialog window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private int dp(int val) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }

    private void saveDeckToRoom(String title, boolean finishAfter) {
        if (cards == null || cards.isEmpty()) return;

        long now = System.currentTimeMillis();

        FlashcardDeckEntity deck = new FlashcardDeckEntity();
        deck.title = title;
        deck.createdAt = now;
        deck.updatedAt = now;
        deck.cardCount = cards.size();
        if (!cards.isEmpty()) {
            deck.previewText = cards.get(0).frontText;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            long deckId = dao.insertDeck(deck);

            List<FlashcardCardEntity> toInsert = new ArrayList<>();
            for (int i = 0; i < cards.size(); i++) {
                FlashcardCardEntity c = new FlashcardCardEntity();
                c.deckId = deckId;
                c.position = i;
                c.frontText = cards.get(i).frontText;
                c.backText = cards.get(i).backText;
                toInsert.add(c);
            }
            dao.insertCards(toInsert);

            runOnUiThread(() -> {
                deckSaved = true;
                CustomToast.show(this, "Flashcards saved!", CustomToast.SUCCESS);
                btnSave.setVisibility(View.GONE);

                if (finishAfter) {
                    showConfettiAndExit();
                }
            });
        });
    }
}