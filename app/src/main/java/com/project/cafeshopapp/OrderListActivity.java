package com.project.cafeshopapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OrderClickListener {

    private static final String TAG = "OrderListActivity";
    
    // UI components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyView;
    
    // Stats TextViews
    private TextView totalOrdersText;
    private TextView pendingOrdersText;
    private TextView completedOrdersText;

    // Data
    private List<Order> orderList = new ArrayList<>();
    private OrderAdapter orderAdapter;
    private ApiService apiService;

    // Background processing
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Auto-refresh mechanism (optimized for real-time)
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 3000; // 3 seconds for real-time updates
    private boolean isRefreshing = false;
    
    // Broadcast receiver for instant updates
    private BroadcastReceiver orderUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        // Initialize background threading
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize API service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Initialize views
        initViews();
        
        // Setup RecyclerView and SwipeRefreshLayout
        setupRecyclerView();
        setupSwipeRefresh();
        
        // Setup broadcast receiver for instant updates
        setupBroadcastReceiver();
        
        // Setup auto-refresh mechanism
        setupAutoRefresh();

        // Load orders
        fetchOrders();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewOrders);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
        
        // Initialize stats TextViews
        totalOrdersText = findViewById(R.id.totalOrdersText);
        pendingOrdersText = findViewById(R.id.pendingOrdersText);
        completedOrdersText = findViewById(R.id.completedOrdersText);
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orderList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(orderAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::fetchOrders);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void setupBroadcastReceiver() {
        orderUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("ORDER_UPDATE".equals(intent.getAction())) {
                    Log.d(TAG, "🔥 Received ORDER_UPDATE broadcast - forcing immediate refresh");
                    fetchOrders(true); // Force refresh bypassing isRefreshing flag
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("ORDER_UPDATE");
        ContextCompat.registerReceiver(this, orderUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "📡 BroadcastReceiver registered for ORDER_UPDATE");
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔄 Auto-refreshing orders...");
                fetchOrders();
                
                // Schedule next refresh
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    private void startAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            Log.d(TAG, "📡 Auto-refresh started (interval: " + REFRESH_INTERVAL + "ms)");
        }
    }

    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            Log.d(TAG, "📡 Auto-refresh stopped");
        }
    }

    private void fetchOrders() {
        fetchOrders(false);
    }
    
    private void fetchOrders(boolean forceRefresh) {
        if (isRefreshing && !forceRefresh) {
            Log.d(TAG, "Already refreshing orders, skipping...");
            return;
        }
        
        isRefreshing = true;
        Log.d(TAG, "🔄 Fetching orders from API..." + (forceRefresh ? " (FORCED)" : ""));
        swipeRefreshLayout.setRefreshing(true);

        // Use FRESH endpoint with no-cache header to force database read
        String selectFields = "id,tableId,total,status,customerName,customerEmail,customerPhone,note,createdAt";
        Call<List<Order>> call = apiService.getAllOrdersFresh(selectFields);

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;

                Log.d(TAG, "📊 API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Fetched " + response.body().size() + " orders successfully");
                    
                    // Process immediately on main thread for faster updates
                    processOrders(response.body());
                } else {
                    // Log detailed error
                    String errorMessage = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }

                    Log.e(TAG, "❌ API Failed: " + response.code() + " - " + response.message());
                    Log.e(TAG, "❌ Error Body: " + errorMessage);

                    // Only show error message, don't try fallback to avoid delays
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            Toast.makeText(OrderListActivity.this, 
                                "Failed to load orders. Retrying...", 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;
                Log.e(TAG, "🌐 Network Error: " + t.getMessage(), t);
                
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        Toast.makeText(OrderListActivity.this, 
                            "Network error. Retrying...", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void processOrders(List<Order> orders) {
        try {
            // Log sample data for debugging
            if (!orders.isEmpty()) {
                Order firstOrder = orders.get(0);
                Log.d(TAG, "📋 Sample order: " + firstOrder.getId() +
                        " - Table: " + firstOrder.getTableId() +
                        " - Total: " + firstOrder.getTotal() +
                        " - Status: " + firstOrder.getStatus());
            }

            // Update UI immediately on main thread for faster updates
            orderList.clear();
            orderList.addAll(orders);
            orderAdapter.notifyDataSetChanged();

            updateEmptyViewVisibility();
            updateOrderStats();

            Log.d(TAG, "✅ Orders updated in UI: " + orders.size() + " orders");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing orders: " + e.getMessage(), e);
            Toast.makeText(OrderListActivity.this,
                    "Data processing error", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyViewVisibility() {
        if (orderList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateOrderStats() {
        int totalOrders = orderList.size();
        int pendingOrders = 0;
        int completedOrders = 0;

        for (Order order : orderList) {
            if ("pending".equals(order.getStatus())) {
                pendingOrders++;
            } else if ("completed".equals(order.getStatus())) {
                completedOrders++;
            }
        }

        totalOrdersText.setText("Total: " + totalOrders);
        pendingOrdersText.setText("Pending: " + pendingOrders);
        completedOrdersText.setText("Completed: " + completedOrders);
    }

    private void handleApiError(String message) {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }    @Override
    public void onViewDetailsClick(Order order) {
        // Navigate to order details
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
    }

    @Override
    public void onUpdateStatusClick(Order order) {
        // Show status update dialog
        String[] statuses = {"pending", "completed", "cancelled"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Order Status")
                .setItems(statuses, (dialog, which) -> {
                    String newStatus = statuses[which];
                    updateOrderStatus(order, newStatus);
                })
                .show();
    }    private void updateOrderStatus(Order order, String newStatus) {
        // Implementation for update order status
        Toast.makeText(this, "Update order " + order.getId() + " to " + newStatus, Toast.LENGTH_SHORT).show();
        
        // Force refresh after status update
        fetchOrders(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 Activity resumed - forcing immediate refresh + auto refresh");
        
        // Force immediate refresh for instant data update
        fetchOrders(true);
        
        // Start auto refresh for ongoing updates
        startAutoRefresh();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "📱 Activity paused - stopping auto refresh");
        stopAutoRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister broadcast receiver
        if (orderUpdateReceiver != null) {
            try {
                unregisterReceiver(orderUpdateReceiver);
                Log.d(TAG, "📡 BroadcastReceiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "BroadcastReceiver was not registered: " + e.getMessage());
            }
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
