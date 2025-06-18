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
    private ApiService apiService;    // Background processing
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Broadcast receiver for instant updates
    private BroadcastReceiver orderUpdateReceiver;
    
    // Auto-refresh mechanism (like OrderListActivity)
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 3000; // 3 seconds for real-time updates
    private boolean isRefreshing = false;

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
        tableId = getIntent().getStringExtra("tableId");        initViews();
        setupRecyclerView();
        setupBroadcastReceiver();
        setupAutoRefresh(); // Add auto-refresh like OrderListActivity
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
    }    private void handleOrderCompletion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Complete Orders")
                .setMessage("This will delete all orders for Table " + tableNumber + " and mark the table as available. Continue?")
                .setPositiveButton("Complete", (dialog, which) -> {
                    // Delete all orders for this table and set table to available
                    deleteAllOrdersForTable();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orders, this);
        orderList.setLayoutManager(new LinearLayoutManager(this));
        orderList.setAdapter(orderAdapter);
    }    private void loadOrdersFromApi() {
        if (isRefreshing) {
            Log.d(TAG, "Already refreshing orders for table " + tableNumber + ", skipping...");
            return;
        }
        
        isRefreshing = true;
        Log.d(TAG, "Loading orders for table: " + tableNumber);

        // Use the FRESH API endpoint for Orders with no-cache header (exact database fields)
        String selectFields = "id,tableId,total,status,customerName,customerEmail,customerPhone,note,updatedAt";
        Call<List<Order>> call = apiService.getOrdersByTableFresh("eq.table" + tableNumber, selectFields);        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                isRefreshing = false; // Reset flag
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
                isRefreshing = false; // Reset flag
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
    }    private void deleteAllOrdersForTable() {
        if (orders.isEmpty()) {
            // No orders to delete, just update table to available
            updateTableToAvailable();
            Toast.makeText(this, "No orders to complete", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deleting all orders for table: " + tableNumber);
        
        // 🚀 OPTIMISTIC UI UPDATE - Update UI immediately for instant feedback
        orders.clear();
        updateUI();
        
        // Send ORDER_UPDATE broadcast immediately for instant refresh in OrderListActivity
        Intent orderUpdateIntent = new Intent("ORDER_UPDATE");
        orderUpdateIntent.setPackage(getPackageName());
        sendBroadcast(orderUpdateIntent);
        Log.d(TAG, "📡 ORDER_UPDATE broadcast sent immediately (optimistic)");
          // Show immediate feedback to user
        Toast.makeText(this, "Completing orders for Table " + tableNumber + "...", Toast.LENGTH_SHORT).show();
        
        // Then do the actual API deletion in background
        performActualDeletion();
    }

    private void performActualDeletion() {
        // Do actual API deletion in background - user doesn't need to wait
        String tableIdFilter = "eq.table" + tableNumber;
        Call<Void> bulkDeleteCall = apiService.deleteOrdersByTable(tableIdFilter);
        
        bulkDeleteCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Successfully deleted all orders for table " + tableNumber + " (background)");
                    updateTableToAvailable();
                    
                    // Finish activity after successful deletion
                    mainHandler.postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 500); // Short delay to ensure UI updates
                } else {
                    Log.w(TAG, "Bulk delete failed: " + response.code() + " - " + response.message());
                    // Even if API fails, UI was already updated optimistically
                    // Just finish the activity
                    mainHandler.postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 500);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Bulk delete error: " + t.getMessage());
                // Even if API fails, UI was already updated optimistically
                mainHandler.postDelayed(() -> {
                    setResult(RESULT_OK);
                    finish();
                }, 500);
            }
        });
    }

    private void deleteOrdersIndividually() {
        Log.d(TAG, "Attempting individual deletion of orders");
        
        final int[] deletedCount = { 0 };
        final int totalOrders = orders.size();

        for (Order order : orders) {
            String orderIdFilter = "eq." + order.getId();
            Call<Void> deleteCall = apiService.deleteOrderById(orderIdFilter);

            deleteCall.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    synchronized (deletedCount) {
                        deletedCount[0]++;
                          if (response.isSuccessful()) {
                            Log.d(TAG, "Successfully deleted order: " + order.getId());
                        } else {
                            Log.w(TAG, "Failed to delete order " + order.getId() + ": " + response.code());
                        }

                        if (deletedCount[0] >= totalOrders) {
                            // All deletion attempts completed
                            Log.d(TAG, "All order deletion attempts completed");
                            
                            // Send ORDER_UPDATE broadcast for instant refresh in OrderListActivity
                            Intent orderUpdateIntent = new Intent("ORDER_UPDATE");
                            orderUpdateIntent.setPackage(getPackageName());
                            sendBroadcast(orderUpdateIntent);
                            Log.d(TAG, "📡 ORDER_UPDATE broadcast sent after individual deletes");
                            
                            mainHandler.post(() -> {
                                // First, reload data from database to ensure accuracy
                                Log.d(TAG, "Reloading orders from database after deletion");
                                reloadOrdersAndUpdateTable();
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    synchronized (deletedCount) {
                        deletedCount[0]++;
                        Log.e(TAG, "Failed to delete order " + order.getId() + ": " + t.getMessage());

                        if (deletedCount[0] >= totalOrders) {
                            // All deletion attempts completed with errors
                            mainHandler.post(() -> {
                                Toast.makeText(OrderActivity.this, 
                                    "Some errors occurred while completing orders", 
                                    Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    }
                }
            });
        }
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
                            updateTableToAvailable();                            mainHandler.post(() -> {
                                Toast.makeText(OrderActivity.this,
                                        "Order for table " + tableNumber + " has been completed",
                                        Toast.LENGTH_SHORT).show();
                                
                                // Set result to notify MainActivity to refresh
                                setResult(RESULT_OK);
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
    }    /**
     * Updates the table status to available after all orders are completed
     */    private void updateTableToAvailable() {
        Log.d(TAG, "🔄 UPDATING table " + tableNumber + " status to available in database");
        
        // First, verify table exists by querying database
        String tableIdFilter = "eq." + tableNumber;
        Log.d(TAG, "📊 Checking table existence: GET /manager_table?tableId=" + tableIdFilter);
        
        Call<List<TableModel>> checkCall = apiService.getTableById(tableIdFilter);
        
        checkCall.enqueue(new Callback<List<TableModel>>() {
            @Override            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Table exists in database, get the record ID and update it
                    TableModel existingTable = response.body().get(0);
                    String recordId = existingTable.getId();
                    Log.d(TAG, "✅ DATABASE CONFIRMS: Table " + tableNumber + " exists (ID: " + recordId + ") - updating status");
                    performTableStatusUpdateById(recordId);
                } else {
                    Log.w(TAG, "⚠️ DATABASE SHOWS: Table " + tableNumber + " not found - creating new record");
                    // Table doesn't exist, create it
                    createTableRecord();
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {                Log.e(TAG, "❌ DATABASE CHECK FAILED: " + t.getMessage());
                // Try to update anyway as fallback
                performTableStatusUpdate();
            }
        });
    }      private void performTableStatusUpdateById(String recordId) {
        Log.d(TAG, "📝 UPDATING table status in database using new status-only endpoint...");
        
        TableStatusOnlyRequest updateRequest = new TableStatusOnlyRequest("available");
        String tableIdFilter = "eq." + tableNumber;  // Use tableNumber instead of recordId for consistency
        
        Log.d(TAG, "📊 Database update: PATCH /manager_table?tableId=" + tableIdFilter + " SET status='available' (status-only)");
        
        Call<Void> call = apiService.updateTableStatusOnly(tableIdFilter, updateRequest);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ DATABASE UPDATED: Table " + tableNumber + " status = 'available' (status-only endpoint)");
                    tableStatus = "available";
                    
                    // Update UI immediately to reflect database change
                    mainHandler.post(() -> {
                        updateTableStatusInUI();
                        
                        // Send broadcast to MainActivity to refresh immediately
                        Intent refreshIntent = new Intent("REFRESH_TABLES");
                        refreshIntent.setPackage(getPackageName()); // Make it explicit for security
                        refreshIntent.putExtra("tableNumber", tableNumber);
                        refreshIntent.putExtra("newStatus", "available");
                        sendBroadcast(refreshIntent);
                        
                        Toast.makeText(OrderActivity.this, 
                                "✅ Table " + tableNumber + " is now available", 
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "❌ DATABASE UPDATE FAILED: Table " + tableNumber + " - Response code: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "Error details: " + errorBody);
                        
                        // No more fallback - just report the error
                        mainHandler.post(() -> {
                            Toast.makeText(OrderActivity.this, 
                                    "❌ Failed to update table status: " + response.code(), 
                                    Toast.LENGTH_LONG).show();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "❌ DATABASE CONNECTION ERROR: Table status update failed: " + t.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(OrderActivity.this, 
                            "❌ Connection error. Please try again.", 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }    private void performTableStatusUpdate() {
        Log.d(TAG, "📝 UPDATING table status in database using status-only endpoint...");
        
        TableStatusOnlyRequest updateRequest = new TableStatusOnlyRequest("available");
        String tableIdFilter = "eq." + tableNumber;
        
        Log.d(TAG, "📊 Database update: PATCH /manager_table?tableId=" + tableIdFilter + " SET status='available' (status-only)");
        
        Call<Void> call = apiService.updateTableStatusOnly(tableIdFilter, updateRequest);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ DATABASE UPDATED: Table " + tableNumber + " status = 'available' (status-only endpoint)");
                    tableStatus = "available";
                    
                    // Update UI immediately to reflect database change
                    mainHandler.post(() -> {
                        updateTableStatusInUI();
                        
                        // Send broadcast to MainActivity to refresh immediately
                        Intent refreshIntent = new Intent("REFRESH_TABLES");
                        refreshIntent.setPackage(getPackageName()); // Make it explicit for security
                        refreshIntent.putExtra("tableNumber", tableNumber);
                        refreshIntent.putExtra("newStatus", "available");
                        sendBroadcast(refreshIntent);
                        
                        Toast.makeText(OrderActivity.this, 
                                "✅ Table " + tableNumber + " is now available", 
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "❌ DATABASE UPDATE FAILED: Table " + tableNumber + " - Response code: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "Error details: " + errorBody);
                        
                        mainHandler.post(() -> {
                            Toast.makeText(OrderActivity.this, 
                                    "❌ Failed to update table status: " + response.code(), 
                                    Toast.LENGTH_LONG).show();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body: " + e.getMessage());
                    }
                }
            }            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "❌ DATABASE CONNECTION ERROR: Table status update failed: " + t.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(OrderActivity.this, 
                            "❌ Connection error. Please try again.", 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void createTableRecord() {
        TableModel newTable = new TableModel(tableNumber, "available");
        
        Call<List<TableModel>> createCall = apiService.createTable(newTable);
        createCall.enqueue(new Callback<List<TableModel>>() {
            @Override
            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Table " + tableNumber + " record created successfully");
                    tableStatus = "available";
                } else {
                    Log.e(TAG, "Failed to create table " + tableNumber + " record. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {
                Log.e(TAG, "Table creation failed: " + t.getMessage());
            }        });
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

        call.enqueue(new Callback<List<Order>>() {            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderActivity.this,
                            "Update successful: " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Send ORDER_UPDATE broadcast for instant refresh in OrderListActivity
                    Intent orderUpdateIntent = new Intent("ORDER_UPDATE");
                    orderUpdateIntent.setPackage(getPackageName());
                    sendBroadcast(orderUpdateIntent);
                    Log.d(TAG, "📡 ORDER_UPDATE broadcast sent");

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
    }    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister broadcast receiver
        if (orderUpdateReceiver != null) {
            try {
                unregisterReceiver(orderUpdateReceiver);
                Log.d(TAG, "📡 BroadcastReceiver unregistered in OrderActivity");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "BroadcastReceiver was not registered: " + e.getMessage());
            }
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }/**
     * Reload orders from database after deletion and update table status accordingly
     * This method ALWAYS queries the database to get the real current state
     */
    private void reloadOrdersAndUpdateTable() {
        Log.d(TAG, "🔄 FORCE RELOADING orders from database for table: " + tableNumber);
        
        // IMPORTANT: Clear local data first to force fresh database query
        orders.clear();
        
        // Query database to check REAL current state (not cache)
        String selectFields = "id,tableId,total,status,customerName,customerEmail,customerPhone,note,updatedAt";
        String tableFilter = "eq.table" + tableNumber;
          Log.d(TAG, "📊 Database query: GET /order?tableId=" + tableFilter + "&select=" + selectFields);
        
        Call<List<Order>> call = apiService.getOrdersByTableFresh(tableFilter, selectFields);

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int dbOrderCount = response.body().size();
                    Log.d(TAG, "✅ DATABASE RESULT: " + dbOrderCount + " orders found for table " + tableNumber);

                    // Update local data with REAL database state
                    orders.clear();
                    orders.addAll(response.body());

                    if (orders.isEmpty()) {
                        // DATABASE CONFIRMS: No more orders exist
                        Log.d(TAG, "✅ DATABASE CONFIRMS: Table " + tableNumber + " has NO orders - updating to available");
                        
                        // Update table status in database
                        updateTableToAvailable();
                        
                        // Update UI to reflect database state
                        updateUI();
                        
                        // Show success message
                        Toast.makeText(OrderActivity.this,
                                "✅ All orders completed for Table " + tableNumber,
                                Toast.LENGTH_SHORT).show();
                                
                        // Close activity after showing empty state
                        mainHandler.postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 2000); // 2 second delay to show empty state
                        
                    } else {
                        // DATABASE SHOWS: Still have orders remaining
                        Log.d(TAG, "⚠️ DATABASE SHOWS: Still have " + orders.size() + " orders remaining for table " + tableNumber);
                        updateUI();
                        Toast.makeText(OrderActivity.this,
                                "⚠️ Some orders completed. " + orders.size() + " remaining.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "❌ DATABASE QUERY FAILED: Response code " + response.code());
                    // Fallback: assume database is empty (orders were deleted)
                    orders.clear();
                    updateUI();
                    updateTableToAvailable();
                    
                    Toast.makeText(OrderActivity.this,
                            "Orders completed for Table " + tableNumber + " (Database query failed)",
                            Toast.LENGTH_SHORT).show();
                    
                    mainHandler.postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 1500);
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "❌ DATABASE CONNECTION ERROR: " + t.getMessage());
                // Fallback: assume orders were deleted successfully
                orders.clear();
                updateUI();
                updateTableToAvailable();
                
                Toast.makeText(OrderActivity.this,
                        "Orders completed for Table " + tableNumber + " (Network error)",
                        Toast.LENGTH_SHORT).show();
                
                mainHandler.postDelayed(() -> {
                    setResult(RESULT_OK);
                    finish();
                }, 1500);
            }
        });
    }
    
    /**
     * Update the table status display in the UI
     */
    private void updateTableStatusInUI() {
        if (tableLabel != null) {
            String statusText = "";
            if ("available".equals(tableStatus)) {
                statusText = " (Available)";
            } else if ("reserved".equals(tableStatus)) {
                statusText = " (Reserved)";
            }
            tableLabel.setText("Table " + tableNumber + " - Orders" + statusText);
        }
    }    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 OrderActivity resumed - starting auto-refresh for table " + tableNumber);
        
        // Force refresh orders when activity resumes to get latest data
        loadOrdersFromApi();
        
        // Start auto-refresh for ongoing updates
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "📱 OrderActivity paused - stopping auto-refresh for table " + tableNumber);
        stopAutoRefresh();
    }

    private void setupBroadcastReceiver() {
        orderUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("ORDER_UPDATE".equals(intent.getAction())) {
                    Log.d(TAG, "🔥 Received ORDER_UPDATE broadcast - refreshing orders for table " + tableNumber);
                    loadOrdersFromApi(); // Refresh orders immediately
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("ORDER_UPDATE");
        ContextCompat.registerReceiver(this, orderUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "📡 BroadcastReceiver registered for ORDER_UPDATE in OrderActivity");
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔄 Auto-refreshing orders for table " + tableNumber + "...");
                if (!isRefreshing) {
                    loadOrdersFromApi();
                }
                
                // Schedule next refresh
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    private void startAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            Log.d(TAG, "📡 Auto-refresh started for table " + tableNumber + " (interval: " + REFRESH_INTERVAL + "ms)");
        }
    }

    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            Log.d(TAG, "📡 Auto-refresh stopped for table " + tableNumber);
        }
    }
}