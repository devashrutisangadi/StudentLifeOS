package com.example.studentlifeos;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(subject));

        // Icon mapping (placeholder, keyword-based)
        String name = subject.name != null ? subject.name.toLowerCase() : "";
        int iconRes;
        if (name.contains("java")) {
            iconRes = R.drawable.ic_subject_java;
        } else if (name.contains("data structures")) {
            iconRes = R.drawable.ic_subject_cube;
        } else if (name.contains("digital systems")) {
            iconRes = R.drawable.ic_subject_logic;
        } else if (name.contains("discrete")) {
            iconRes = R.drawable.ic_subject_network;
        } else {
            iconRes = R.drawable.ic_subject_generic;
        }
        holder.ivSubjectIcon.setImageResource(iconRes);

        // Progress bar + tier color
        int progress = subject.progress;
        holder.progressSubject.setProgress(progress);

        int fillColor;
        if (progress < 40) {
            fillColor = Color.parseColor("#E57373");
        } else if (progress < 75) {
            fillColor = Color.parseColor("#F0C05A");
        } else {
            fillColor = Color.parseColor("#7FD99D");
        }
        android.graphics.drawable.LayerDrawable layerDrawable =
                (android.graphics.drawable.LayerDrawable) holder.progressSubject.getProgressDrawable().mutate();
        android.graphics.drawable.Drawable progressLayer =
                layerDrawable.findDrawableByLayerId(android.R.id.progress);
        if (progressLayer != null) {
            progressLayer.setColorFilter(fillColor, PorterDuff.Mode.SRC_IN);
        }
        holder.tvProgress.setText(progress + "%");
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSubjectIcon;
        ProgressBar progressSubject;
        TextView tvName, tvFaculty, tvProgress;

        ViewHolder(View itemView) {
            super(itemView);
            ivSubjectIcon = itemView.findViewById(R.id.ivSubjectIcon);
            progressSubject = itemView.findViewById(R.id.progressSubject);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvFaculty = itemView.findViewById(R.id.tvSubjectFaculty);
            tvProgress = itemView.findViewById(R.id.tvSubjectProgress);
        }
    }
}