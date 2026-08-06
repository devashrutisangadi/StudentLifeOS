package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceOverviewAdapter extends RecyclerView.Adapter<AttendanceOverviewAdapter.ViewHolder> {

    public interface OnSubjectClickListener {
        void onClick(SubjectSummary subject);
    }

    public static class SubjectSummary {
        public String subjectId, subjectName;
        public int attended, held;
        public boolean hasLogs;
    }

    private final List<SubjectSummary> subjects;
    private final OnSubjectClickListener listener;

    public AttendanceOverviewAdapter(List<SubjectSummary> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    public void notifyItemChangedFor(int position) {
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectSummary s = subjects.get(position);
        holder.tvName.setText(s.subjectName != null ? s.subjectName : "Untitled subject");

        if (!s.hasLogs) {
            holder.tvLectureCount.setText("No lectures logged yet");
            holder.tvPercent.setText("—");
            holder.tvPercent.setTextColor(holder.itemView.getResources().getColor(R.color.hint_gray, holder.itemView.getContext().getTheme()));
        } else {
            holder.tvLectureCount.setText(s.attended + " / " + s.held + " lectures");
            int percent = s.held == 0 ? 0 : (100 * s.attended / s.held);
            holder.tvPercent.setText(percent + "%");
            int colorRes = percent >= 75 ? R.color.checkbox_tint : android.R.color.holo_red_light;
            holder.tvPercent.setTextColor(holder.itemView.getResources().getColor(colorRes, holder.itemView.getContext().getTheme()));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLectureCount, tvPercent;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvOverviewSubjectName);
            tvLectureCount = itemView.findViewById(R.id.tvOverviewLectureCount);
            tvPercent = itemView.findViewById(R.id.tvOverviewPercent);
        }
    }
}