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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderActivity extends AppCompatActivity implements OrderAdapter.OrderClickListener {
    private static final String TAG = "OrderActivity";

    private TextView tableLabel;
    private RecyclerView orderList;
    private MaterialButton confirmBtn;
    private TextView emptyOrderText;

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

        // Display table info with status
        String statusText = tableStatus != null ? " (" + tableStatus + ")" : "";
        tableLabel.setText("Bàn " + tableNumber + " - Đơn hàng" + statusText);

        confirmBtn.setOnClickListener(v -> {
            handleOrderCompletion();
        });
    }

    private void handleOrderCompletion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận")
                .setMessage("Đánh dấu đơn hàng đã hoàn thành?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    // Update all orders for this table to COMPLETED
                    updateAllOrdersStatus("COMPLETED");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orders, this);
        orderList.setLayoutManager(new LinearLayoutManager(this));
        orderList.setAdapter(orderAdapter);
    }

    private void loadOrdersFromApi() {
        Log.d(TAG, "Loading orders for table: " + tableNumber);

        // Use the new API endpoint for Orders
        Call<List<Order>> call = apiService.getOrdersByTable("eq.table" + tableNumber);

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
                Toast.makeText(OrderActivity.this, "Không thể tải đơn hàng, hiển thị dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (orders.isEmpty()) {
            emptyOrderText.setVisibility(View.VISIBLE);
            orderList.setVisibility(View.GONE);
            emptyOrderText.setText("Bàn " + tableNumber + " chưa có đơn hàng nào");
        } else {
            emptyOrderText.setVisibility(View.GONE);
            orderList.setVisibility(View.VISIBLE);
            orderAdapter.notifyDataSetChanged();
        }
    }

    private void loadSampleData() {
        Log.d(TAG, "Loading sample order data for table: " + tableNumber);

        orders.clear();

        // Sample data based on the screenshot
        orders.add(new Order("cmbq9wn1n0000lc04r92w06f0", "table" + tableNumber, 13.09, "PENDING", "PICKUP", null));
        orders.add(new Order("cmbqmmbzd000jy04tcrpr459", "table" + tableNumber, 5.39, "PENDING", "PICKUP", null));

        if (tableNumber % 3 == 0) {
            orders.add(new Order("cmbr5e6x30002o9csji0mg9h", "table" + tableNumber, 6.49, "PENDING", "DELIVERY", "2315"));
        }

        updateUI();
        Toast.makeText(this, "Hiển thị dữ liệu mẫu cho bàn " + tableNumber, Toast.LENGTH_SHORT).show();
    }

    private void updateAllOrdersStatus(String newStatus) {
        if (orders.isEmpty()) {
            Toast.makeText(this, "Không có đơn hàng nào để cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }

        // Count for tracking completion
        final int[] completedCount = {0};
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
                            mainHandler.post(() -> {
                                Toast.makeText(OrderActivity.this, "Đơn bàn " + tableNumber + " đã hoàn thành", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(OrderActivity.this, "Xảy ra lỗi khi cập nhật đơn hàng", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onViewDetailsClick(Order order) {
        Toast.makeText(this, "Chi tiết đơn hàng " + order.getId(), Toast.LENGTH_SHORT).show();
        // TODO: Navigate to order details screen
    }

    @Override
    public void onUpdateStatusClick(Order order) {
        String[] statuses = {"PENDING", "PROCESSING", "COMPLETED", "CANCELLED"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật trạng thái")
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
                            "Cập nhật thành công: " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Refresh orders
                    loadOrdersFromApi();
                } else {
                    Toast.makeText(OrderActivity.this,
                            "Không thể cập nhật (Lỗi: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(OrderActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
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