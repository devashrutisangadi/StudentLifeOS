package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {

    public interface OnCardClickListener {
        void onCardClick(FlashcardItem card);
    }

    public interface OnCardDeleteListener {
        void onCardDelete(FlashcardItem card);
    }

    private List<FlashcardItem> cards;
    private final OnCardClickListener clickListener;
    private final OnCardDeleteListener deleteListener;

    public FlashcardAdapter(List<FlashcardItem> cards, OnCardClickListener clickListener,
                             OnCardDeleteListener deleteListener) {
        this.cards = cards;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<FlashcardItem> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }

    public List<FlashcardItem> getCards() {
        return cards;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashcardItem card = cards.get(position);
        holder.tvFront.setText(card.front != null ? card.front : "");
        holder.tvBack.setText(card.back != null ? card.back : "");
        holder.tvSourceBadge.setVisibility("generated".equals(card.source) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> clickListener.onCardClick(card));
        holder.ivDelete.setOnClickListener(v -> deleteListener.onCardDelete(card));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFront, tvBack, tvSourceBadge;
        ImageView ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvFront = itemView.findViewById(R.id.tvCardFront);
            tvBack = itemView.findViewById(R.id.tvCardBack);
            tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
            ivDelete = itemView.findViewById(R.id.ivDeleteCard);
        }
    }
}
