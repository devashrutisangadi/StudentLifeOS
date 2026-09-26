package com.example.studentlifeos;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Editable review list shown after a timetable PDF is parsed: each draft entry can be
 * corrected in place (including reassigning its day, in case the model misread the table)
 * or excluded via the checkbox, before the batch is saved to Firestore.
 */
public class TimetableReviewAdapter extends RecyclerView.Adapter<TimetableReviewAdapter.ViewHolder> {

    private final List<TimetableEntry> drafts;
    private final boolean[] included;

    public TimetableReviewAdapter(List<TimetableEntry> drafts) {
        this.drafts = drafts;
        this.drafts.sort((a, b) -> {
            int dayCompare = Integer.compare(a.dayIndex, b.dayIndex);
            return dayCompare != 0 ? dayCompare : a.startTime.compareTo(b.startTime);
        });
        this.included = new boolean[drafts.size()];
        java.util.Arrays.fill(included, true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableEntry draft = drafts.get(position);

        if (holder.dayAdapter == null) {
            holder.dayAdapter = new ArrayAdapter<>(holder.itemView.getContext(),
                    android.R.layout.simple_spinner_item, TimetableEntry.DAY_NAMES);
            holder.dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerDay.setAdapter(holder.dayAdapter);
        }
        holder.spinnerDay.setOnItemSelectedListener(null);
        holder.spinnerDay.setSelection(Math.max(0, draft.dayIndex - 1));
        holder.spinnerDay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                draft.dayIndex = pos + 1;
                draft.dayName = TimetableEntry.dayNameFor(draft.dayIndex);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(included[position]);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> included[position] = isChecked);

        setFieldWithWatcher(holder.etStartTime, holder.startWatcher, draft.startTime, text -> draft.startTime = text);
        holder.startWatcher = lastWatcher;
        setFieldWithWatcher(holder.etEndTime, holder.endWatcher, draft.endTime, text -> draft.endTime = text);
        holder.endWatcher = lastWatcher;
        setFieldWithWatcher(holder.etSubject, holder.subjectWatcher, draft.subjectName, text -> draft.subjectName = text);
        holder.subjectWatcher = lastWatcher;
        setFieldWithWatcher(holder.etProfessor, holder.professorWatcher, draft.professorName, text -> draft.professorName = text);
        holder.professorWatcher = lastWatcher;
        setFieldWithWatcher(holder.etRoom, holder.roomWatcher, draft.room, text -> draft.room = text);
        holder.roomWatcher = lastWatcher;
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    /** Returns only the drafts the user left checked, with current edited values. */
    public List<TimetableEntry> getIncludedDrafts() {
        List<TimetableEntry> result = new java.util.ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            TimetableEntry d = drafts.get(i);
            boolean valid = d.subjectName != null && !d.subjectName.trim().isEmpty()
                    && d.startTime != null && !d.startTime.trim().isEmpty()
                    && d.endTime != null && !d.endTime.trim().isEmpty()
                    && d.dayIndex >= 1 && d.dayIndex <= 7;
            if (included[i] && valid) result.add(d);
        }
        return result;
    }

    private interface TextChanged {
        void onChanged(String text);
    }

    private TextWatcher lastWatcher;

    private void setFieldWithWatcher(EditText field, TextWatcher oldWatcher, String value, TextChanged onChange) {
        if (oldWatcher != null) field.removeTextChangedListener(oldWatcher);
        field.setText(value);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                onChange.onChanged(s.toString().trim());
            }
        };
        field.addTextChangedListener(watcher);
        lastWatcher = watcher;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        Spinner spinnerDay;
        ArrayAdapter<String> dayAdapter;
        EditText etStartTime, etEndTime, etSubject, etProfessor, etRoom;
        TextWatcher startWatcher, endWatcher, subjectWatcher, professorWatcher, roomWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxIncludeEntry);
            spinnerDay = itemView.findViewById(R.id.spinnerReviewDay);
            etStartTime = itemView.findViewById(R.id.etReviewStartTime);
            etEndTime = itemView.findViewById(R.id.etReviewEndTime);
            etSubject = itemView.findViewById(R.id.etReviewSubject);
            etProfessor = itemView.findViewById(R.id.etReviewProfessor);
            etRoom = itemView.findViewById(R.id.etReviewRoom);
        }
    }
}
