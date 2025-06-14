package com.project.cafeshopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying order items in a RecyclerView
 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_product, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public void updateData(List<OrderItem> newItems) {
        this.orderItems = newItems;
        notifyDataSetChanged();
    }

    class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName;
        private TextView customizations;
        private TextView itemQty;
        private TextView unitPrice;
        private TextView totalPrice;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            customizations = itemView.findViewById(R.id.customizations);
            itemQty = itemView.findViewById(R.id.itemQty);
            unitPrice = itemView.findViewById(R.id.unitPrice);
            totalPrice = itemView.findViewById(R.id.totalPrice);
        }

        public void bind(OrderItem item) {
            // Build the title based on available data
            String titleDisplay = item.getTitle() != null ? item.getTitle() : "Unknown Product";

            // Only add size if it's available (we might not have requested it)
            if (item.getSize() != null && !item.getSize().isEmpty()) {
                titleDisplay += " (Size " + item.getSize() + ")";
            }
            itemName.setText(titleDisplay);

            // Only show customizations if milk field is available (we might not have
            // requested it)
            if (item.getMilk() != null && !item.getMilk().isEmpty()) {
                customizations.setVisibility(View.VISIBLE);
                customizations.setText("Sữa: " + item.getMilk());
            } else {
                customizations.setVisibility(View.GONE);
            }

            // Show quantity
            itemQty.setText("Số lượng: " + item.getQuantity());

            // Show unit price if available
            if (item.getPrice() > 0) {
                unitPrice.setText(String.format("%.2f€", item.getPrice()));

                // Show total price (price * quantity)
                double total = item.getPrice() * item.getQuantity();
                totalPrice.setText(String.format("%.2f€", total));
            } else {
                unitPrice.setText("N/A");
                totalPrice.setText("N/A");
            }
        }
    }
}
