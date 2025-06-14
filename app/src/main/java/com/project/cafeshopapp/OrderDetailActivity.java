package com.project.cafeshopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {
    private static final String TAG = "OrderDetailActivity";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewOrderItems;
    private OrderItemAdapter orderItemAdapter;
    private TextView orderIdText;
    private TextView totalPriceText;
    private TextView statusText;
    private LinearLayout emptyItemsText;
    private MaterialButton backButton;

    private String orderId;
    private ApiService apiService;
    private List<OrderItem> orderItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Get order ID from intent
        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadOrderDetails();
        loadOrderItems();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewOrderItems = findViewById(R.id.recyclerViewOrderItems);
        orderIdText = findViewById(R.id.orderIdText);
        totalPriceText = findViewById(R.id.totalPriceText);
        statusText = findViewById(R.id.statusText);
        emptyItemsText = findViewById(R.id.emptyItemsText);
        backButton = findViewById(R.id.backButton);

        // Set toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Order Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Init API service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Display order ID
        orderIdText.setText("Order ID: " + orderId);
    }

    private void setupRecyclerView() {
        orderItemAdapter = new OrderItemAdapter(orderItems);
        recyclerViewOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrderItems.setAdapter(orderItemAdapter);
    }

    private void loadOrderDetails() {
        Call<List<Order>> call = apiService.getOrderById("eq." + orderId);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Order order = response.body().get(0);
                    updateOrderUI(order);
                } else {
                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());
                    Toast.makeText(OrderDetailActivity.this, "Cannot load order information", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                Toast.makeText(OrderDetailActivity.this, "Connection error when loading order information",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void updateOrderUI(Order order) {
        // Update total price
        totalPriceText.setText(String.format("%.2f€", order.getTotal()));

        // Update status
        statusText.setText(order.getStatus());

        // More UI updates can be added here
    }

    private void loadOrderItems() {
        Log.d(TAG, "Loading order items for order ID: " + orderId);

        // Try a different API approach with direct SQL query via RPC
        // This bypasses row-level security issues
        Call<List<OrderItem>> call;

        if (true) { // Set to true to use the RPC method, false to use the normal method
            // Use RPC for more reliable permission handling
            call = apiService.getOrderItemsViaRPC(orderId);
        } else {
            // Only select the specific columns we need: orderId, title, quantity, price
            String selectFields = "id,orderId,title,quantity,price";

            // Use the new API method with select parameter
            call = apiService.getOrderItemsByOrderIdWithSelect(
                    "eq." + orderId,
                    selectFields);
        }

        // Log the actual URL to help with debugging
        Log.d(TAG, "Request URL: " + call.request().url());

        call.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully loaded " + response.body().size() + " order items");

                    orderItems.clear();
                    orderItems.addAll(response.body());
                    orderItemAdapter.notifyDataSetChanged();

                    if (orderItems.isEmpty()) {
                        emptyItemsText.setVisibility(View.VISIBLE);
                        recyclerViewOrderItems.setVisibility(View.GONE);
                    } else {
                        emptyItemsText.setVisibility(View.GONE);
                        recyclerViewOrderItems.setVisibility(View.VISIBLE);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string()
                                : "No error body";
                        Log.e(TAG, "API Failed: " + response.code() + " - " + response.message());
                        Log.e(TAG, "Error details: " + errorBody);
                        Log.e(TAG, "Request URL: " + call.request().url());
                        Log.e(TAG, "Request headers: " + call.request().headers());

                        // Try to parse the error for more clarity
                        if (errorBody.contains("\"code\"")) {
                            Log.e(TAG,
                                    "Database error code detected. This might be a permission issue or column name mismatch.");
                        }

                        // Display detailed error message
                        String errorMessage = "Error " + response.code() + ": Unable to load order details";
                        Toast.makeText(OrderDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }

                    // If API fails, display sample data to check UI
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                Log.e(TAG, "API Error when calling: " + call.request().url(), t);
                Log.e(TAG, "Error message: " + t.getMessage(), t);
                Toast.makeText(OrderDetailActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();

                // If API fails, display sample data to check UI
                loadSampleData();
            }
        });
    }

    /**
     * Load sample data when API is not working
     */
    private void loadSampleData() {
        Log.d(TAG, "Loading sample order items for order: " + orderId);

        orderItems.clear();

        // Add some sample items
        orderItems.add(new OrderItem(
                "sample1",
                orderId,
                "Iced Milk Coffee",
                2,
                25000.0,
                "L",
                "Less sweet",
                "No sugar"));

        orderItems.add(new OrderItem(
                "sample2",
                orderId,
                "Iced White Coffee",
                1,
                30000.0,
                "M",
                "No cream",
                ""));

        orderItems.add(new OrderItem(
                "sample3",
                orderId,
                "Peach Orange Lemongrass Tea",
                3,
                28000.0,
                "L",
                "",
                "Extra ice"));

        // Update UI
        orderItemAdapter.notifyDataSetChanged();

        if (orderItems.isEmpty()) {
            emptyItemsText.setVisibility(View.VISIBLE);
            recyclerViewOrderItems.setVisibility(View.GONE);
        } else {
            emptyItemsText.setVisibility(View.GONE);
            recyclerViewOrderItems.setVisibility(View.VISIBLE);
        }

        Toast.makeText(this, "Showing sample data", Toast.LENGTH_SHORT).show();
    }
}
