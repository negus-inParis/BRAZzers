package com.example.brazzers.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class DeckWithCards {
    @Embedded
    public FlashcardDeckEntity deck;

    @Relation(
            parentColumn = "deckId",
            entityColumn = "deckId"
    )
    public List<FlashcardCardEntity> cards;
}
