package com.project.cafeshopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class TableAdapter extends BaseAdapter {
    private Context context;
    private List<TableModel> tableList;

    public TableAdapter(Context context, List<TableModel> tableList) {
        this.context = context;
        this.tableList = tableList;
    }

    @Override
    public int getCount() {
        return tableList != null ? tableList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return tableList != null && position < tableList.size() ? tableList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView icon;
        TextView number;
        TextView statusText;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_table, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.tableIcon);
            holder.number = convertView.findViewById(R.id.tableNumber);
            holder.statusText = convertView.findViewById(R.id.tableStatus);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position < tableList.size()) {
            TableModel table = tableList.get(position);
            String formattedNumber = String.format("Table %d", table.getTableId());
            holder.number.setText(formattedNumber);

            // Handle status based on database values
            String status = table.getStatus() != null ? table.getStatus().toLowerCase() : "available";

            switch (status) {
                case "reserved":
                    setTableStatus(holder, R.drawable.ic_reserved, "Reserved", R.color.status_serving);
                    break;
                case "occupied":
                case "serving":
                    setTableStatus(holder, R.drawable.ic_coffee_clock, "Currently serving", R.color.status_waiting);
                    break;
                case "available":
                default:
                    setTableStatus(holder, R.drawable.ic_table, "Available", R.color.status_empty);
                    break;
            }
        }

        return convertView;
    }

    private void setTableStatus(ViewHolder holder, int iconRes, String statusText, int colorRes) {
        try {
            holder.icon.setImageResource(iconRes);
            holder.statusText.setText(statusText);
            holder.statusText.setTextColor(ContextCompat.getColor(context, colorRes));
        } catch (Exception e) {
            // Fallback if resources not found
            holder.icon.setImageResource(android.R.drawable.ic_menu_info_details);
            holder.statusText.setText(statusText);
            holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }
    }
}