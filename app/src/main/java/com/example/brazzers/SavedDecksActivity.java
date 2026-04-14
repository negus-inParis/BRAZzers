package com.example.brazzers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private TextView tvEmpty;
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
        new AlertDialog.Builder(this)
                .setTitle("Delete Deck")
                .setMessage("Delete \"" + deck.title + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        dao.deleteDeck(deck.deckId);
                        runOnUiThread(this::loadDecks);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeckRename(FlashcardDeckEntity deck) {
        EditText input = new EditText(this);
        input.setText(deck.title);
        input.setSelectAllOnFocus(true);
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Rename Deck")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            dao.renameDeck(deck.deckId, newTitle, System.currentTimeMillis());
                            runOnUiThread(this::loadDecks);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                        String line = oldCards.get(i);
                        if (line.contains(":")) {
                            String[] parts = line.split(":", 2);
                            card.frontText = parts[0].trim();
                            card.backText = parts[1].trim();
                        } else {
                            card.frontText = line.trim();
                            card.backText = "";
                        }
                        cards.add(card);
                    }
                    dao.insertCards(cards);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Imported " + oldCards.size() + " flashcards from previous version", Toast.LENGTH_SHORT).show();
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
