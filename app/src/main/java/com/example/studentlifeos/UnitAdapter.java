package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UnitAdapter extends RecyclerView.Adapter<UnitAdapter.ViewHolder> {

    public interface OnUnitToggleListener {
        void onUnitToggle(Unit unit, boolean isChecked);
    }

    public interface OnUnitClickListener {
        void onUnitClick(Unit unit);
    }

    public static class Unit {
        public String id, title;
        public boolean completed;
    }

    private List<Unit> units;
    private final OnUnitToggleListener toggleListener;
    private final OnUnitClickListener clickListener;

    public UnitAdapter(List<Unit> units, OnUnitToggleListener toggleListener, OnUnitClickListener clickListener) {
        this.units = units;
        this.toggleListener = toggleListener;
        this.clickListener = clickListener;
    }

    public void updateData(List<Unit> newUnits) {
        this.units = newUnits;
        notifyDataSetChanged();
    }

    public List<Unit> getUnits() {
        return units;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Unit unit = units.get(position);
        holder.checkBox.setOnCheckedChangeListener(null); // avoid firing during recycle-bind
        holder.checkBox.setChecked(unit.completed);
        holder.tvTitle.setText(unit.title != null ? unit.title : "Untitled unit");

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                toggleListener.onUnitToggle(unit, isChecked));

        View.OnClickListener openNotes = v -> clickListener.onUnitClick(unit);
        holder.tvTitle.setOnClickListener(openNotes);
        holder.ivOpenNotes.setOnClickListener(openNotes);
    }

    @Override
    public int getItemCount() {
        return units.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvTitle;
        android.widget.ImageView ivOpenNotes;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxUnit);
            tvTitle = itemView.findViewById(R.id.tvUnitTitle);
            ivOpenNotes = itemView.findViewById(R.id.ivOpenNotes);
        }
    }
}