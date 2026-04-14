package com.example.brazzers;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.brazzers.data.AppDatabase;
import com.example.brazzers.data.DeckWithCards;
import com.example.brazzers.data.FlashcardCardEntity;
import com.example.brazzers.data.FlashcardDao;
import com.example.brazzers.data.FlashcardDeckEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardCounter;
    private TextView tvTerm, tvDefinition;
    private CardView cardFront, cardBack;
    private View cardContainer;
    private Button btnNext, btnPrev, btnSave;

    private List<FlashcardCardEntity> cards;
    private ArrayList<String> rawFlashcards; // from generation intent
    private int currentIndex = 0;
    private boolean isFront = true;

    private FlashcardDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        dao = AppDatabase.getInstance(this).flashcardDao();

        tvCardCounter = findViewById(R.id.tvCardCounter);
        tvTerm = findViewById(R.id.tvTerm);
        tvDefinition = findViewById(R.id.tvDefinition);
        cardFront = findViewById(R.id.cardFront);
        cardBack = findViewById(R.id.cardBack);
        cardContainer = findViewById(R.id.cardContainer);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnSave = findViewById(R.id.btnSave);

        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        cardFront.setCameraDistance(8000 * scale);
        cardBack.setCameraDistance(8000 * scale);

        boolean fromGeneration = getIntent().getBooleanExtra("from_generation", false);
        long deckId = getIntent().getLongExtra("deck_id", -1);

        if (fromGeneration) {
            // MODE 1: Generated flashcards from AI
            rawFlashcards = getIntent().getStringArrayListExtra("flashcards");
            if (rawFlashcards == null || rawFlashcards.isEmpty()) {
                showEmpty();
                return;
            }
            cards = convertRawToCards(rawFlashcards);
            btnSave.setVisibility(View.VISIBLE);
            btnSave.setOnClickListener(v -> promptSaveDeck());
            setupCardUI();
        } else if (deckId != -1) {
            // MODE 2: Load existing deck from Room
            btnSave.setVisibility(View.GONE);
            loadDeckFromRoom(deckId);
        } else {
            showEmpty();
        }
    }

    private void loadDeckFromRoom(long deckId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            DeckWithCards deckWithCards = dao.getDeckWithCards(deckId);
            runOnUiThread(() -> {
                if (deckWithCards == null || deckWithCards.cards == null || deckWithCards.cards.isEmpty()) {
                    showEmpty();
                    return;
                }
                // Sort by position
                deckWithCards.cards.sort((a, b) -> Integer.compare(a.position, b.position));
                cards = deckWithCards.cards;
                setupCardUI();
            });
        });
    }

    private List<FlashcardCardEntity> convertRawToCards(ArrayList<String> raw) {
        List<FlashcardCardEntity> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            FlashcardCardEntity card = new FlashcardCardEntity();
            String line = raw.get(i);
            if (line.contains(":")) {
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
        showCurrentCard(false);

        cardContainer.setOnClickListener(v -> flipCard());

        btnNext.setOnClickListener(v -> {
            if (currentIndex < cards.size() - 1) {
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

    private void showEmpty() {
        tvTerm.setText("");
        tvDefinition.setText(getString(R.string.no_saved_flashcards));
        tvCardCounter.setText("");
        btnNext.setEnabled(false);
        btnPrev.setEnabled(false);
        btnSave.setVisibility(View.GONE);
    }

    private void showCurrentCard(boolean resetToFront) {
        if (cards == null || cards.isEmpty()) return;

        FlashcardCardEntity card = cards.get(currentIndex);
        tvTerm.setText(card.frontText);
        tvDefinition.setText(card.backText);

        tvCardCounter.setText(String.format(getString(R.string.card_counter), currentIndex + 1, cards.size()));

        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < cards.size() - 1);

        if (resetToFront && !isFront) {
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
            cardFront.animate().rotationY(180f).setDuration(300).withEndAction(() ->
                    cardFront.setVisibility(View.GONE)
            ).start();

            cardBack.setVisibility(View.VISIBLE);
            cardBack.setRotationY(-180f);
            cardBack.animate().rotationY(0f).setDuration(300).start();

            isFront = false;
        } else {
            cardBack.animate().rotationY(180f).setDuration(300).withEndAction(() ->
                    cardBack.setVisibility(View.GONE)
            ).start();

            cardFront.setVisibility(View.VISIBLE);
            cardFront.setRotationY(-180f);
            cardFront.animate().rotationY(0f).setDuration(300).start();

            isFront = true;
        }
    }

    /**
     * Show a dialog to name the deck, then save to Room.
     */
    private void promptSaveDeck() {
        String defaultTitle = "Flashcards " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());

        EditText input = new EditText(this);
        input.setText(defaultTitle);
        input.setSelectAllOnFocus(true);
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Save Deck")
                .setMessage("Enter a name for this deck:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) title = defaultTitle;
                    saveDeckToRoom(title);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveDeckToRoom(String title) {
        if (cards == null || cards.isEmpty()) return;

        long now = System.currentTimeMillis();

        FlashcardDeckEntity deck = new FlashcardDeckEntity();
        deck.title = title;
        deck.createdAt = now;
        deck.updatedAt = now;
        deck.cardCount = cards.size();
        // Preview from first card
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
                Toast.makeText(this, getString(R.string.flashcards_saved), Toast.LENGTH_SHORT).show();
                btnSave.setVisibility(View.GONE);
            });
        });
    }
}