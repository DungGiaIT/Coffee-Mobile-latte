package com.project.cafeshopapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderActivity extends AppCompatActivity {
    private static final String TAG = "OrderActivity";

    private TextView tableLabel;
    private RecyclerView orderList;
    private MaterialButton confirmBtn;
    private TextView emptyOrderText;

    private int tableNumber;
    private String tableStatus;
    private List<OrderItem> orderItems = new ArrayList<>();
    private OrderAdapter orderAdapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Get table info from intent
        tableNumber = getIntent().getIntExtra("tableNumber", 1);
        tableStatus = getIntent().getStringExtra("tableStatus");

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
        Toast.makeText(this, "Đơn bàn " + tableNumber + " đã hoàn thành", Toast.LENGTH_SHORT).show();

        // Update table status back to available if needed
        // updateTableStatus("available");

        finish();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orderItems);
        orderList.setLayoutManager(new LinearLayoutManager(this));
        orderList.setAdapter(orderAdapter);
    }

    private void loadOrdersFromApi() {
        Log.d(TAG, "Loading orders for table: " + tableNumber);

        // FIX: Chỉ truyền 1 parameter như định nghĩa trong ApiService
        Call<List<OrderItem>> call = apiService.getOrdersByTable("eq." + tableNumber);

        call.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Success: Received " + response.body().size() + " orders");

                    orderItems.clear();
                    orderItems.addAll(response.body());

                    updateUI();
                } else {
                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());
                    // Hiển thị dữ liệu mẫu nếu không có dữ liệu từ API
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                // Hiển thị dữ liệu mẫu nếu lỗi kết nối
                loadSampleData();
                Toast.makeText(OrderActivity.this, "Không thể tải đơn hàng, hiển thị dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (orderItems.isEmpty()) {
            emptyOrderText.setVisibility(android.view.View.VISIBLE);
            orderList.setVisibility(android.view.View.GONE);
            emptyOrderText.setText("Bàn " + tableNumber + " chưa có đơn hàng nào");
        } else {
            emptyOrderText.setVisibility(android.view.View.GONE);
            orderList.setVisibility(android.view.View.VISIBLE);
            orderAdapter.notifyDataSetChanged();
        }
    }

    private void loadSampleData() {
        Log.d(TAG, "Loading sample order data for table: " + tableNumber);

        orderItems.clear();

        // Sample data based on table number for variety
        switch (tableNumber % 3) {
            case 1:
                orderItems.add(new OrderItem("Trà sữa", "Ít đường, ít đá", 1));
                orderItems.add(new OrderItem("Cà phê đen", "Không đường", 2));
                orderItems.add(new OrderItem("Bánh croissant", "", 1));
                break;
            case 2:
                orderItems.add(new OrderItem("Cappuccino", "Size L", 1));
                orderItems.add(new OrderItem("Bánh mì", "Thêm phô mai", 2));
                break;
            case 0:
                orderItems.add(new OrderItem("Americano", "Đá riêng", 1));
                orderItems.add(new OrderItem("Bánh cookie", "", 3));
                orderItems.add(new OrderItem("Nước cam", "Không đá", 1));
                break;
        }

        updateUI();
        Toast.makeText(this, "Hiển thị dữ liệu mẫu cho bàn " + tableNumber, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // Return to previous activity
        super.onBackPressed();
    }
}