package com.example.studentlifeos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.VH> {

    public interface Listener {
        void onClick(NoteItem note);
        void onLongClick(NoteItem note);
    }

    private final List<NoteItem> items;
    private final Listener listener;

    public NoteAdapter(List<NoteItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NoteItem n = items.get(position);

        h.tvTitle.setText(n.displayTitle());

        String preview = n.preview();
        h.tvPreview.setVisibility(preview.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvPreview.setText(preview);

        if (n.hasFile()) {
            String type = n.fileType != null && !n.fileType.isEmpty() ? n.fileType.toUpperCase() : "FILE";
            h.tvFile.setVisibility(View.VISIBLE);
            h.tvFile.setText("📎 " + type + (n.fileName != null ? " · " + n.fileName : ""));
        } else {
            h.tvFile.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(n));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(n);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvPreview, tvFile;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvPreview = itemView.findViewById(R.id.tvNotePreview);
            tvFile = itemView.findViewById(R.id.tvNoteFile);
        }
    }
}