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
        Log.d(TAG, "🔄 Fetching orders from API...");
        swipeRefreshLayout.setRefreshing(true);

        // 🛠️ THỬ VỚI SELECT PARAMETER ĐỂ GIẢM DỮ LIỆU TẢI VỀ
        String selectFields = "id,tableId,total,status,deliveryMethod,deliveryAddress,customerName,customerPhone,note,createdAt";
        Call<List<Order>> call = apiService.getAllOrdersWithSelect(selectFields);

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                swipeRefreshLayout.setRefreshing(false);

                Log.d(TAG, "📊 API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Fetched " + response.body().size() + " orders successfully");

                    // Process on background thread
                    processOrders(response.body());
                } else {
                    // 🔍 LOG CHI TIẾT LỖI
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

                    handleApiError("Không thể tải đơn hàng (Lỗi: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "🌐 Network Error: " + t.getMessage(), t);
                handleApiError("Lỗi kết nối: " + t.getMessage());
            }
        });

        // 🔄 FALLBACK: THỬ VỚI API CALL ĐƠN GIẢN HƠN NÊU CÓ LỖI
        if (!swipeRefreshLayout.isRefreshing()) {
            trySimpleFetch();
        }
    }

    private void trySimpleFetch() {
        Log.d(TAG, "🔄 Trying simple fetch as fallback...");

        Call<List<Order>> simplCall = apiService.getAllOrders();
        simplCall.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Simple fetch successful: " + response.body().size() + " orders");
                    processOrders(response.body());
                } else {
                    Log.e(TAG, "❌ Simple fetch also failed: " + response.code());
                    handleApiError("API không thể truy cập. Hiển thị dữ liệu mẫu.");
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "❌ Simple fetch network error: " + t.getMessage());
                handleApiError("Lỗi kết nối. Hiển thị dữ liệu mẫu.");
            }
        });
    }

    private void processOrders(List<Order> orders) {
        executorService.execute(() -> {
            try {
                // Log sample data for debugging
                if (!orders.isEmpty()) {
                    Order firstOrder = orders.get(0);
                    Log.d(TAG, "📋 Sample order: " + firstOrder.getId() +
                            " - Table: " + firstOrder.getTableId() +
                            " - Total: " + firstOrder.getTotal() +
                            " - Status: " + firstOrder.getStatus());
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    orderList.clear();
                    orderList.addAll(orders);
                    orderAdapter.notifyDataSetChanged();

                    updateEmptyViewVisibility();

                    Toast.makeText(OrderListActivity.this,
                            "✅ Tải " + orders.size() + " đơn hàng thành công",
                            Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
        Log.d(TAG, "📋 Loading sample data based on database structure...");

        // Create sample orders based on the database table in the screenshot
        orderList.clear();

        // Add sample orders matching the structure in screenshot - với đúng cấu trúc database
        orderList.add(createSampleOrder("cmbq9wn1n0000lc04r92w06f0", "table1", 13.09, "PENDING", "PICKUP", "Khanh Le"));
        orderList.add(createSampleOrder("cmbqmmbzd000jy04tcrpr459", "table2", 5.39, "PENDING", "PICKUP", "Khanh Le"));
        orderList.add(createSampleOrder("cmbqskpiw0000vctw1mw0dqiy", "table5", 43.67, "PENDING", "PICKUP", "Lê Bảo Khanh"));
        orderList.add(createSampleOrder("cmbr5tue00000o9cswu6rvdfz", "table1", 28.501, "PENDING", "PICKUP", "Nguyen Tien Dung"));
        orderList.add(createSampleOrder("cmbr5e6x30002o9csji0mg9h", null, 6.49, "PENDING", "DELIVERY", "Nguyen Tien Dung"));
        orderList.add(createSampleOrder("cmbr5ql9k0000l404rxg5y1b6", "table1", 21.89, "PENDING", "PICKUP", "du"));

        orderAdapter.notifyDataSetChanged();
        Toast.makeText(this, "📋 Hiển thị dữ liệu mẫu (6 đơn hàng)", Toast.LENGTH_SHORT).show();

        updateEmptyViewVisibility();
    }

    private Order createSampleOrder(String id, String tableId, double total, String status, String deliveryMethod, String customerName) {
        Order order = new Order();
        order.setId(id);
        order.setTableId(tableId);
        order.setTotal(total);
        order.setStatus(status);
        order.setDeliveryMethod(deliveryMethod);
        order.setCustomerName(customerName);
        order.setCreatedAt("2025-06-11");
        return order;
    }

    @Override
    public void onViewDetailsClick(Order order) {
        // Navigate to order details screen
        String customerInfo = order.getCustomerName() != null ?
                "Khách: " + order.getCustomerName() : "Khách: Không có thông tin";
        Toast.makeText(this, "Chi tiết đơn hàng " + order.getId() + "\n" + customerInfo, Toast.LENGTH_LONG).show();
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
                .setTitle("Cập nhật trạng thái đơn hàng")
                .setMessage("Đơn hàng: " + order.getId())
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
                            "✅ Cập nhật trạng thái thành " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Refresh data
                    fetchOrders();
                } else {
                    Log.e(TAG, "Failed to update order status. Response code: " + response.code());
                    Toast.makeText(OrderListActivity.this,
                            "❌ Không thể cập nhật trạng thái (Lỗi: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "Error updating order status: " + t.getMessage(), t);
                Toast.makeText(OrderListActivity.this,
                        "❌ Lỗi kết nối khi cập nhật trạng thái",
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