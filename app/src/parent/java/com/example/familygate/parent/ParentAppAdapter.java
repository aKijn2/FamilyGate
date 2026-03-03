package com.example.familygate.parent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familygate.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the parent app manager.
 * Displays each installed app with a toggle to mark it as blocked.
 */
public class ParentAppAdapter extends RecyclerView.Adapter<ParentAppAdapter.ViewHolder> {

    public interface OnBlockToggleListener {
        void onToggle(String packageName, boolean blocked);
    }

    private final List<ParentViewModel.ParentAppInfo> data = new ArrayList<>();
    private final OnBlockToggleListener listener;

    public ParentAppAdapter(OnBlockToggleListener listener) {
        this.listener = listener;
    }

    /** Replaces the entire dataset and redraws the list. */
    public void submitList(List<ParentViewModel.ParentAppInfo> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parent_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParentViewModel.ParentAppInfo item = data.get(position);

        holder.appName.setText(item.appName);
        holder.packageName.setText(item.packageName);

        // Detach listener before programmatically setting state to avoid feedback loops
        holder.blockSwitch.setOnCheckedChangeListener(null);
        holder.blockSwitch.setChecked(item.blocked);
        holder.blockSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            item.blocked = isChecked;   // update in-memory state immediately
            listener.onToggle(item.packageName, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView appName;
        final TextView packageName;
        final MaterialSwitch blockSwitch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.text_parent_app_name);
            packageName = itemView.findViewById(R.id.text_parent_package_name);
            blockSwitch = itemView.findViewById(R.id.switch_parent_block);
        }
    }
}
