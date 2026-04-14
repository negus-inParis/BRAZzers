package com.example.brazzers.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "decks")
public class FlashcardDeckEntity {
    @PrimaryKey(autoGenerate = true)
    public long deckId;

    public String title;
    public long createdAt;
    public long updatedAt;
    public int cardCount;
    public String previewText;
}
