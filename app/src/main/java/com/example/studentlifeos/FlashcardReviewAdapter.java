package com.example.studentlifeos;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Editable review list shown after AI generation: each draft can be edited in place
 * or excluded via the checkbox before the batch is saved to Firestore.
 */
public class FlashcardReviewAdapter extends RecyclerView.Adapter<FlashcardReviewAdapter.ViewHolder> {

    private final List<FlashcardItem> drafts;
    private final boolean[] included;

    public FlashcardReviewAdapter(List<FlashcardItem> drafts) {
        this.drafts = drafts;
        this.included = new boolean[drafts.size()];
        java.util.Arrays.fill(included, true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashcardItem draft = drafts.get(position);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(included[position]);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> included[position] = isChecked);

        holder.etFront.removeTextChangedListener(holder.frontWatcher);
        holder.etFront.setText(draft.front);
        holder.frontWatcher = simpleWatcher(text -> draft.front = text);
        holder.etFront.addTextChangedListener(holder.frontWatcher);

        holder.etBack.removeTextChangedListener(holder.backWatcher);
        holder.etBack.setText(draft.back);
        holder.backWatcher = simpleWatcher(text -> draft.back = text);
        holder.etBack.addTextChangedListener(holder.backWatcher);
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    /** Returns only the drafts the user left checked, with current edited text. */
    public List<FlashcardItem> getIncludedDrafts() {
        List<FlashcardItem> result = new java.util.ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            if (included[i] && !drafts.get(i).front.trim().isEmpty() && !drafts.get(i).back.trim().isEmpty()) {
                result.add(drafts.get(i));
            }
        }
        return result;
    }

    private interface TextChanged {
        void onChanged(String text);
    }

    private TextWatcher simpleWatcher(TextChanged callback) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                callback.onChanged(s.toString());
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        EditText etFront, etBack;
        TextWatcher frontWatcher, backWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxIncludeCard);
            etFront = itemView.findViewById(R.id.etReviewFront);
            etBack = itemView.findViewById(R.id.etReviewBack);
        }
    }
}
