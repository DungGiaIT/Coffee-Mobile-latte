package com.project.cafeshopapp;

import android.content.Intent;
import android.os.Bundle;
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
    }

    private void initViews() {
        gridView = findViewById(R.id.gridView);
        btnLogout = findViewById(R.id.btnLogout);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(TableActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupAdapter() {
        adapter = new TableAdapter(this, tableList);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            TableModel selectedTable = tableList.get(position);
            int tableId = selectedTable.getTableId();

            // Cập nhật trạng thái bàn thành "serving"
            TableModel updatedTable = new TableModel();
            updatedTable.setStatus("serving");

            Call<List<TableModel>> updateCall = apiService.updateTableStatus("eq." + tableId, updatedTable);
            updateCall.enqueue(new Callback<List<TableModel>>() {
                @Override
                public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(TableActivity.this, "Bàn #" + tableId + " đang phục vụ", Toast.LENGTH_SHORT).show();

                        // Cập nhật UI
                        tableList.get(position).setStatus("serving");
                        adapter.notifyDataSetChanged();

                        // Chuyển sang OrderActivity
                        Intent intent = new Intent(TableActivity.this, OrderActivity.class);
                        intent.putExtra("tableNumber", tableId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(TableActivity.this, "Không thể cập nhật bàn", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<TableModel>> call, Throwable t) {
                    Toast.makeText(TableActivity.this, "Lỗi kết nối khi cập nhật bàn", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchTablesFromApi() {
        Call<List<TableModel>> call = apiService.getTables("*");

        call.enqueue(new Callback<List<TableModel>>() {
            @Override
            public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tableList.clear();
                    tableList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(TableActivity.this, "Không tải được dữ liệu bàn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TableModel>> call, Throwable t) {
                Toast.makeText(TableActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh dữ liệu khi quay lại activity
        fetchTablesFromApi();
    }
}