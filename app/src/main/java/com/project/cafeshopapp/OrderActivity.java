package com.project.cafeshopapp;

import android.os.Bundle;
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
    private TextView tableLabel;
    private RecyclerView orderList;
    private MaterialButton confirmBtn;
    private TextView emptyOrderText;

    private int tableNumber;
    private List<OrderItem> orderItems = new ArrayList<>();
    private OrderAdapter orderAdapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        tableNumber = getIntent().getIntExtra("tableNumber", 1);

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

        tableLabel.setText("Bàn " + tableNumber + " - Đơn hàng");

        confirmBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Đơn bàn " + tableNumber + " đã hoàn thành", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orderItems);
        orderList.setLayoutManager(new LinearLayoutManager(this));
        orderList.setAdapter(orderAdapter);
    }

    private void loadOrdersFromApi() {
        Call<List<OrderItem>> call = apiService.getOrdersByTable("*", "eq." + tableNumber);

        call.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderItems.clear();
                    orderItems.addAll(response.body());

                    if (orderItems.isEmpty()) {
                        emptyOrderText.setVisibility(android.view.View.VISIBLE);
                        orderList.setVisibility(android.view.View.GONE);
                    } else {
                        emptyOrderText.setVisibility(android.view.View.GONE);
                        orderList.setVisibility(android.view.View.VISIBLE);
                        orderAdapter.notifyDataSetChanged();
                    }
                } else {
                    // Hiển thị dữ liệu mẫu nếu không có dữ liệu từ API
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                // Hiển thị dữ liệu mẫu nếu lỗi kết nối
                loadSampleData();
                Toast.makeText(OrderActivity.this, "Không thể tải đơn hàng, hiển thị dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSampleData() {
        orderItems.clear();
        orderItems.add(new OrderItem("Trà sữa", "Ít đường, ít đá", 1));
        orderItems.add(new OrderItem("Cà phê đen", "Không đường", 2));
        orderItems.add(new OrderItem("Bánh croissant", "", 1));

        emptyOrderText.setVisibility(android.view.View.GONE);
        orderList.setVisibility(android.view.View.VISIBLE);
        orderAdapter.notifyDataSetChanged();
    }
}