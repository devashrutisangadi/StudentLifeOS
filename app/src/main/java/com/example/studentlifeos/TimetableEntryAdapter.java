package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimetableEntryAdapter extends RecyclerView.Adapter<TimetableEntryAdapter.ViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(TimetableEntry entry);
    }

    public interface OnEntryDeleteListener {
        void onEntryDelete(TimetableEntry entry);
    }

    private List<TimetableEntry> entries;
    private final OnEntryClickListener clickListener;
    private final OnEntryDeleteListener deleteListener;

    public TimetableEntryAdapter(List<TimetableEntry> entries, OnEntryClickListener clickListener,
                                  OnEntryDeleteListener deleteListener) {
        this.entries = entries;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<TimetableEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableEntry entry = entries.get(position);
        holder.tvTime.setText(TimeFormatUtil.formatRange(entry.startTime, entry.endTime));
        holder.tvSubject.setText(entry.subjectName != null ? entry.subjectName : "");

        String meta = "";
        if (entry.professorName != null && !entry.professorName.trim().isEmpty()) meta += entry.professorName.trim();
        if (entry.room != null && !entry.room.trim().isEmpty()) {
            meta += meta.isEmpty() ? entry.room.trim() : " · " + entry.room.trim();
        }
        holder.tvMeta.setText(meta);
        holder.tvMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> clickListener.onEntryClick(entry));
        holder.ivDelete.setOnClickListener(v -> deleteListener.onEntryDelete(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSubject, tvMeta;
        ImageView ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvEntryTime);
            tvSubject = itemView.findViewById(R.id.tvEntrySubject);
            tvMeta = itemView.findViewById(R.id.tvEntryMeta);
            ivDelete = itemView.findViewById(R.id.ivDeleteEntry);
        }
    }
}
