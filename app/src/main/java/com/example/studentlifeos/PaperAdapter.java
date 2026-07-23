package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.ViewHolder> {

    public interface OnPaperClickListener {
        void onPaperClick(Paper paper);
    }

    public static class Paper {
        public String title, examType;
        public int year;
    }

    private final List<Paper> papers;
    private final OnPaperClickListener listener;

    public PaperAdapter(List<Paper> papers, OnPaperClickListener listener) {
        this.papers = papers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paper paper = papers.get(position);
        holder.tvTitle.setText(paper.title);
        holder.itemView.setOnClickListener(v -> listener.onPaperClick(paper));
    }

    @Override
    public int getItemCount() {
        return papers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPaperTitle);
        }
    }
}