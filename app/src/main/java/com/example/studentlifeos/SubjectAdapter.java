package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    public static class Subject {
        public String id;
        public String name;
        public String code;
        public String faculty;
        public int progress;
    }

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    private final List<Subject> subjects;
    private final OnSubjectClickListener listener;

    private static final int[] ICON_BOXES = {
            R.drawable.bg_icon_box_1,
            R.drawable.bg_icon_box_2,
            R.drawable.bg_icon_box_3
    };

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    public void updateData(List<Subject> newSubjects) {
        subjects.clear();
        subjects.addAll(newSubjects);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject s = subjects.get(position);
        holder.name.setText(s.name);
        holder.faculty.setText(s.faculty);
        holder.progress.setText(s.progress + "%");

        String initials = s.name != null && s.name.length() >= 2
                ? s.name.substring(0, 2).toUpperCase()
                : "SU";
        holder.badge.setText(initials);

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(s));
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView badge, name, faculty, progress;

        ViewHolder(View itemView) {
            super(itemView);
            badge = itemView.findViewById(R.id.tvSubjectBadge);
            name = itemView.findViewById(R.id.tvSubjectName);
            faculty = itemView.findViewById(R.id.tvSubjectFaculty);
            progress = itemView.findViewById(R.id.tvSubjectProgress);
        }
    }

}
