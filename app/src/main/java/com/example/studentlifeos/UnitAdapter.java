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

    public static class Unit {
        public String id, title;
        public boolean completed;
    }

    private List<Unit> units;
    private final OnUnitToggleListener listener;

    public UnitAdapter(List<Unit> units, OnUnitToggleListener listener) {
        this.units = units;
        this.listener = listener;
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
                listener.onUnitToggle(unit, isChecked));
    }

    @Override
    public int getItemCount() {
        return units.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvTitle;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxUnit);
            tvTitle = itemView.findViewById(R.id.tvUnitTitle);
        }
    }
}