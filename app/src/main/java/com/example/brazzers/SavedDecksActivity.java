package com.example.brazzers;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brazzers.data.AppDatabase;
import com.example.brazzers.data.FlashcardCardEntity;
import com.example.brazzers.data.FlashcardDao;
import com.example.brazzers.data.FlashcardDeckEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SavedDecksActivity extends AppCompatActivity implements DeckAdapter.OnDeckActionListener {

    private RecyclerView rvDecks;
    private View tvEmpty;
    private DeckAdapter adapter;
    private FlashcardDao dao;

    // Old SharedPreferences constants for migration
    private static final String OLD_PREF_NAME = "QuizzersPrefs";
    private static final String OLD_KEY_FLASHCARDS = "saved_flashcards";
    private static final String MIGRATION_DONE_KEY = "migration_v2_done";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_decks);

        // Immersive fullscreen — hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        dao = AppDatabase.getInstance(this).flashcardDao();

        rvDecks = findViewById(R.id.rvDecks);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvDecks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeckAdapter(new ArrayList<>(), this);
        rvDecks.setAdapter(adapter);

        // Migrate old data on first launch
        migrateOldDataIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDecks();
    }

    private void loadDecks() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FlashcardDeckEntity> decks = dao.getAllDecks();
            runOnUiThread(() -> {
                adapter.updateDecks(decks);
                tvEmpty.setVisibility(decks.isEmpty() ? View.VISIBLE : View.GONE);
                rvDecks.setVisibility(decks.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    public void onDeckClick(FlashcardDeckEntity deck) {
        Intent intent = new Intent(this, FlashcardsActivity.class);
        intent.putExtra("deck_id", deck.deckId);
        startActivity(intent);
    }

    @Override
    public void onDeckDelete(FlashcardDeckEntity deck) {
        showThemedDialog(
                "🗑️  Delete Deck",
                "Delete \"" + deck.title + "\"?\nThis cannot be undone.",
                "Delete",
                "#FF5252",
                () -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        dao.deleteDeck(deck.deckId);
                        runOnUiThread(this::loadDecks);
                    });
                }
        );
    }

    @Override
    public void onDeckRename(FlashcardDeckEntity deck) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

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
        tvTitle.setText("✏️  Rename Deck");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dp(16));
        root.addView(tvTitle);

        // Gold accent
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
        line.setBackgroundColor(Color.parseColor("#F5C518"));
        ((LinearLayout.LayoutParams) line.getLayoutParams()).bottomMargin = dp(20);
        root.addView(line);

        // EditText
        EditText input = new EditText(this);
        input.setText(deck.title);
        input.setSelectAllOnFocus(true);
        input.setTextColor(Color.parseColor("#F0F0F0"));
        input.setHintTextColor(Color.parseColor("#555555"));
        input.setHint("Deck name");
        input.setTextSize(16);
        input.setBackgroundResource(R.drawable.bg_edittext_premium);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.bottomMargin = dp(8);
        input.setLayoutParams(inputParams);
        root.addView(input);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = dp(24);
        btnRow.setLayoutParams(btnRowParams);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(20), dp(12), dp(20), dp(12));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnRename = new TextView(this);
        btnRename.setText("Rename");
        btnRename.setTextColor(Color.parseColor("#000000"));
        btnRename.setTextSize(15);
        btnRename.setTypeface(null, Typeface.BOLD);
        btnRename.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable renameBg = new GradientDrawable();
        renameBg.setColor(Color.parseColor("#F5C518"));
        renameBg.setCornerRadius(dp(12));
        btnRename.setBackground(renameBg);
        btnRename.setOnClickListener(v -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    dao.renameDeck(deck.deckId, newTitle, System.currentTimeMillis());
                    runOnUiThread(this::loadDecks);
                });
            }
            dialog.dismiss();
        });
        btnRow.addView(btnRename);

        root.addView(btnRow);
        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    /**
     * Generic themed confirmation dialog (for delete, etc.)
     */
    private void showThemedDialog(String title, String message, String actionText, String actionColor, Runnable onConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1A1A"));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.parseColor("#2A2A2A"));
        root.setBackground(bg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dp(12));
        root.addView(tvTitle);

        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
        line.setBackgroundColor(Color.parseColor(actionColor));
        ((LinearLayout.LayoutParams) line.getLayoutParams()).bottomMargin = dp(16);
        root.addView(line);

        TextView tvMessage = new TextView(this);
        tvMessage.setText(message);
        tvMessage.setTextColor(Color.parseColor("#B0B0B0"));
        tvMessage.setTextSize(15);
        tvMessage.setLineSpacing(dp(2), 1f);
        root.addView(tvMessage);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = dp(24);
        btnRow.setLayoutParams(btnRowParams);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(20), dp(12), dp(20), dp(12));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnAction = new TextView(this);
        btnAction.setText(actionText);
        btnAction.setTextColor(Color.WHITE);
        btnAction.setTextSize(15);
        btnAction.setTypeface(null, Typeface.BOLD);
        btnAction.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable actionBg = new GradientDrawable();
        actionBg.setColor(Color.parseColor(actionColor));
        actionBg.setCornerRadius(dp(12));
        btnAction.setBackground(actionBg);
        btnAction.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });
        btnRow.addView(btnAction);

        root.addView(btnRow);
        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }

    /**
     * Migrate old SharedPreferences flashcards to Room.
     */
    private void migrateOldDataIfNeeded() {
        SharedPreferences migrationPrefs = getSharedPreferences(OLD_PREF_NAME, Context.MODE_PRIVATE);
        boolean alreadyMigrated = migrationPrefs.getBoolean(MIGRATION_DONE_KEY, false);

        if (alreadyMigrated) return;

        String json = migrationPrefs.getString(OLD_KEY_FLASHCARDS, null);
        if (json == null || json.isEmpty()) {
            migrationPrefs.edit().putBoolean(MIGRATION_DONE_KEY, true).apply();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<String>>() {}.getType();
                ArrayList<String> oldCards = gson.fromJson(json, type);

                if (oldCards != null && !oldCards.isEmpty()) {
                    long now = System.currentTimeMillis();

                    FlashcardDeckEntity deck = new FlashcardDeckEntity();
                    deck.title = "Imported Flashcards";
                    deck.createdAt = now;
                    deck.updatedAt = now;
                    deck.cardCount = oldCards.size();
                    if (!oldCards.isEmpty()) {
                        String first = oldCards.get(0);
                        deck.previewText = first.contains(":") ? first.split(":", 2)[0].trim() : first;
                    }

                    long deckId = dao.insertDeck(deck);

                    List<FlashcardCardEntity> cards = new ArrayList<>();
                    for (int i = 0; i < oldCards.size(); i++) {
                        FlashcardCardEntity card = new FlashcardCardEntity();
                        card.deckId = deckId;
                        card.position = i;
                        String cardLine = oldCards.get(i);
                        if (cardLine.contains(":")) {
                            String[] parts = cardLine.split(":", 2);
                            card.frontText = parts[0].trim();
                            card.backText = parts[1].trim();
                        } else {
                            card.frontText = cardLine.trim();
                            card.backText = "";
                        }
                        cards.add(card);
                    }
                    dao.insertCards(cards);

                    runOnUiThread(() -> {
                        CustomToast.show(this, "Imported " + oldCards.size() + " flashcards from previous version", CustomToast.SUCCESS);
                        loadDecks();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Mark migration done
            migrationPrefs.edit().putBoolean(MIGRATION_DONE_KEY, true).apply();
        });
    }
}
