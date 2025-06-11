package com.project.cafeshopapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REFRESH_INTERVAL = 30000; // 30 seconds auto-refresh
    private static final int FAST_REFRESH_INTERVAL = 10000; // 10 seconds for active mode

    // API Configuration
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4";
    private static final String BASE_URL = "https://ufgxsicqlaraqaeziohf.supabase.co/rest/v1/";

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView staffInfoLayout;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerViewTables;
    private TextView staffNameTextView;
    private TextView staffLoginTimeTextView;
    private MaterialButton btnBack;
    private MaterialButton btnRefresh;
    private FloatingActionButton fabRefresh;
    private TextView lastUpdateTextView;

    // Data & API
    private List<TableModel> tableList = new ArrayList<>();
    private TableRecyclerAdapter tableAdapter;
    private ApiService apiService;

    // Auto-refresh & Background Threading
    private Handler refreshHandler;
    private Handler mainHandler;
    private Runnable refreshRunnable;
    private ExecutorService executorService;
    private boolean isRefreshing = false;
    private boolean isActiveMode = false; // Fast refresh when user is actively using app
    private boolean apiKeyValidated = false; // Track API key validation status

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
        ApiClient.initCache(new File(getCacheDir(), "api-cache"));

        initBackgroundThreading();
        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupAutoRefresh();
        setupSwipeRefresh();

        // Load staff info in background to avoid blocking main thread
        loadStaffInfoAsync();

        // Test API connection first, then fetch data
        testApiConnectionAndFetchData();
    }

    private void initBackgroundThreading() {
        // Create thread pool for background operations
        executorService = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
        refreshHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        // Find views efficiently
        findViewsById();

        // Initialize API service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Setup click listeners
        setupClickListeners();
    }

    private void findViewsById() {
        // Batch findViewById calls to reduce overhead
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        staffInfoLayout = findViewById(R.id.staffInfoLayout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        recyclerViewTables = findViewById(R.id.recyclerViewTables);
        staffNameTextView = findViewById(R.id.staffNameTextView);
        staffLoginTimeTextView = findViewById(R.id.staffLoginTimeTextView);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        fabRefresh = findViewById(R.id.fabRefresh);
        lastUpdateTextView = findViewById(R.id.lastUpdateTextView);
    }

    private void setupClickListeners() {
        // Setup click listeners without heavy operations
        btnBack.setOnClickListener(v -> staffInfoLayout.setVisibility(View.GONE));

        btnRefresh.setOnClickListener(v -> triggerManualRefresh());

        if (fabRefresh != null) {
            fabRefresh.setOnClickListener(v -> triggerManualRefresh());
        }
    }

    // 🧪 KIỂM TRA API KEY
    private void testApiConnectionAndFetchData() {
        Log.d(TAG, "🧪 Testing API connection and key validation...");

        // Show initial loading message
        mainHandler.post(() -> {
            if (lastUpdateTextView != null) {
                lastUpdateTextView.setText("🔍 Đang kiểm tra kết nối API...");
            }
            Toast.makeText(this, "🧪 Kiểm tra API key...", Toast.LENGTH_SHORT).show();
        });

        testApiConnection();
    }

    private void testApiConnection() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "🔗 Creating HTTP client for API test...");

                // 🔍 DEBUG: KIỂM TRA KEY TYPE
                Log.d(TAG, "🔑 API Key first 50 chars: " + API_KEY.substring(0, Math.min(50, API_KEY.length())));

                // Decode JWT payload để xem role
                try {
                    String[] parts = API_KEY.split("\\.");
                    if (parts.length > 1) {
                        byte[] decodedBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE);
                        String payload = new String(decodedBytes);
                        Log.d(TAG, "🎫 JWT Payload: " + payload);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding JWT: " + e.getMessage());
                }

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "manager_table?limit=1")
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json")
                        .build();

                Log.d(TAG, "📡 Sending API test request to: " + request.url());
                Log.d(TAG, "🔑 Using API key: " + API_KEY.substring(0, 20) + "...");

                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                        handleApiTestResponse(response);
                    }

                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        handleApiTestFailure(e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error setting up API test: " + e.getMessage(), e);
                handleApiTestFailure(e);
            }
        });
    }

    private void handleApiTestResponse(okhttp3.Response response) {
        executorService.execute(() -> {
            try {
                int responseCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "No body";

                Log.d(TAG, "📊 API Test Response Code: " + responseCode);
                Log.d(TAG, "📄 API Test Response Body: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

                mainHandler.post(() -> {
                    if (responseCode == 200) {
                        // API key is valid
                        apiKeyValidated = true;
                        Log.d(TAG, "✅ API Key validation successful!");

                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("✅ API kết nối thành công - " + getCurrentTime());
                        }

                        Toast.makeText(this, "✅ API key hợp lệ! Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();

                        // Now fetch the actual data
                        fetchTablesFromApi();

                    } else if (responseCode == 401) {
                        // Unauthorized - API key invalid
                        apiKeyValidated = false;
                        Log.e(TAG, "🔐 API Key validation failed - Unauthorized (401)");

                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("❌ API key không hợp lệ - " + getCurrentTime());
                        }

                        Toast.makeText(this, "❌ API key không hợp lệ! Sử dụng dữ liệu mẫu.", Toast.LENGTH_LONG).show();
                        loadSampleTablesAsync();

                    } else if (responseCode == 403) {
                        // Forbidden - Permission denied
                        apiKeyValidated = false;
                        Log.e(TAG, "🚫 API Key validation failed - Forbidden (403)");

                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("🚫 Không có quyền truy cập - " + getCurrentTime());
                        }

                        Toast.makeText(this, "🚫 Không có quyền truy cập database! Sử dụng dữ liệu mẫu.", Toast.LENGTH_LONG).show();
                        loadSampleTablesAsync();

                    } else {
                        // Other error codes
                        apiKeyValidated = false;
                        Log.w(TAG, "⚠️ API Test returned unexpected code: " + responseCode);

                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("⚠️ Lỗi API (" + responseCode + ") - " + getCurrentTime());
                        }

                        Toast.makeText(this, "⚠️ Lỗi API (" + responseCode + ")! Sử dụng dữ liệu mẫu.", Toast.LENGTH_SHORT).show();
                        loadSampleTablesAsync();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error processing API test response: " + e.getMessage(), e);
                handleApiTestFailure(e);
            }
        });
    }

    private void handleApiTestFailure(Exception e) {
        Log.e(TAG, "🌐 API Test Failed: " + e.getMessage(), e);

        mainHandler.post(() -> {
            apiKeyValidated = false;

            if (lastUpdateTextView != null) {
                lastUpdateTextView.setText("🌐 Lỗi kết nối API - " + getCurrentTime());
            }

            String errorMessage = "🌐 Không thể kết nối API";
            if (e instanceof java.net.SocketTimeoutException) {
                errorMessage += " (Timeout)";
            } else if (e instanceof java.net.UnknownHostException) {
                errorMessage += " (DNS Error)";
            } else if (e instanceof java.net.ConnectException) {
                errorMessage += " (Connection Error)";
            }
            errorMessage += "! Sử dụng dữ liệu mẫu.";

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            loadSampleTablesAsync();
        });
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull-to-refresh triggered");
            isActiveMode = true; // Enable fast refresh mode

            if (apiKeyValidated) {
                fetchTablesFromApi();
            } else {
                // Retest API connection if key was invalid
                testApiConnectionAndFetchData();
            }
        });

        // Customize refresh colors
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRefreshing) {
                    Log.d(TAG, "Auto-refreshing table data... (Active mode: " + isActiveMode + ", API Valid: " + apiKeyValidated + ")");

                    if (apiKeyValidated) {
                        fetchTablesFromApi();
                    } else {
                        // Periodically retest API connection
                        Log.d(TAG, "🔄 API key not validated, retesting connection...");
                        testApiConnection();
                    }
                }

                // Schedule next refresh
                int interval = isActiveMode ? FAST_REFRESH_INTERVAL : REFRESH_INTERVAL;
                refreshHandler.postDelayed(this, interval);
            }
        };
    }

    private void triggerManualRefresh() {
        isActiveMode = true; // Enable fast refresh for next few minutes

        // Show toast on main thread
        mainHandler.post(() ->
                Toast.makeText(this, "🔄 Đang cập nhật trạng thái bàn...", Toast.LENGTH_SHORT).show()
        );

        if (apiKeyValidated) {
            fetchTablesFromApi();
        } else {
            // Retest API connection if key was invalid
            Log.d(TAG, "🔄 Manual refresh triggered, but API key not validated. Retesting...");
            testApiConnectionAndFetchData();
        }

        // Reset to normal mode after 2 minutes
        refreshHandler.postDelayed(() -> isActiveMode = false, 120000);
    }

    private void setupRecyclerView() {
        tableAdapter = new TableRecyclerAdapter(tableList, this::handleTableClick);
        recyclerViewTables.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewTables.setAdapter(tableAdapter);
    }

    private void handleTableClick(TableModel table) {
        if (table != null) {
            Log.d(TAG, "Table clicked: " + table.getTableId() + " - Status: " + table.getStatus());

            // Handle UI operations on main thread
            mainHandler.post(() -> {
                String statusMessage = getStatusMessage(table.getStatus());
                Toast.makeText(this, "Bàn " + table.getTableId() + " - " + statusMessage, Toast.LENGTH_SHORT).show();

                // Navigate to OrderActivity
                Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                intent.putExtra("tableNumber", table.getTableId());
                intent.putExtra("tableStatus", table.getStatus());
                intent.putExtra("tableId", table.getId());
                intent.putExtra("apiKeyValid", apiKeyValidated); // Pass API status to next activity
                startActivity(intent);
            });

            // Update status in background if available and API is valid
            if ("available".equals(table.getStatus()) && apiKeyValidated) {
                updateTableStatusAsync(table.getTableId(), "reserved");
            } else if ("available".equals(table.getStatus()) && !apiKeyValidated) {
                Toast.makeText(this, "⚠️ Không thể cập nhật bàn - API không khả dụng", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getStatusMessage(String status) {
        switch (status != null ? status.toLowerCase() : "available") {
            case "reserved":
                return "Đã được đặt";
            case "occupied":
            case "serving":
                return "Đang phục vụ";
            case "available":
                return "Còn trống - Có thể đặt";
            default:
                return "Trạng thái: " + status;
        }
    }

    private void updateTableStatusAsync(int tableId, String newStatus) {
        // Only update if API key is validated
        if (!apiKeyValidated) {
            Log.w(TAG, "Cannot update table status - API key not validated");
            mainHandler.post(() ->
                    Toast.makeText(this, "❌ Không thể cập nhật - API không khả dụng", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        // Move API call to background thread
        executorService.execute(() -> {
            try {
                TableUpdateRequest updateRequest = new TableUpdateRequest(newStatus);
                Call<List<TableModel>> call = apiService.updateTableStatusById("eq." + tableId, updateRequest);

                call.enqueue(new Callback<List<TableModel>>() {
                    @Override
                    public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                        mainHandler.post(() -> {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                TableModel updatedTable = response.body().get(0);

                                // Consistency check: verify that returned status matches what we expected
                                if (!newStatus.equals(updatedTable.getStatus())) {
                                    Log.w(TAG, "Status mismatch: Expected " + newStatus + ", got " + updatedTable.getStatus());
                                    // Consider handling the mismatch - maybe retry or notify user
                                }

                                Log.d(TAG, "Table " + tableId + " status updated to " + updatedTable.getStatus());
                                Toast.makeText(MainActivity.this, "✅ Bàn " + tableId + " đã cập nhật thành " + updatedTable.getStatus(), Toast.LENGTH_SHORT).show();

                                // Immediate refresh after update
                                fetchTablesFromApi();
                            } else {
                                Log.e(TAG, "Failed to update table status. Response code: " + response.code());

                                if (response.code() == 401) {
                                    apiKeyValidated = false; // Mark API key as invalid
                                    Toast.makeText(MainActivity.this, "❌ API key hết hạn - Không thể cập nhật bàn " + tableId, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "❌ Không thể cập nhật bàn " + tableId + " (Lỗi: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                }

                                handleApiError(response);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<TableModel>> call, Throwable t) {
                        mainHandler.post(() -> {
                            Log.e(TAG, "Error updating table status: " + t.getMessage(), t);
                            Toast.makeText(MainActivity.this, "❌ Lỗi kết nối khi cập nhật bàn " + tableId, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in updateTableStatusAsync: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "❌ Lỗi hệ thống khi cập nhật bàn " + tableId, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                staffInfoLayout.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_order) {
                // Mở OrderListActivity thay vì hiển thị thông báo
                Intent intent = new Intent(MainActivity.this, OrderListActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_notify) {
                Toast.makeText(this, "🔔 Thông báo", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to NotificationActivity
                return true;
            } else if (itemId == R.id.nav_profile) {
                staffInfoLayout.setVisibility(View.VISIBLE);
                return true;
            }

            return false;
        });
    }

    private void loadStaffInfoAsync() {
        // Move SharedPreferences operations to background thread
        executorService.execute(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
                String staffName = prefs.getString("staff_name", "Chưa đăng nhập");
                String staffPosition = prefs.getString("staff_position", "");
                String staffCode = prefs.getString("staff_code", "");
                long loginTime = prefs.getLong("login_time", 0);

                // Format staff info with emojis
                StringBuilder staffInfo = new StringBuilder();
                staffInfo.append("👤 Tên: ").append(staffName).append("\n");
                staffInfo.append("💼 Chức vụ: ").append(staffPosition).append("\n");
                staffInfo.append("🆔 Mã NV: ").append(staffCode);

                // Add API status info
                staffInfo.append("\n🔑 API: ").append(apiKeyValidated ? "✅ Hoạt động" : "❌ Không khả dụng");

                String loginTimeText;
                if (loginTime > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                    String formattedTime = sdf.format(new Date(loginTime));
                    loginTimeText = "🕐 Giờ đăng nhập: " + formattedTime;
                } else {
                    loginTimeText = "🕐 Giờ đăng nhập: Không xác định";
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    if (staffNameTextView != null) {
                        staffNameTextView.setText(staffInfo.toString());
                    }
                    if (staffLoginTimeTextView != null) {
                        staffLoginTimeTextView.setText(loginTimeText);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading staff info: " + e.getMessage(), e);
            }
        });
    }

    private void fetchTablesFromApi() {
        // Only fetch if API key is validated
        if (!apiKeyValidated) {
            Log.w(TAG, "Skipping API fetch - API key not validated");
            loadSampleTablesAsync();
            return;
        }

        if (isRefreshing) {
            Log.d(TAG, "Already refreshing, skipping...");
            return;
        }

        isRefreshing = true;
        Log.d(TAG, "Fetching tables from API...");

        // Show refresh indicator on main thread
        mainHandler.post(() -> {
            if (!swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        // Move API call to background thread
        executorService.execute(() -> {
            try {
                Call<List<TableModel>> call = apiService.getAllTables();
                call.enqueue(new Callback<List<TableModel>>() {
                    @Override
                    public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                        // Handle response on main thread
                        mainHandler.post(() -> {
                            isRefreshing = false;
                            swipeRefreshLayout.setRefreshing(false);

                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "API Success: Received " + response.body().size() + " tables");

                                // Process data in background
                                processTableDataAsync(response.body());

                            } else {
                                Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());

                                if (response.code() == 401) {
                                    apiKeyValidated = false; // Mark API key as invalid
                                    Toast.makeText(MainActivity.this, "🔑 API key hết hạn! Sử dụng dữ liệu mẫu.", Toast.LENGTH_LONG).show();
                                }

                                handleApiError(response);
                                loadSampleTablesAsync();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<TableModel>> call, Throwable t) {
                        mainHandler.post(() -> {
                            isRefreshing = false;
                            swipeRefreshLayout.setRefreshing(false);

                            Log.e(TAG, "API Error: " + t.getMessage(), t);
                            loadSampleTablesAsync();
                            Toast.makeText(MainActivity.this, "🌐 Không thể kết nối database. Hiển thị dữ liệu mẫu.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in fetchTablesFromApi: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    isRefreshing = false;
                    swipeRefreshLayout.setRefreshing(false);
                    loadSampleTablesAsync();
                });
            }
        });
    }

    private void processTableDataAsync(List<TableModel> newTableList) {
        // Process data comparison in background
        executorService.execute(() -> {
            try {
                boolean hasChanges = checkForTableChanges(newTableList);

                // Log first table data for debugging
                if (!newTableList.isEmpty()) {
                    TableModel firstTable = newTableList.get(0);
                    Log.d(TAG, "Sample table - ID: " + firstTable.getId() +
                            ", TableID: " + firstTable.getTableId() +
                            ", Status: " + firstTable.getStatus());
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    tableAdapter.updateData(newTableList);
                    updateLastRefreshTime();

                    if (hasChanges) {
                        Toast.makeText(MainActivity.this, "🔄 Trạng thái bàn đã được cập nhật!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing table data: " + e.getMessage(), e);
            }
        });
    }

    private void handleApiError(Response<?> response) {
        // Handle error processing in background
        executorService.execute(() -> {
            try {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                Log.e(TAG, "Error response: " + errorBody);

                // Show user-friendly error message on main thread
                mainHandler.post(() -> {
                    if (response.code() == 401) {
                        apiKeyValidated = false;
                        Toast.makeText(this, "⚠️ Lỗi xác thực API. Sử dụng dữ liệu mẫu.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "⚠️ Lỗi server (" + response.code() + "). Sử dụng dữ liệu mẫu.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error reading error body: " + e.getMessage());
            }
        });
    }

    private boolean checkForTableChanges(List<TableModel> newTableList) {
        if (tableList.size() != newTableList.size()) {
            return true;
        }

        for (int i = 0; i < tableList.size(); i++) {
            if (i < newTableList.size()) {
                TableModel newTable = newTableList.get(i);

                // Find corresponding old table by tableId
                TableModel oldTable = null;
                for (TableModel t : tableList) {
                    if (t.getTableId() == newTable.getTableId()) {
                        oldTable = t;
                        break;
                    }
                }

                if (oldTable == null || !oldTable.getStatus().equals(newTable.getStatus())) {
                    Log.d(TAG, "Table " + newTable.getTableId() + " status changed: " +
                            (oldTable != null ? oldTable.getStatus() : "new") + " -> " + newTable.getStatus());
                    return true;
                }
            }
        }

        return false;
    }

    private void updateLastRefreshTime() {
        String currentTime = getCurrentTime();
        if (lastUpdateTextView != null) {
            String statusIcon = apiKeyValidated ? "✅" : "📱";
            String source = apiKeyValidated ? "API" : "Mẫu";
            lastUpdateTextView.setText(statusIcon + " Cập nhật lần cuối (" + source + "): " + currentTime);
        }
    }

    private void loadSampleTablesAsync() {
        // Move sample data creation to background thread
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Loading sample tables...");
                List<TableModel> sampleTables = new ArrayList<>();

                // Sample data matching your database structure
                String[] statuses = {"reserved", "reserved", "available", "available", "reserved", "reserved", "available", "reserved"};
                String[] ids = {"table1", "table2", "table3", "table4", "table5", "table6", "table7", "table8"};

                for (int i = 1; i <= 8; i++) {
                    TableModel table = new TableModel();
                    table.setTableId(i);
                    table.setStatus(statuses[i - 1]);
                    table.setId(ids[i - 1]);
                    sampleTables.add(table);
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    tableAdapter.updateData(sampleTables);
                    updateLastRefreshTime();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading sample tables: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed - starting auto refresh");

        // Update staff info to reflect current API status
        loadStaffInfoAsync();

        // Immediate refresh when resuming (in background)
        if (apiKeyValidated) {
            fetchTablesFromApi();
        } else {
            // Retest API connection
            testApiConnection();
        }

        // Start auto-refresh
        int interval = isActiveMode ? FAST_REFRESH_INTERVAL : REFRESH_INTERVAL;
        refreshHandler.postDelayed(refreshRunnable, interval);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused - stopping auto refresh");

        // Stop auto-refresh to save battery
        refreshHandler.removeCallbacks(refreshRunnable);
        isActiveMode = false; // Reset active mode
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up background threads
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown completed");
        }
    }

    @Override
    public void onBackPressed() {
        if (staffInfoLayout.getVisibility() == View.VISIBLE) {
            staffInfoLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}