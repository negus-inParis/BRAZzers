package com.example.brazzers.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FlashcardDao {
    @Insert
    long insertDeck(FlashcardDeckEntity deck);

    @Insert
    void insertCards(List<FlashcardCardEntity> cards);

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    List<FlashcardDeckEntity> getAllDecks();

    @Transaction
    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    DeckWithCards getDeckWithCards(long deckId);

    @Query("UPDATE decks SET title = :newTitle, updatedAt = :updatedAt WHERE deckId = :deckId")
    void renameDeck(long deckId, String newTitle, long updatedAt);

    @Query("DELETE FROM decks WHERE deckId = :deckId")
    void deleteDeck(long deckId);

    @Update
    void updateDeckMetadata(FlashcardDeckEntity deck);

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    void clearCardsForDeck(long deckId);
}
