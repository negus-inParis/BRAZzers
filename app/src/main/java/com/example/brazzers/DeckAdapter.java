package com.example.brazzers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brazzers.data.FlashcardDeckEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {

    public interface OnDeckActionListener {
        void onDeckClick(FlashcardDeckEntity deck);
        void onDeckDelete(FlashcardDeckEntity deck);
        void onDeckRename(FlashcardDeckEntity deck);
    }

    private List<FlashcardDeckEntity> decks;
    private final OnDeckActionListener listener;

    public DeckAdapter(List<FlashcardDeckEntity> decks, OnDeckActionListener listener) {
        this.decks = decks;
        this.listener = listener;
    }

    public void updateDecks(List<FlashcardDeckEntity> newDecks) {
        this.decks = newDecks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        FlashcardDeckEntity deck = decks.get(position);

        holder.tvTitle.setText(deck.title);
        holder.tvCardCount.setText(deck.cardCount + " cards");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(deck.createdAt)));

        if (deck.previewText != null && !deck.previewText.isEmpty()) {
            holder.tvPreview.setText(deck.previewText);
            holder.tvPreview.setVisibility(View.VISIBLE);
        } else {
            holder.tvPreview.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onDeckClick(deck));

        holder.btnDelete.setOnClickListener(v -> listener.onDeckDelete(deck));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeckRename(deck);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    static class DeckViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCardCount, tvDate, tvPreview;
        ImageButton btnDelete;

        DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDeckTitle);
            tvCardCount = itemView.findViewById(R.id.tvDeckCardCount);
            tvDate = itemView.findViewById(R.id.tvDeckDate);
            tvPreview = itemView.findViewById(R.id.tvDeckPreview);
            btnDelete = itemView.findViewById(R.id.btnDeleteDeck);
        }
    }
}
