package com.project.cafeshopapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TableActivity extends AppCompatActivity {
    private static final String TAG = "TableActivity";

    private GridView gridView;
    private ImageButton btnLogout;
    private List<TableModel> tableList = new ArrayList<>();
    private TableAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        initViews();
        setupAdapter();
        fetchTablesFromApi();

        // Log that we're implementing the new feature
        Log.d(TAG, "Implementing table availability feature - tables without orders will be marked as available");
    }

    private void initViews() {
        gridView = findViewById(R.id.gridView);
        btnLogout = findViewById(R.id.btnLogout);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnLogout.setOnClickListener(v -> {
            // Clear login info
            clearLoginInfo();

            Intent intent = new Intent(TableActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void clearLoginInfo() {
        SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void setupAdapter() {
        adapter = new TableAdapter(this, tableList);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < tableList.size()) {
                TableModel selectedTable = tableList.get(position);
                int tableId = selectedTable.getTableId();

                Log.d(TAG, "Table clicked: " + tableId + " - Current status: " + selectedTable.getStatus());

                // Check if there are orders for this table
                checkTableOrders(selectedTable, position);
            }
        });
    }

    private void checkTableOrders(TableModel selectedTable, int position) {
        int tableId = selectedTable.getTableId();        String tableIdParam = "eq.table" + tableId;

        // Get orders for this table with select parameter
        String selectFields = "id,tableId,total,status,customerName,customerEmail,customerPhone,note,createdAt";
        Call<List<Order>> call = apiService.getOrdersByTableWithSelect(tableIdParam, selectFields);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Order> orders = response.body();
                    Log.d(TAG, "Found " + orders.size() + " orders for table " + tableId);

                    // If no orders, table should be available
                    String newStatus = orders.isEmpty() ? "available" : "reserved";
                    Log.d(TAG, "Setting table " + tableId + " status to " + newStatus + " based on " + orders.size()
                            + " orders");
                    updateTableStatus(tableId, newStatus, position);
                } else {
                    // In case of API error, default to reserved
                    Log.w(TAG, "Failed to check orders, setting to reserved by default");
                    updateTableStatus(tableId, "reserved", position);
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                // In case of connection error, default to reserved
                updateTableStatus(tableId, "reserved", position);
            }
        });
    }    private void updateTableStatus(int tableId, String newStatus, int position) {
        // Create update request
        TableUpdateRequest updateRequest = new TableUpdateRequest(newStatus);

        Call<Void> updateCall = apiService.updateTableStatusOnly("eq." + tableId, updateRequest);
        updateCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Table " + tableId + " status updated to " + newStatus + " (status-only endpoint)");
                    Toast.makeText(TableActivity.this, "Table #" + tableId + " has been updated", Toast.LENGTH_SHORT)
                            .show();

                    // Update local data
                    if (position < tableList.size()) {
                        tableList.get(position).setStatus(newStatus);
                        adapter.notifyDataSetChanged();
                    }

                    // Navigate to OrderActivity
                    Intent intent = new Intent(TableActivity.this, OrderActivity.class);
                    intent.putExtra("tableNumber", tableId);
                    intent.putExtra("tableStatus", newStatus);
                    startActivity(intent);                } else {
                    Log.e(TAG, "Failed to update table status. Response code: " + response.code());
                    Toast.makeText(TableActivity.this, "Cannot update table", Toast.LENGTH_SHORT).show();

                    // Log error details
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string()
                                : "No error body";
                        Log.e(TAG, "Error response: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error updating table status: " + t.getMessage(), t);
                Toast.makeText(TableActivity.this, "Connection error when updating table", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTablesFromApi() {
        Log.d(TAG, "Fetching tables from API...");

        Call<List<TableModel>> call = apiService.getAllTables();

        call.enqueue(new Callback<List<TableModel>>() {
            @Override
            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Success: Received " + response.body().size() + " tables");

                    tableList.clear();
                    tableList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    // Check orders for each table and update their status accordingly
                    checkAllTablesOrders();

                    // Log table data for debugging
                    for (TableModel table : response.body()) {
                        Log.d(TAG, "Table ID: " + table.getTableId() + ", Status: " + table.getStatus());
                    }
                } else {
                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());
                    Toast.makeText(TableActivity.this, "Could not load table data", Toast.LENGTH_SHORT).show();

                    // Load sample data as fallback
                    loadSampleTables();
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                Toast.makeText(TableActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();

                // Load sample data as fallback
                loadSampleTables();
            }
        });
    }

    private void loadSampleTables() {
        Log.d(TAG, "Loading sample tables...");

        tableList.clear();

        // Sample data - tables 3, 4, 7, and 8 are available (have no orders)
        String[] statuses = { "reserved", "reserved", "available", "available", "reserved", "reserved", "available",
                "available" };
        String[] ids = { "table1", "table2", "table3", "table4", "table5", "table6", "table7", "table8" };

        for (int i = 1; i <= 8; i++) {
            TableModel table = new TableModel();
            table.setTableId(i);
            table.setStatus(statuses[i - 1]);
            table.setId(ids[i - 1]);
            tableList.add(table);
        }

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Displaying sample data", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        fetchTablesFromApi();
    }    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to MainActivity instead of closing
        Intent intent = new Intent(TableActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Checks orders for all tables and updates their status
     * Tables with no orders should be marked as "available"
     */
    private void checkAllTablesOrders() {
        Log.d(TAG, "Checking orders for all tables to mark tables without orders as 'available'...");

        for (int i = 0; i < tableList.size(); i++) {
            TableModel table = tableList.get(i);
            int tableId = table.getTableId();
            final int position = i;            String tableIdParam = "eq.table" + tableId;
            String selectFields = "id,tableId,total,status";  // Only need minimal fields for checking
            Call<List<Order>> call = apiService.getOrdersByTableWithSelect(tableIdParam, selectFields);

            call.enqueue(new Callback<List<Order>>() {
                @Override
                public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Order> orders = response.body();
                        String newStatus = orders.isEmpty() ? "available" : "reserved";

                        Log.d(TAG, "Table " + tableId + ": Found " + orders.size() + " orders -> Status should be "
                                + newStatus);

                        if (!newStatus.equals(table.getStatus())) {
                            Log.d(TAG, "Table " + tableId + " status updated from " +
                                    table.getStatus() + " to " + newStatus + " based on " + orders.size() + " orders");

                            // Update the status in the model
                            table.setStatus(newStatus);

                            // Update UI
                            adapter.notifyDataSetChanged();

                            // Also update in database (if needed)
                            updateTableStatus(tableId, newStatus, position);
                        } else {
                            Log.d(TAG, "Table " + tableId + " status already correct: " + table.getStatus());
                        }
                    } else {
                        Log.w(TAG, "Failed to check orders for table " + tableId + ": " +
                                response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<List<Order>> call, Throwable t) {
                    Log.e(TAG, "Error checking orders for table " + tableId + ": " + t.getMessage());
                }
            });
        }
    }
}