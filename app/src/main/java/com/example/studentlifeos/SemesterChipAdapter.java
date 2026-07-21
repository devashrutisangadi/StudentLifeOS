package com.example.studentlifeos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentlifeos.R;
import java.util.List;

public class SemesterChipAdapter extends RecyclerView.Adapter<SemesterChipAdapter.ViewHolder> {

    public interface OnChipClickListener {
        void onChipSelected(String semester);
    }

    private final Context context;
    private final List<String> semesters;
    private final OnChipClickListener listener;
    private int selectedPosition = 0;

    public SemesterChipAdapter(Context context, List<String> semesters, OnChipClickListener listener) {
        this.context = context;
        this.semesters = semesters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_semester_chip, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String label = semesters.get(position);
        holder.chipLabel.setText(label);

        boolean isSelected = position == selectedPosition;
        holder.chipLabel.setBackgroundResource(
                isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        holder.chipLabel.setTextColor(ContextCompat.getColor(context,
                isSelected ? R.color.chip_selected_text : R.color.chip_unselected_text));

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onChipSelected(label);
        });
    }

    @Override
    public int getItemCount() { return semesters.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chipLabel;
        ViewHolder(View itemView) {
            super(itemView);
            chipLabel = itemView.findViewById(R.id.chipLabel);
        }
    }
}