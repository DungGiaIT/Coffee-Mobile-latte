package com.project.cafeshopapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class OrderActivity extends AppCompatActivity implements OrderAdapter.OrderClickListener {
    private static final String TAG = "OrderActivity";

    private TextView tableLabel;
    private RecyclerView orderList;
    private MaterialButton confirmBtn;
    private LinearLayout emptyOrderText;

    private int tableNumber;
    private String tableStatus;
    private String tableId;
    private List<Order> orders = new ArrayList<>();
    private OrderAdapter orderAdapter;
    private ApiService apiService;

    // Background processing
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize background threading
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // Get table info from intent
        tableNumber = getIntent().getIntExtra("tableNumber", 1);
        tableStatus = getIntent().getStringExtra("tableStatus");
        tableId = getIntent().getStringExtra("tableId");

        initViews();
        setupRecyclerView();
        loadOrdersFromApi();
    }

    private void initViews() {
        tableLabel = findViewById(R.id.tableLabel);
        orderList = findViewById(R.id.recyclerView);
        confirmBtn = findViewById(R.id.confirmBtn);
        emptyOrderText = findViewById(R.id.emptyOrderText);

        apiService = ApiClient.getClient().create(ApiService.class);

        // Format the status text with proper capitalization and styling
        String statusText = "";
        if (tableStatus != null) {
            String formattedStatus = tableStatus.substring(0, 1).toUpperCase() + tableStatus.substring(1).toLowerCase();
            statusText = " (" + formattedStatus + ")";
        }
        tableLabel.setText("Table " + tableNumber + " - Orders" + statusText);

        confirmBtn.setOnClickListener(v -> {
            handleOrderCompletion();
        });
    }

    private void handleOrderCompletion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm")
                .setMessage("Mark order as completed?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Update all orders for this table to COMPLETED
                    updateAllOrdersStatus("COMPLETED");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orders, this);
        orderList.setLayoutManager(new LinearLayoutManager(this));
        orderList.setAdapter(orderAdapter);
    }    private void loadOrdersFromApi() {
        Log.d(TAG, "Loading orders for table: " + tableNumber);

        // Use the new API endpoint for Orders with select parameter
        String selectFields = "id,tableId,total,status,customerName,customerEmail,customerPhone,note,createdAt";
        Call<List<Order>> call = apiService.getOrdersByTableWithSelect("eq.table" + tableNumber, selectFields);

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Success: Received " + response.body().size() + " orders");

                    orders.clear();
                    orders.addAll(response.body());

                    updateUI();
                } else {
                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                loadSampleData();
                Toast.makeText(OrderActivity.this, "Cannot load orders, showing sample data", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void updateUI() {        if (orders.isEmpty()) {
            emptyOrderText.setVisibility(View.VISIBLE);
            orderList.setVisibility(View.GONE);

            // Update table status to "available" if there are no orders
            if (!"available".equalsIgnoreCase(tableStatus)) {
                tableStatus = "available";
                String formattedStatus = "Available";
                String statusText = " (" + formattedStatus + ")";
                tableLabel.setText("Table " + tableNumber + " - Orders" + statusText);
            }
        } else {
            emptyOrderText.setVisibility(View.GONE);
            orderList.setVisibility(View.VISIBLE);
            orderAdapter.notifyDataSetChanged();

            // Update table status to "reserved" if there are orders
            if (!"reserved".equalsIgnoreCase(tableStatus)) {
                tableStatus = "reserved";
                String formattedStatus = "Reserved";
                String statusText = " (" + formattedStatus + ")";
                tableLabel.setText("Table " + tableNumber + " - Orders" + statusText);
            }
        }
    }

    private void loadSampleData() {
        Log.d(TAG, "Loading sample order data for table: " + tableNumber);

        orders.clear();        // Only add sample orders for tables that should have orders (based on our data
        // model)
        // Tables 3, 4, 7, and 8 should be available (have no orders)
        if (tableNumber != 3 && tableNumber != 4 && tableNumber != 7 && tableNumber != 8) {
            // Sample data based on the screenshot
            Order order1 = new Order("cmbq9wn1n0000lc04r92w06f0", "table" + tableNumber, 13.09, "PENDING");
            order1.setCustomerName("Khanh Le");
            orders.add(order1);
            
            Order order2 = new Order("cmbqmmbzd000jy04tcrpr459", "table" + tableNumber, 5.39, "PENDING");
            order2.setCustomerName("Khanh Le");
            orders.add(order2);
        }

        updateUI();
        Toast.makeText(this, "Showing sample data for table " + tableNumber, Toast.LENGTH_SHORT).show();
    }

    private void updateAllOrdersStatus(String newStatus) {
        if (orders.isEmpty()) {
            Toast.makeText(this, "No orders to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Count for tracking completion
        final int[] completedCount = { 0 };
        final int totalOrders = orders.size();

        for (Order order : orders) {
            OrderStatusUpdate statusUpdate = new OrderStatusUpdate(newStatus);
            Call<List<Order>> call = apiService.updateOrderStatus("eq." + order.getId(), statusUpdate);

            call.enqueue(new Callback<List<Order>>() {
                @Override
                public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                    synchronized (completedCount) {
                        completedCount[0]++;

                        if (completedCount[0] >= totalOrders) {
                            // All updates completed
                            // Set table status to available since all orders completed
                            updateTableToAvailable();

                            mainHandler.post(() -> {
                                Toast.makeText(OrderActivity.this,
                                        "Order for table " + tableNumber + " has been completed",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Order>> call, Throwable t) {
                    synchronized (completedCount) {
                        completedCount[0]++;

                        if (completedCount[0] >= totalOrders) {
                            // All updates completed but with errors
                            mainHandler.post(() -> {
                                Toast.makeText(OrderActivity.this, "An error occurred while updating orders",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    }
                }
            });
        }
    }

    /**
     * Updates the table status to available after all orders are completed
     */
    private void updateTableToAvailable() {
        // Update table status to available
        TableUpdateRequest updateRequest = new TableUpdateRequest("available");

        Call<List<TableModel>> call = apiService.updateTableStatusById("eq." + tableNumber, updateRequest);
        call.enqueue(new Callback<List<TableModel>>() {
            @Override
            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Table " + tableNumber + " status updated to available after order completion");
                } else {
                    Log.e(TAG, "Failed to update table status. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {
                Log.e(TAG, "Error updating table status: " + t.getMessage());
            }
        });
    }

    @Override
    public void onViewDetailsClick(Order order) {
        Toast.makeText(this, "Opening order details " + order.getId(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
    }

    @Override
    public void onUpdateStatusClick(Order order) {
        String[] statuses = { "PENDING", "PROCESSING", "COMPLETED", "CANCELLED" };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Status")
                .setItems(statuses, (dialog, which) -> {
                    updateOrderStatus(order, statuses[which]);
                })
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        OrderStatusUpdate statusUpdate = new OrderStatusUpdate(newStatus);
        Call<List<Order>> call = apiService.updateOrderStatus("eq." + order.getId(), statusUpdate);

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderActivity.this,
                            "Update successful: " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Refresh orders
                    loadOrdersFromApi();
                } else {
                    Toast.makeText(OrderActivity.this,
                            "Cannot update (Error: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(OrderActivity.this,
                        "Connection error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads order items for a specific order
     * 
     * @param order The order to load items for
     */
    private void loadOrderItems(Order order) {
        if (order == null || order.getId() == null) {
            Toast.makeText(this, "Cannot load order details", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Loading order items for order: " + order.getId());

        // Use RPC function to get order items - this bypasses RLS issues
        Call<List<OrderItem>> call = apiService.getOrderItemsViaRPC(order.getId());
        call.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<OrderItem> items = response.body();
                    Log.d(TAG, "API Success: Received " + items.size() + " order items");

                    if (items.isEmpty()) {
                        Toast.makeText(OrderActivity.this, "No products in this order",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        displayOrderItems(items);
                    }
                } else {
                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());
                    Toast.makeText(OrderActivity.this, "Cannot load order details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                Toast.makeText(OrderActivity.this, "Connection error when loading order details", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    /**
     * Displays order items in a dialog
     * 
     * @param items The list of order items to display
     */
    private void displayOrderItems(List<OrderItem> items) {
        // Create a recycler view to display the items
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up the adapter
        OrderItemAdapter adapter = new OrderItemAdapter(items);
        recyclerView.setAdapter(adapter); // Show the dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle("Order Details")
                .setView(recyclerView)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        // Return to previous activity
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}