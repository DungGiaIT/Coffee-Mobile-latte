package com.project.cafeshopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orders;
    private Context context;
    private OrderClickListener listener;

    public interface OrderClickListener {
        void onViewDetailsClick(Order order);
        void onUpdateStatusClick(Order order);
    }

    // Constructors to support both new Order list and old OrderItem list
    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    public OrderAdapter(List<Order> orders, OrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (orders != null && position < orders.size()) {
            Order order = orders.get(position);
            holder.bind(order);
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    // Update the dataset
    public void updateData(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderIdDetail, orderStatus, totalPrice;
        TextView deliveryIcon, deliveryMethod;
        LinearLayout customerLayout, addressLayout, noteLayout;
        MaterialButton btnViewDetails, btnUpdateStatus;
        MaterialCardView statusCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find all views with null checking
            orderId = itemView.findViewById(R.id.orderId);
            orderIdDetail = itemView.findViewById(R.id.orderIdDetail);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            deliveryIcon = itemView.findViewById(R.id.deliveryIcon);
            deliveryMethod = itemView.findViewById(R.id.deliveryMethod);
            statusCard = itemView.findViewById(R.id.statusCard);

            customerLayout = itemView.findViewById(R.id.customerLayout);
            addressLayout = itemView.findViewById(R.id.addressLayout);
            noteLayout = itemView.findViewById(R.id.noteLayout);

            // Buttons
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }

        public void bind(Order order) {
            if (order == null) return;

            // Set basic order info with null checking
            if (orderId != null) {
                // Hiển thị table hoặc delivery info
                if (order.getTableId() != null && !order.getTableId().equals("NULL")) {
                    orderId.setText("Đơn hàng của " + order.getTableId());
                } else {
                    orderId.setText("Đơn hàng giao hàng");
                }
            }

            if (orderIdDetail != null) {
                orderIdDetail.setText("ID: " + order.getId());
            }

            // Set status with appropriate styling
            if (orderStatus != null) {
                String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING";
                orderStatus.setText(status);
                setupStatusColor(status);
            }

            // Set price - định dạng theo Euro như trong database
            if (totalPrice != null) {
                totalPrice.setText(String.format("%.2f€", order.getTotal()));
            }

            // Set delivery method
            setupDeliveryMethod(order.getDeliveryMethod());

            // Hiển thị thông tin khách hàng nếu có
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                TextView customerInfo = itemView.findViewById(R.id.customerInfo);
                if (customerInfo != null && customerLayout != null) {
                    customerLayout.setVisibility(View.VISIBLE);
                    String customerText = "Khách: " + order.getCustomerName();
                    if (order.getCustomerPhone() != null) {
                        customerText += " - " + order.getCustomerPhone();
                    }
                    customerInfo.setText(customerText);
                }
            }

            // Hiển thị note nếu có
            if (order.getNote() != null && !order.getNote().isEmpty() && !order.getNote().equals("EMPTY")) {
                TextView orderNote = itemView.findViewById(R.id.orderNote);
                if (orderNote != null && noteLayout != null) {
                    noteLayout.setVisibility(View.VISIBLE);
                    orderNote.setText("Ghi chú: " + order.getNote());
                }
            }

            // Setup buttons if listener is available
            if (listener != null) {
                if (btnViewDetails != null) {
                    btnViewDetails.setOnClickListener(v -> listener.onViewDetailsClick(order));
                }
                if (btnUpdateStatus != null) {
                    btnUpdateStatus.setOnClickListener(v -> listener.onUpdateStatusClick(order));
                }
            }
        }

        private void setupStatusColor(String status) {
            if (orderStatus == null || context == null) return;

            if (status == null) status = "PENDING";

            int backgroundColor;
            switch (status.toUpperCase()) {
                case "COMPLETED":
                    backgroundColor = context.getResources().getColor(R.color.status_completed);
                    break;
                case "PROCESSING":
                    backgroundColor = context.getResources().getColor(R.color.status_processing);
                    break;
                case "CANCELLED":
                    backgroundColor = context.getResources().getColor(R.color.status_cancelled);
                    break;
                case "PENDING":
                default:
                    backgroundColor = context.getResources().getColor(R.color.status_pending);
                    break;
            }

            orderStatus.setBackgroundColor(backgroundColor);
        }

        private void setupDeliveryMethod(String method) {
            if (deliveryIcon == null || deliveryMethod == null || context == null) return;

            if (method == null) method = "PICKUP";

            String icon;
            int textColor;

            switch (method.toUpperCase()) {
                case "DELIVERY":
                    icon = "🚚";
                    textColor = context.getResources().getColor(R.color.delivery_color);
                    break;
                case "PICKUP":
                default:
                    icon = "🥡";
                    textColor = context.getResources().getColor(R.color.pickup_color);
                    break;
            }

            deliveryIcon.setText(icon);
            deliveryMethod.setText(method.toUpperCase());
            deliveryMethod.setTextColor(textColor);
        }
    }
}