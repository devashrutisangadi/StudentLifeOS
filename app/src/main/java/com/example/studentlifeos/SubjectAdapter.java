package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    public static class Subject {
        public String id, name, code, faculty;
        public int progress;
    }

    private List<Subject> subjects;
    private final OnSubjectClickListener listener;

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    public void updateData(List<Subject> newSubjects) {
        this.subjects = newSubjects;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        holder.tvName.setText(subject.name != null ? subject.name : "Untitled subject");
        holder.tvFaculty.setText(subject.faculty != null ? subject.faculty : "—");
        holder.tvProgress.setText(subject.progress + "%");

        String initials = subject.code != null && subject.code.length() >= 2
                ? subject.code.substring(0, 2).toUpperCase()
                : (subject.name != null && subject.name.length() >= 2
                ? subject.name.substring(0, 2).toUpperCase() : "SU");
        holder.tvBadge.setText(initials);

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(subject));
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvName, tvFaculty, tvProgress;

        ViewHolder(View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tvSubjectBadge);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvFaculty = itemView.findViewById(R.id.tvSubjectFaculty);
            tvProgress = itemView.findViewById(R.id.tvSubjectProgress);
        }
    }
}