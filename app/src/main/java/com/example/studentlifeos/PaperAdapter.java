package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Shows papers grouped by year, with a Mid-Semester and End-Semester row under each year.
 * If a paper hasn't been uploaded for a given year/exam type, a "Coming Soon" placeholder
 * row is shown instead so the layout matches the shared design (see AddPaperActivity for
 * how real rows get created).
 */
public class PaperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final String MID = "Mid-Semester";
    private static final String END = "End-Semester";

    public interface OnPaperClickListener {
        void onPaperClick(Paper paper);
    }

    public interface OnPaperDeleteListener {
        void onPaperDelete(Paper paper);
    }

    public static class Paper {
        public String id, title, examType, fileUrl, fileType, studentId;
        public int year;
        public boolean isPlaceholder;
    }

    private static class Row {
        boolean isHeader;
        int year;
        Paper paper;
    }

    private List<Paper> realPapers;
    private final List<Row> rows = new ArrayList<>();
    private String filterExamType = null; // null = "All"
    private final String currentUserId;
    private final OnPaperClickListener listener;
    private final OnPaperDeleteListener deleteListener;

    public PaperAdapter(List<Paper> papers, String currentUserId,
                        OnPaperClickListener listener, OnPaperDeleteListener deleteListener) {
        this.realPapers = papers;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.deleteListener = deleteListener;
        rebuildRows();
    }

    public void updateData(List<Paper> newPapers) {
        this.realPapers = newPapers;
        rebuildRows();
    }

    /** Pass "Mid-Semester", "End-Semester", or null for "All". */
    public void setFilter(String examType) {
        this.filterExamType = examType;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();

        TreeSet<Integer> years = new TreeSet<>(Collections.reverseOrder());
        for (Paper p : realPapers) years.add(p.year);

        for (int year : years) {
            List<Row> yearRows = new ArrayList<>();
            for (Paper p : realPapers) {
                if (p.year != year) continue;
                if (filterExamType != null && !filterExamType.equals(p.examType)) continue;

                Row row = new Row();
                row.year = year;
                row.paper = p;
                yearRows.add(row);
            }
            if (!yearRows.isEmpty()) {
                Row header = new Row();
                header.isHeader = true;
                header.year = year;
                rows.add(header);
                rows.addAll(yearRows);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paper_year_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paper, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvYear.setText(String.valueOf(row.year));
            return;
        }

        ItemViewHolder h = (ItemViewHolder) holder;
        Paper paper = row.paper;
        String genericLabel = MID.equals(paper.examType) ? "Mid-Semester Exam" : "End-Semester Exam";

        if (paper.isPlaceholder) {
            h.tvTitle.setText(genericLabel);
            h.tvMeta.setText("Not uploaded yet");
            h.tvComingSoon.setVisibility(View.VISIBLE);
            h.btnDownload.setVisibility(View.GONE);
            h.ivDelete.setVisibility(View.GONE);
            h.itemView.setOnClickListener(null);
            h.itemView.setAlpha(0.6f);
        } else {
            h.tvTitle.setText(paper.title != null && !paper.title.isEmpty() ? paper.title : genericLabel);
            h.tvMeta.setText(paper.fileType != null ? paper.fileType.toUpperCase() : "FILE");
            h.tvComingSoon.setVisibility(View.GONE);
            h.btnDownload.setVisibility(View.VISIBLE);
            h.itemView.setAlpha(1f);
            h.itemView.setOnClickListener(v -> listener.onPaperClick(paper));
            h.btnDownload.setOnClickListener(v -> listener.onPaperClick(paper));

            boolean isOwnPaper = currentUserId != null && currentUserId.equals(paper.studentId);
            if (deleteListener != null && isOwnPaper) {
                h.ivDelete.setVisibility(View.VISIBLE);
                h.ivDelete.setOnClickListener(v -> deleteListener.onPaperDelete(paper));
            } else {
                h.ivDelete.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvYear;

        HeaderViewHolder(View itemView) {
            super(itemView);
            tvYear = itemView.findViewById(R.id.tvYearHeader);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvComingSoon, btnDownload;
        View ivDelete;

        ItemViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPaperTitle);
            tvMeta = itemView.findViewById(R.id.tvPaperMeta);
            tvComingSoon = itemView.findViewById(R.id.tvComingSoon);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            ivDelete = itemView.findViewById(R.id.ivDeletePaper);
        }
    }
}
