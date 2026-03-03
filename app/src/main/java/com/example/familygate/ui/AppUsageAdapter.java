package com.example.familygate.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familygate.R;
import com.example.familygate.data.AppUsage;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {
    public interface OnBlockToggleListener {
        void onToggle(String packageName, boolean blocked);
    }

    private final List<AppUsage> data = new ArrayList<>();
    private final OnBlockToggleListener onBlockToggleListener;

    public AppUsageAdapter(OnBlockToggleListener onBlockToggleListener) {
        this.onBlockToggleListener = onBlockToggleListener;
    }

    public void submitList(List<AppUsage> appUsages) {
        data.clear();
        data.addAll(appUsages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppUsage item = data.get(position);
        holder.name.setText(item.getAppName());

        String limitLabel = item.isOverLimit() ? " · Limit reached" : "";
        holder.usage.setText(item.getMinutesToday() + " min today" + limitLabel);

        holder.blockSwitch.setOnCheckedChangeListener(null);
        holder.blockSwitch.setChecked(item.isBlocked());
        holder.blockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onBlockToggleListener.onToggle(item.getPackageName(), isChecked));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView usage;
        MaterialSwitch blockSwitch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_app_name);
            usage = itemView.findViewById(R.id.text_usage_time);
            blockSwitch = itemView.findViewById(R.id.switch_block);
        }
    }
}
