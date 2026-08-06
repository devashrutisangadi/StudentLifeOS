package com.example.studentlifeos;

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

    public interface OnSubjectDeleteListener {
        void onSubjectDelete(Subject subject);
    }

    public interface OnSubjectAttendanceListener {
        void onSubjectAttendance(Subject subject);
    }

    public static class Subject {
        public String id, name, code, faculty;
        public int progress;
        public Long totalLecturesPlanned; // null if not set
    }

    private List<Subject> subjects;
    private final OnSubjectClickListener listener;
    private final OnSubjectDeleteListener deleteListener; // null = delete icon hidden
    private final OnSubjectAttendanceListener attendanceListener; // null = attendance icon hidden

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener) {
        this(subjects, listener, null, null);
    }

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener,
                          OnSubjectDeleteListener deleteListener) {
        this(subjects, listener, deleteListener, null);
    }

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener,
                          OnSubjectDeleteListener deleteListener,
                          OnSubjectAttendanceListener attendanceListener) {
        this.subjects = subjects;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.attendanceListener = attendanceListener;
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
        holder.progressBar.setProgress(subject.progress);

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(subject));

        if (deleteListener != null) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> deleteListener.onSubjectDelete(subject));
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }

        if (attendanceListener != null) {
            holder.ivAttendance.setVisibility(View.VISIBLE);
            holder.ivAttendance.setOnClickListener(v -> attendanceListener.onSubjectAttendance(subject));
        } else {
            holder.ivAttendance.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivDelete, ivAttendance;
        TextView tvName, tvFaculty, tvProgress;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivSubjectIcon);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvFaculty = itemView.findViewById(R.id.tvSubjectFaculty);
            tvProgress = itemView.findViewById(R.id.tvSubjectProgress);
            progressBar = itemView.findViewById(R.id.progressSubject);
            ivDelete = itemView.findViewById(R.id.ivDeleteSubject);
            ivAttendance = itemView.findViewById(R.id.ivAttendance);
        }
    }
}