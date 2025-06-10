package com.project.cafeshopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TableRecyclerAdapter extends RecyclerView.Adapter<TableRecyclerAdapter.TableViewHolder> {

    private List<TableModel> tableList;
    private OnTableClickListener onTableClickListener;
    private Context context;

    public interface OnTableClickListener {
        void onTableClick(TableModel table);
    }

    public TableRecyclerAdapter(List<TableModel> tableList, OnTableClickListener listener) {
        this.tableList = tableList;
        this.onTableClickListener = listener;
    }

    @NonNull
    @Override
    public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_table, parent, false);
        return new TableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
        TableModel table = tableList.get(position);
        holder.bind(table, context, onTableClickListener);
    }

    @Override
    public int getItemCount() {
        return tableList != null ? tableList.size() : 0;
    }

    static class TableViewHolder extends RecyclerView.ViewHolder {
        private final ImageView tableIcon;
        private final TextView tableNumber;
        private final TextView tableStatus;

        public TableViewHolder(@NonNull View itemView) {
            super(itemView);
            tableIcon = itemView.findViewById(R.id.tableIcon);
            tableNumber = itemView.findViewById(R.id.tableNumber);
            tableStatus = itemView.findViewById(R.id.tableStatus);
        }

        public void bind(TableModel table, Context context, OnTableClickListener clickListener) {
            if (table == null) return;

            tableNumber.setText(String.format("Bàn %d", table.getTableId()));

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTableClick(table);
                }
            });

            // Handle status based on database values
            String status = table.getStatus() != null ? table.getStatus().toLowerCase() : "available";

            switch (status) {
                case "reserved":
                    setTableStatus(context, R.drawable.ic_reserved, "Đã đặt", R.color.status_serving);
                    break;
                case "occupied":
                case "serving":
                    setTableStatus(context, R.drawable.ic_coffee_clock, "Đang phục vụ", R.color.status_waiting);
                    break;
                case "available":
                default:
                    setTableStatus(context, R.drawable.ic_table, "Còn trống", R.color.status_empty);
                    break;
            }
        }

        private void setTableStatus(Context context, int iconRes, String statusText, int colorRes) {
            try {
                tableIcon.setImageResource(iconRes);
                tableStatus.setText(statusText);
                tableStatus.setTextColor(ContextCompat.getColor(context, colorRes));
            } catch (Exception e) {
                // Fallback if resources not found
                tableIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                tableStatus.setText(statusText);
                tableStatus.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }
        }
    }

    // Method to update data
    public void updateData(List<TableModel> newTableList) {
        if (this.tableList != null) {
            this.tableList.clear();
            if (newTableList != null) {
                this.tableList.addAll(newTableList);
            }
            notifyDataSetChanged();
        }
    }
}