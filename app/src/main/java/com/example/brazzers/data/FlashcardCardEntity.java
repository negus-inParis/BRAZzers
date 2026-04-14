package com.example.brazzers.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cards",
        indices = {@Index("deckId")},
        foreignKeys = @ForeignKey(
                entity = FlashcardDeckEntity.class,
                parentColumns = "deckId",
                childColumns = "deckId",
                onDelete = ForeignKey.CASCADE
        ))
public class FlashcardCardEntity {
    @PrimaryKey(autoGenerate = true)
    public long cardId;

    public long deckId;
    public int position;
    public String frontText;
    public String backText;
}
