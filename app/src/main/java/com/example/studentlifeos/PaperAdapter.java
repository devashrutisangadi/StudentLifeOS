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

    public interface OnPaperDeleteListener {
        void onPaperDelete(Paper paper);
    }

    public static class Paper {
        public String id, title, examType, fileUrl, fileType;
        public int year;
    }

    private List<Paper> papers;
    private final OnPaperClickListener listener;
    private final OnPaperDeleteListener deleteListener;

    public PaperAdapter(List<Paper> papers, OnPaperClickListener listener, OnPaperDeleteListener deleteListener) {
        this.papers = papers;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<Paper> newPapers) {
        this.papers = newPapers;
        notifyDataSetChanged();
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
        holder.tvTitle.setText(paper.title != null ? paper.title : "Untitled");
        holder.tvMeta.setText((paper.examType != null ? paper.examType : "—") + " · " + paper.year);
        holder.itemView.setOnClickListener(v -> listener.onPaperClick(paper));

        if (deleteListener != null) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> deleteListener.onPaperDelete(paper));
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return papers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta;
        View ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPaperTitle);
            tvMeta = itemView.findViewById(R.id.tvPaperMeta);
            ivDelete = itemView.findViewById(R.id.ivDeletePaper);
        }
    }
}