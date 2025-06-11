package com.project.cafeshopapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OrderClickListener {

    private static final String TAG = "OrderListActivity";

    // UI components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;

    // Data
    private List<Order> orderList = new ArrayList<>();
    private OrderAdapter orderAdapter;
    private ApiService apiService;

    // Background processing
    private ExecutorService executorService;
    private Handler mainHandler;

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

        // Load orders
        fetchOrders();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewOrders);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
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
                android.R.color.holo_red_light
        );
    }

    private void fetchOrders() {
        swipeRefreshLayout.setRefreshing(true);

        Call<List<Order>> call = apiService.getAllOrders();
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched " + response.body().size() + " orders");

                    // Process on background thread
                    processOrders(response.body());
                } else {
                    // API call failed
                    Log.e(TAG, "Failed to fetch orders: " + response.code());
                    handleApiError("Không thể tải đơn hàng (Lỗi: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Error fetching orders: " + t.getMessage(), t);
                handleApiError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void processOrders(List<Order> orders) {
        executorService.execute(() -> {
            try {
                // Process data in background if needed

                // Update UI on main thread
                mainHandler.post(() -> {
                    orderList.clear();
                    orderList.addAll(orders);
                    orderAdapter.notifyDataSetChanged();

                    updateEmptyViewVisibility();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing orders: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(OrderListActivity.this,
                                "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleApiError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        loadSampleData();
    }

    private void updateEmptyViewVisibility() {
        if (orderList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void loadSampleData() {
        // Create sample orders based on the database table in the screenshot
        orderList.clear();

        // Add sample orders matching the structure in screenshot
        orderList.add(new Order("cmbq9wn1n0000lc04r92w06f0", "table1", 13.09, "PENDING", "PICKUP", null));
        orderList.add(new Order("cmbqmmbzd000jy04tc rpr459", "table2", 5.39, "PENDING", "PICKUP", null));
        orderList.add(new Order("cmbqskpiw0000vctw1mw0dqiy", "table5", 43.67, "PENDING", "PICKUP", null));
        orderList.add(new Order("cmbr5tue00000o9cswu6rvdfz", "table1", 28.501, "PENDING", "PICKUP", null));
        orderList.add(new Order("cmbr5e6x30002o9csji0mg9h", "NULL", 6.49, "PENDING", "DELIVERY", null));
        orderList.add(new Order("cmbr5ql9k0000l404rxg5y1b6", "table1", 21.89, "PENDING", "PICKUP", null));

        orderAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Hiển thị dữ liệu mẫu", Toast.LENGTH_SHORT).show();

        updateEmptyViewVisibility();
    }

    @Override
    public void onViewDetailsClick(Order order) {
        // Navigate to order details screen
        Toast.makeText(this, "Chi tiết đơn hàng " + order.getId(), Toast.LENGTH_SHORT).show();
        // TODO: Create OrderDetailsActivity and start it here
    }

    @Override
    public void onUpdateStatusClick(Order order) {
        // Show dialog to update status
        showStatusUpdateDialog(order);
    }

    private void showStatusUpdateDialog(Order order) {
        String[] statuses = {"PENDING", "PROCESSING", "COMPLETED", "CANCELLED"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật trạng thái")
                .setItems(statuses, (dialog, which) -> {
                    updateOrderStatus(order.getId(), statuses[which]);
                })
                .show();
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        OrderStatusUpdate statusUpdate = new OrderStatusUpdate(newStatus);

        Call<List<Order>> call = apiService.updateOrderStatus("eq." + orderId, statusUpdate);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Update successful
                    Toast.makeText(OrderListActivity.this,
                            "Cập nhật trạng thái thành " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Refresh data
                    fetchOrders();
                } else {
                    Log.e(TAG, "Failed to update order status. Response code: " + response.code());
                    Toast.makeText(OrderListActivity.this,
                            "Không thể cập nhật trạng thái (Lỗi: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "Error updating order status: " + t.getMessage(), t);
                Toast.makeText(OrderListActivity.this,
                        "Lỗi kết nối khi cập nhật trạng thái",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}