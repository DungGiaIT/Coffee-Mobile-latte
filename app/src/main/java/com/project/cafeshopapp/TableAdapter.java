package com.project.cafeshopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
        return tableList.size();
    }

    @Override
    public Object getItem(int position) {
        return tableList.get(position);
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

        TableModel table = tableList.get(position);
        String formattedNumber = String.format("#Bàn %02d", table.getTableId());
        holder.number.setText(formattedNumber);

        // Gán icon và text tùy theo trạng thái
        switch (table.getStatus()) {
            case "serving":
                holder.icon.setImageResource(R.drawable.ic_reserved);
                holder.statusText.setText("Đang phục vụ");
                break;
            case "paid":
                holder.icon.setImageResource(R.drawable.ic_check_done);
                holder.statusText.setText("Đã thanh toán");
                break;
            case "waiting":
                holder.icon.setImageResource(R.drawable.ic_coffee_clock);
                holder.statusText.setText("Đang chờ");
                break;
            case "empty":
            default:
                holder.icon.setImageResource(R.drawable.ic_table);
                holder.statusText.setText("Trống");
                break;
        }

        return convertView;
    }
}
