package com.project.cafeshopapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ScrollView staffInfoLayout;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerViewTables;
    private TextView staffNameTextView;
    private TextView staffLoginTimeTextView;
    private MaterialButton btnBack;

    private List<TableModel> tableList = new ArrayList<>();
    private TableRecyclerAdapter tableAdapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadStaffInfo();
        fetchTablesFromApi();
    }

    private void initViews() {
        staffInfoLayout = findViewById(R.id.staffInfoLayout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        recyclerViewTables = findViewById(R.id.recyclerViewTables);
        staffNameTextView = findViewById(R.id.staffNameTextView);
        staffLoginTimeTextView = findViewById(R.id.staffLoginTimeTextView);
        btnBack = findViewById(R.id.btnBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnBack.setOnClickListener(v -> staffInfoLayout.setVisibility(View.GONE));
    }

    private void setupRecyclerView() {
        // Create adapter with click listener
        tableAdapter = new TableRecyclerAdapter(tableList, this::handleTableClick);

        recyclerViewTables.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewTables.setAdapter(tableAdapter);
    }

    private void handleTableClick(TableModel table) {
        if (table != null) {
            // Show toast with table info
            Toast.makeText(this, "Đã chọn " + "Bàn " + table.getTableId(), Toast.LENGTH_SHORT).show();

            // Navigate to TableActivity or OrderActivity based on table status
            Intent intent;
            if ("empty".equals(table.getStatus())) {
                intent = new Intent(MainActivity.this, TableActivity.class);
            } else {
                intent = new Intent(MainActivity.this, OrderActivity.class);
                intent.putExtra("tableNumber", table.getTableId());
            }
            startActivity(intent);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                staffInfoLayout.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_order) {
                // Handle order click
                Toast.makeText(this, "Đơn hàng", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_notify) {
                // Handle notification click
                Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                staffInfoLayout.setVisibility(View.VISIBLE);
                return true;
            }

            return false;
        });
    }

    private void loadStaffInfo() {
        SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
        String staffName = prefs.getString("staff_name", "Chưa đăng nhập");
        String staffPosition = prefs.getString("staff_position", "");
        String staffCode = prefs.getString("staff_code", "");
        long loginTime = prefs.getLong("login_time", 0);

        // Format staff info
        StringBuilder staffInfo = new StringBuilder();
        staffInfo.append("Tên: ").append(staffName).append("\n");
        staffInfo.append("Chức vụ: ").append(staffPosition).append("\n");
        staffInfo.append("Mã NV: ").append(staffCode);

        staffNameTextView.setText(staffInfo.toString());

        // Format login time
        if (loginTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            String formattedTime = sdf.format(new Date(loginTime));
            staffLoginTimeTextView.setText("Giờ đăng nhập: " + formattedTime);
        } else {
            staffLoginTimeTextView.setText("Giờ đăng nhập: Không xác định");
        }
    }

    private void fetchTablesFromApi() {
        Call<List<TableModel>> call = apiService.getTables("*");

        call.enqueue(new Callback<List<TableModel>>() {
            @Override
            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tableAdapter.updateData(response.body());
                } else {
                    // Load sample data if API fails
                    loadSampleTables();
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {
                // Load sample data if API fails
                loadSampleTables();
                Toast.makeText(MainActivity.this, "Không thể tải dữ liệu bàn, hiển thị dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSampleTables() {
        List<TableModel> sampleTables = new ArrayList<>();

        // Add sample tables with different statuses
        String[] statuses = {"empty", "serving", "waiting", "paid"};
        for (int i = 1; i <= 8; i++) {
            TableModel table = new TableModel();
            table.setTableId(i);
            table.setStatus(statuses[(i - 1) % statuses.length]);
            sampleTables.add(table);
        }

        tableAdapter.updateData(sampleTables);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tables when returning to this activity
        fetchTablesFromApi();
    }
}