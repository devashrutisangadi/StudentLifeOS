package com.example.studentlifeos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LinearLayout timelineContainer;

    private static class ClassItem {
        String time, subject, professorRoom;
        boolean isCurrent;
        boolean isNew;

        ClassItem(String time, String subject, String professorRoom, boolean isCurrent, boolean isNew) {
            this.time = time;
            this.subject = subject;
            this.professorRoom = professorRoom;
            this.isCurrent = isCurrent;
            this.isNew = isNew;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        timelineContainer = root.findViewById(R.id.timelineContainer);

        // TODO: replace this static list with data from Firestore later
        List<ClassItem> classes = new ArrayList<>();
        classes.add(new ClassItem("9:00 - 10:00 AM", "Data Structures", "Prof. Name · Room", true, false));
        classes.add(new ClassItem("11:00 - 12:00 AM", "Data Structure", "Prof. Name · Room", false, false));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, true));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, false));

        for (int i = 0; i < classes.size(); i++) {
            boolean isLast = (i == classes.size() - 1);
            addTimelineRow(classes.get(i), isLast);
        }

        TextView tvSeeAll = root.findViewById(R.id.tvSeeAll);
        tvSeeAll.setOnClickListener(v ->
                Toast.makeText(getContext(), "See all classes - hook this up later", Toast.LENGTH_SHORT).show());

        LinearLayout cardNotesRepo = root.findViewById(R.id.cardNotesRepo);
        cardNotesRepo.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open Notes Repo", Toast.LENGTH_SHORT).show());

        LinearLayout cardPyqPapers = root.findViewById(R.id.cardPyqPapers);
        cardPyqPapers.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open PYQ Papers", Toast.LENGTH_SHORT).show());

        return root;
    }

    private void addTimelineRow(ClassItem item, boolean isLast) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View row = inflater.inflate(R.layout.item_timeline_class, timelineContainer, false);

        View dot = row.findViewById(R.id.timelineDot);
        View line = row.findViewById(R.id.timelineLine);
        TextView tvTime = row.findViewById(R.id.tvClassTime);
        TextView tvSubject = row.findViewById(R.id.tvClassSubject);
        TextView tvProfRoom = row.findViewById(R.id.tvClassProfRoom);
        TextView tvNewBadge = row.findViewById(R.id.tvNewBadge);

        dot.setBackgroundResource(item.isCurrent ? R.drawable.dot_filled_purple : R.drawable.dot_outline_purple);
        line.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        tvTime.setText(item.time);
        tvSubject.setText(item.subject);
        tvProfRoom.setText(item.professorRoom);
        tvNewBadge.setVisibility(item.isNew ? View.VISIBLE : View.GONE);

        timelineContainer.addView(row);
    }
}