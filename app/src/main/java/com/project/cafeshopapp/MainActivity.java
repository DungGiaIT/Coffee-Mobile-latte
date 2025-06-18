package com.project.cafeshopapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.core.content.ContextCompat;
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
import java.util.concurrent.RejectedExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";    private static final int REFRESH_INTERVAL = 15000; // Reduced from 30s to 15s
    private static final int FAST_REFRESH_INTERVAL = 5000; // 10 seconds for active mode
    private static final int ORDER_ACTIVITY_REQUEST_CODE = 1001; // Request code for OrderActivity

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

    // Broadcast receiver for real-time table updates
    private BroadcastReceiver tableUpdateReceiver;

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
        initViews();        setupRecyclerView();
        setupBottomNavigation();
        setupAutoRefresh();
        setupSwipeRefresh();
        setupBroadcastReceiver(); // Setup real-time table updates

        // Load staff info in background to avoid blocking main thread
        loadStaffInfoAsync();

        // Test API connection first, then fetch data
        testApiConnectionAndFetchData();
        NetworkDebugUtils.logNetworkInfo(this);
        NetworkDebugUtils.testDNSResolution();
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
    } // 🧪 TEST API KEY

    private void testApiConnectionAndFetchData() {
        Log.d(TAG, "🧪 Testing API connection and key validation...");

        // Show initial loading message
        mainHandler.post(() -> {
            if (lastUpdateTextView != null) {
                lastUpdateTextView.setText("🔍 Checking API connection...");
            }
            Toast.makeText(this, "🧪 Checking API key...", Toast.LENGTH_SHORT).show();
        });

        testApiConnection();
    }

    private void testApiConnection() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "🔗 Creating optimized HTTP client for API test...");

                OkHttpClient client = new OkHttpClient.Builder() // 🚀 INCREASED TIMEOUT FOR TEST CONNECTION
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Increased to 30s
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Increased to 30s
                        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Added write timeout

                        // 🔄 RETRY CONFIGURATION
                        .retryOnConnectionFailure(true)

                        // 🔗 OPTIMIZED DNS
                        .dns(okhttp3.Dns.SYSTEM)
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "manager_table?limit=1")
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Accept", "application/json").addHeader("Content-Type", "application/json")
                        // 🚀 Added performance headers
                        .addHeader("User-Agent", "CoffeeShopApp/1.0")
                        .addHeader("Connection", "keep-alive")
                        .build();

                Log.d(TAG, "📡 Sending optimized API test request to: " + request.url());

                // 📊 TRACK PERFORMANCE
                long startTime = System.currentTimeMillis();

                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                        long duration = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "🚀 API test completed in " + duration + "ms");
                        handleApiTestResponse(response);
                    }

                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        long duration = System.currentTimeMillis() - startTime;
                        Log.e(TAG, "❌ API test failed after " + duration + "ms: " + e.getMessage());
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
                Log.d(TAG, "📄 API Test Response Body: "
                        + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

                mainHandler.post(() -> {
                    if (responseCode == 200) {
                        // API key is valid
                        apiKeyValidated = true;
                        Log.d(TAG, "✅ API Key validation successful!");
                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("✅ API connection successful - " + getCurrentTime());
                        }

                        Toast.makeText(this, "✅ Valid API key! Loading data...", Toast.LENGTH_SHORT).show();

                        // Now fetch the actual data
                        fetchTablesFromApi();

                    } else if (responseCode == 401) {
                        // Unauthorized - API key invalid
                        apiKeyValidated = false;
                        Log.e(TAG, "🔐 API Key validation failed - Unauthorized (401)");
                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("❌ Invalid API key - " + getCurrentTime());
                        }

                        Toast.makeText(this, "❌ Invalid API key! Using sample data.", Toast.LENGTH_LONG).show();
                        loadSampleTablesAsync();

                    } else if (responseCode == 403) {
                        // Forbidden - Permission denied
                        apiKeyValidated = false;
                        Log.e(TAG, "🚫 API Key validation failed - Forbidden (403)");
                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("🚫 Access denied - " + getCurrentTime());
                        }

                        Toast.makeText(this, "🚫 No database access permission! Using sample data.", Toast.LENGTH_LONG)
                                .show();
                        loadSampleTablesAsync();

                    } else {
                        // Other error codes
                        apiKeyValidated = false;
                        Log.w(TAG, "⚠️ API Test returned unexpected code: " + responseCode);
                        if (lastUpdateTextView != null) {
                            lastUpdateTextView.setText("⚠️ API Error (" + responseCode + ") - " + getCurrentTime());
                        }

                        Toast.makeText(this, "⚠️ API Error (" + responseCode + ")! Using sample data.",
                                Toast.LENGTH_SHORT).show();
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
                lastUpdateTextView.setText("🌐 API connection error - " + getCurrentTime());
            }

            String errorMessage = "🌐 Cannot connect to API";
            if (e instanceof java.net.SocketTimeoutException) {
                errorMessage += " (Timeout)";
            } else if (e instanceof java.net.UnknownHostException) {
                errorMessage += " (DNS Error)";
            } else if (e instanceof java.net.ConnectException) {
                errorMessage += " (Connection Error)";
            }
            errorMessage += "! Using sample data.";

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
                android.R.color.holo_red_light);
    }

    private void setupAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRefreshing) {
                    Log.d(TAG, "Auto-refreshing table data... (Active mode: " + isActiveMode + ", API Valid: "
                            + apiKeyValidated + ")");

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

        // Show toast on main thread mainHandler.post(() ->
        Toast.makeText(this, "🔄 Updating table status...", Toast.LENGTH_SHORT).show();

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
                Toast.makeText(this, "Table " + table.getTableId() + " - " + statusMessage, Toast.LENGTH_SHORT).show();                // Navigate to OrderActivity
                Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                intent.putExtra("tableNumber", table.getTableId());
                intent.putExtra("tableStatus", table.getStatus());
                intent.putExtra("tableId", table.getId());
                intent.putExtra("apiKeyValid", apiKeyValidated); // Pass API status to next activity
                startActivityForResult(intent, ORDER_ACTIVITY_REQUEST_CODE);
            });

            // Update status in background if available and API is valid
            if ("available".equals(table.getStatus()) && apiKeyValidated) {
                updateTableStatusAsync(table.getTableId(), "reserved");
            } else if ("available".equals(table.getStatus()) && !apiKeyValidated) {
                Toast.makeText(this, "⚠️ Cannot update table - API not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getStatusMessage(String status) {
        switch (status != null ? status.toLowerCase() : "available") {
            case "reserved":
                return "Reserved";
            case "occupied":
            case "serving":
                return "Currently serving";
            case "available":
                return "Available - Can be reserved";
            default:
                return "Status: " + status;
        }
    }    private void updateTableStatusAsync(int tableId, String newStatus) {
        // Only update if API key is validated
        if (!apiKeyValidated) {
            Log.w(TAG, "Cannot update table status - API key not validated");
            mainHandler
                    .post(() -> Toast.makeText(this, "❌ Cannot update - API not available", Toast.LENGTH_SHORT).show());
            return;
        }

        // Move API call to background thread
        executorService.execute(() -> {
            try {
                TableUpdateRequest updateRequest = new TableUpdateRequest(newStatus);
                Call<Void> call = apiService.updateTableStatusOnly("eq." + tableId, updateRequest);

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        mainHandler.post(() -> {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Table " + tableId + " status updated to " + newStatus + " (status-only endpoint)");
                                Toast.makeText(MainActivity.this,
                                        "✅ Table " + tableId + " has been updated to " + newStatus,
                                        Toast.LENGTH_SHORT).show();

                                // Immediate refresh after update
                                fetchTablesFromApi();
                            } else {
                                Log.e(TAG, "Failed to update table status. Response code: " + response.code());

                                if (response.code() == 401) {
                                    apiKeyValidated = false; // Mark API key as invalid
                                                             // Toast.makeText(MainActivity.this, "❌ API key expired -
                                                             // Cannot update table " + tableId,
                                                             // Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            "❌ Cannot update table " + tableId + " (Error: " + response.code() + ")",
                                            Toast.LENGTH_SHORT).show();
                                }

                                handleApiError(response);
                            }
                        });
                    }                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        mainHandler.post(() -> {
                            Log.e(TAG, "Error updating table status: " + t.getMessage(), t);
                            Toast.makeText(MainActivity.this, "❌ Connection error when updating table " + tableId,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in updateTableStatusAsync: " + e.getMessage(), e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "❌ System error when updating table " + tableId, Toast.LENGTH_SHORT).show());
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
                Toast.makeText(this, "🔔 Notifications", Toast.LENGTH_SHORT).show();
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
                String staffName = prefs.getString("staff_name", "Not logged in");
                String staffPosition = prefs.getString("staff_position", "");
                String staffCode = prefs.getString("staff_code", "");
                long loginTime = prefs.getLong("login_time", 0);

                // Format staff info with emojis
                StringBuilder staffInfo = new StringBuilder();
                staffInfo.append("👤 Name: ").append(staffName).append("\n");
                staffInfo.append("💼 Position: ").append(staffPosition).append("\n");
                staffInfo.append("🆔 Staff ID: ").append(staffCode);

                // Add API status info
                staffInfo.append("\n🔑 API: ").append(apiKeyValidated ? "✅ Working" : "❌ Not available");

                String loginTimeText;
                if (loginTime > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                    String formattedTime = sdf.format(new Date(loginTime));
                    loginTimeText = "🕐 Login time: " + formattedTime;
                } else {
                    loginTimeText = "🕐 Login time: Not specified";
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
        Log.d(TAG, "Fetching tables from API..."); // Show refresh indicator on main thread
        mainHandler.post(() -> {
            if (!swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        // Use synchronized block for thread-safe access to the ExecutorService
        ExecutorService localExecutor;
        synchronized (this) {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newFixedThreadPool(3);
                Log.d(TAG, "ExecutorService re-initialized in fetchTablesFromApi");
            }
            localExecutor = executorService;
        }

        // Move API call to background thread with rejection handling
        try {
            localExecutor.execute(() -> {
                try {
                    // Check if activity is still active
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "Skipping API call because activity is no longer active");
                        return;
                    }

                    Call<List<TableModel>> call = apiService.getAllTables();
                    call.enqueue(new Callback<List<TableModel>>() {
                        @Override
                        public void onResponse(Call<List<TableModel>> call, Response<List<TableModel>> response) {
                            // Handle response on main thread with activity lifecycle check
                            mainHandler.post(() -> {
                                // Check activity state before updating UI
                                if (isFinishing() || isDestroyed()) {
                                    Log.d(TAG, "Skipping response handling - activity no longer active");
                                    return;
                                }

                                isRefreshing = false;
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }

                                if (response.isSuccessful() && response.body() != null) {
                                    Log.d(TAG, "API Success: Received " + response.body().size() + " tables");

                                    // Process data in background
                                    processTableDataAsync(response.body());

                                } else {
                                    Log.w(TAG, "API Failed: " + response.code() + " - " + response.message());

                                    if (response.code() == 401) {
                                        apiKeyValidated = false; // Mark API key as invalid
                                        Toast.makeText(MainActivity.this, "🔑 API key expired! Using sample data.",
                                                Toast.LENGTH_LONG).show();
                                    }

                                    handleApiError(response);
                                    loadSampleTablesAsync();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<List<TableModel>> call, Throwable t) {
                            mainHandler.post(() -> {
                                // Check if activity is still active before updating UI
                                if (isFinishing() || isDestroyed()) {
                                    Log.d(TAG, "Skipping failure handling - activity no longer active");
                                    return;
                                }

                                isRefreshing = false;
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }

                                Log.e(TAG, "API Error: " + t.getMessage(), t);

                                // Record API validation failure
                                apiKeyValidated = false;

                                // Load sample data safely using synchronized ExecutorService
                                try {
                                    loadSampleTablesAsync();
                                    Toast.makeText(MainActivity.this,
                                            "🌐 Cannot connect to database. Showing sample data.",
                                            Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading sample data after API failure", e);
                                }
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
        } catch (java.util.concurrent.RejectedExecutionException rex) {
            // Use fully qualified class name to avoid import issues
            Log.e(TAG, "API fetch task was rejected: " + rex.getMessage(), rex);
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    isRefreshing = false;
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    Toast.makeText(MainActivity.this, "🌐 Showing offline data...", Toast.LENGTH_SHORT).show();
                    loadSampleTablesAsync();
                }
            });
        }
    }

    private void processTableDataAsync(List<TableModel> newTableList) {
        // Safety check for activity lifecycle state and instance state
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Skipping processTableDataAsync because activity is finishing or destroyed");
            return;
        }

        // Use a local reference to avoid race conditions
        final ExecutorService localExecutor;

        synchronized (this) {
            if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
                // Create new ExecutorService if it's shutdown or terminated
                executorService = Executors.newFixedThreadPool(3);
                Log.d(TAG, "ExecutorService was re-initialized in processTableDataAsync");
            }
            localExecutor = executorService;
        }

        try {
            // Process data comparison in background using the local reference
            if (!localExecutor.isShutdown()) {
                localExecutor.execute(() -> {
                    try {
                        // Check if activity is still alive before processing
                        if (isFinishing() || isDestroyed()) {
                            Log.w(TAG, "Skipping table data processing because activity is no longer active");
                            return;
                        }

                        boolean hasChanges = checkForTableChanges(newTableList);

                        // Log first table data for debugging
                        if (!newTableList.isEmpty()) {
                            TableModel firstTable = newTableList.get(0);
                            Log.d(TAG, "Sample table - ID: " + firstTable.getId() +
                                    ", TableID: " + firstTable.getTableId() +
                                    ", Status: " + firstTable.getStatus());
                        }

                        // Update UI on main thread with null check on activity state
                        final boolean finalHasChanges = hasChanges;
                        mainHandler.post(() -> {
                            // Double-check activity state before touching UI
                            if (!isFinishing() && !isDestroyed()) {
                                tableAdapter.updateData(newTableList);
                                updateLastRefreshTime();
                                if (finalHasChanges) {
                                    Toast.makeText(MainActivity.this, "🔄 Table status has been updated!",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing table data: " + e.getMessage(), e);
                    }
                });
            } else {
                Log.w(TAG, "Cannot process table data - ExecutorService is shutdown");
            }
        } catch (java.util.concurrent.RejectedExecutionException rex) {
            Log.e(TAG, "Task rejected by ExecutorService: " + rex.getMessage(), rex);
            // Handle the rejection gracefully, possibly retry with a new executor
            synchronized (this) {
                if (!isFinishing() && !isDestroyed()) {
                    Log.d(TAG, "Creating new ExecutorService after rejection");
                    executorService = Executors.newFixedThreadPool(2);
                    // Consider retrying the operation after a short delay
                }
            }
        }
    }

    private void handleApiError(Response<?> response) {
        // Safety check for activity lifecycle state
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Skipping handleApiError because activity is finishing or destroyed");
            return;
        }

        // Read error information first before attempting background processing
        String errorBody;
        try {
            errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
            Log.e(TAG, "Error response: " + errorBody);
        } catch (IOException e) {
            errorBody = "Error reading error body: " + e.getMessage();
            Log.e(TAG, errorBody, e);
        }

        // Capture error code for UI thread
        final int errorCode = response.code();
        final String finalErrorBody = errorBody;

        // Use a try-catch block to handle RejectedExecutionException
        try {
            // Use synchronized to safely check executorService
            ExecutorService localExecutor;
            synchronized (this) {
                if (executorService == null || executorService.isShutdown()) {
                    Log.w(TAG, "Creating new ExecutorService for error handling");
                    executorService = Executors.newFixedThreadPool(1);
                }
                localExecutor = executorService;
            }

            localExecutor.execute(() -> {
                // Log detailed error information
                Log.e(TAG, "Processing API error: " + errorCode + ", " + finalErrorBody);

                // Show user-friendly error message on main thread
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        if (errorCode == 401) {
                            apiKeyValidated = false;
                            Toast.makeText(this, "⚠️ API authentication error. Using sample data.", Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Toast.makeText(this, "⚠️ Server error (" + errorCode + "). Using sample data.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException rex) {
            Log.e(TAG, "Error handling was rejected: " + rex.getMessage(), rex);

            // Fall back to main thread for critical error handling
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    apiKeyValidated = false;
                    Toast.makeText(this, "⚠️ Connection error. Using sample data.", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            String source = apiKeyValidated ? "API" : "Sample";
            lastUpdateTextView.setText(statusIcon + " Last update (" + source + "): " + currentTime);
        }
    }

    private void loadSampleTablesAsync() {
        // Safety check for activity lifecycle state
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Skipping loadSampleTablesAsync because activity is finishing or destroyed");
            return;
        }

        // Create sample data first, in case the background thread fails
        List<TableModel> sampleTables = new ArrayList<>();
        String[] statuses = { "reserved", "reserved", "available", "available", "reserved", "reserved",
                "available", "reserved" };
        String[] ids = { "table1", "table2", "table3", "table4", "table5", "table6", "table7", "table8" };

        for (int i = 1; i <= 8; i++) {
            TableModel table = new TableModel();
            table.setTableId(i);
            table.setStatus(statuses[i - 1]);
            table.setId(ids[i - 1]);
            sampleTables.add(table);
        }

        // Keep a final reference to the sample data
        final List<TableModel> finalSampleTables = sampleTables;

        try {
            // Use synchronized block to safely check ExecutorService state
            ExecutorService localExecutor;
            synchronized (this) {
                if (executorService == null || executorService.isShutdown()) {
                    Log.d(TAG, "Creating new ExecutorService for sample data");
                    executorService = Executors.newFixedThreadPool(1);
                }
                localExecutor = executorService;
            }

            // Process on background thread
            localExecutor.execute(() -> {
                try {
                    Log.d(TAG, "Loading sample tables...");

                    // Update UI on main thread with lifecycle checks
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            tableAdapter.updateData(finalSampleTables);
                            updateLastRefreshTime();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in sample tables background task: " + e.getMessage(), e);

                    // Fallback - update directly on main thread if background fails
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            try {
                                tableAdapter.updateData(finalSampleTables);
                                updateLastRefreshTime();
                            } catch (Exception ex) {
                                Log.e(TAG, "Fatal error updating sample data on UI thread", ex);
                            }
                        }
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException rex) {
            Log.e(TAG, "Sample data task was rejected: " + rex.getMessage(), rex);

            // Last resort fallback - try to update directly on main thread
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    try {
                        tableAdapter.updateData(finalSampleTables);
                        updateLastRefreshTime();
                        Toast.makeText(MainActivity.this, "📱 Showing sample data (offline mode)", Toast.LENGTH_SHORT)
                                .show();
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot update UI with sample data", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed - starting auto refresh");

        synchronized (this) {
            // Recreate ExecutorService if it's shutdown or null
            if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
                executorService = Executors.newFixedThreadPool(3);
                Log.d(TAG, "ExecutorService was re-initialized in onResume");
            }
        }

        // First priority: Check if any pending UI updates are needed
        mainHandler.post(() -> {
            // Update staff info to reflect current API status (do this first as it's
            // lightweight)
            loadStaffInfoAsync();

            // Schedule data refresh with a small delay to avoid overwhelming the UI thread
            mainHandler.postDelayed(() -> {
                if (!isFinishing()) {
                    if (apiKeyValidated) {
                        fetchTablesFromApi();
                    } else {
                        // Retest API connection
                        testApiConnection();
                    }
                }
            }, 500); // Short delay to let UI stabilize first
        });

        // Start auto-refresh with a small buffer to avoid race conditions
        isActiveMode = true; // Initially in active mode when resumed
        int interval = isActiveMode ? FAST_REFRESH_INTERVAL : REFRESH_INTERVAL;
        refreshHandler.postDelayed(refreshRunnable, 1000); // Initial refresh after 1 second

        // Reset to normal mode after 30 seconds of inactivity
        refreshHandler.postDelayed(() -> isActiveMode = false, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused - stopping auto refresh");

        // First, stop any scheduled refresh tasks
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        // Reset active mode to conserve battery
        isActiveMode = false;

        // Use a synchronized block to safely handle the ExecutorService
        synchronized (this) {
            // Only shutdown if the executor exists and is not already shutdown
            if (executorService != null && !executorService.isShutdown()) {
                try {
                    // Use shutdownNow only for immediate operations, normally prefer shutdown()
                    // shutdownNow() can cause RejectedExecutionException for new tasks
                    // but will interrupt currently running tasks - use with caution
                    Log.d(TAG, "Shutting down ExecutorService in onPause");
                    executorService.shutdown();

                    // Don't wait for termination here as it could block the UI thread
                } catch (Exception e) {
                    Log.e(TAG, "Error shutting down ExecutorService: " + e.getMessage(), e);
                }
            }
        }
    }    @Override
    protected void onDestroy() {
        Log.d(TAG, "Activity being destroyed - cleaning up resources");

        // Unregister broadcast receiver
        if (tableUpdateReceiver != null) {
            try {
                unregisterReceiver(tableUpdateReceiver);
                Log.d(TAG, "📡 BroadcastReceiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "BroadcastReceiver was not registered: " + e.getMessage());
            }
        }

        // First, stop any scheduled refresh tasks
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler.removeCallbacksAndMessages(null); // Remove all pending messages
        }

        // Handle the ExecutorService in a thread-safe way
        synchronized (this) {
            if (executorService != null) {
                if (!executorService.isShutdown()) {
                    try {
                        // Force immediate shutdown of all tasks
                        List<Runnable> pendingTasks = executorService.shutdownNow();
                        Log.d(TAG, "ExecutorService shutdown with " + pendingTasks.size() + " pending tasks");

                        // Wait a short time for tasks to terminate
                        boolean terminated = executorService.awaitTermination(500,
                                java.util.concurrent.TimeUnit.MILLISECONDS);
                        Log.d(TAG, "ExecutorService terminated: " + terminated);

                    } catch (Exception e) {
                        Log.e(TAG, "Error during ExecutorService shutdown: " + e.getMessage(), e);
                    }
                } else {
                    Log.d(TAG, "ExecutorService was already shutdown");
                }
                executorService = null; // Allow for garbage collection
            }
        }

        // Clear any references to activity context
        apiService = null;

        // Call parent method last
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (staffInfoLayout.getVisibility() == View.VISIBLE) {
            staffInfoLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Utility method to safely run tasks on background thread with proper error
     * handling
     *
     * @param task         The task to run in the background
     * @param errorMessage Message to log if execution is rejected
     */
    private void safeBackgroundExecute(Runnable task, String errorMessage) {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Skipping task execution because activity is finishing or destroyed");
            return;
        }

        try {
            // Use synchronized block to safely handle the ExecutorService
            ExecutorService localExecutor;
            synchronized (this) {
                if (executorService == null || executorService.isShutdown()) {
                    executorService = Executors.newFixedThreadPool(2);
                    Log.d(TAG, "ExecutorService was re-initialized for task: " + errorMessage);
                }
                localExecutor = executorService;
            }

            localExecutor.execute(task);

        } catch (java.util.concurrent.RejectedExecutionException rex) {
            Log.e(TAG, "Task rejected: " + errorMessage + " - " + rex.getMessage(), rex);

            // Attempt to recreate the executor and retry once
            synchronized (this) {
                try {
                    if (!isFinishing() && !isDestroyed()) {
                        if (executorService != null && !executorService.isTerminated()) {
                            executorService.shutdown();
                        }
                        executorService = Executors.newFixedThreadPool(1);
                        Log.d(TAG, "Recreated ExecutorService after rejection");
                        executorService.execute(task);
                    }
                } catch (Exception e) {
                    // If we still get an error, log it and give up
                    Log.e(TAG, "Fatal error executing task after retry: " + e.getMessage(), e);

                    // Run critical tasks on main thread as a last resort
                    // Note: This should be used very carefully to avoid ANRs
                    if (errorMessage.contains("CRITICAL")) {
                        Log.w(TAG, "Running critical task on main thread as last resort");
                        mainHandler.post(task);
                    }                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ORDER_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            // OrderActivity completed successfully, immediately refresh data
            Log.d(TAG, "🔄 OrderActivity completed - triggering immediate refresh");
            
            // Cancel any pending refresh and trigger immediate one
            if (refreshHandler != null && refreshRunnable != null) {
                refreshHandler.removeCallbacks(refreshRunnable);
            }
            
            // Force immediate refresh
            if (apiKeyValidated && !isRefreshing) {
                Log.d(TAG, "📊 Immediate refresh triggered after order completion");
                fetchTablesFromApi();
            }
            
            // Resume normal refresh cycle
            if (refreshHandler != null && refreshRunnable != null) {
                int interval = isActiveMode ? FAST_REFRESH_INTERVAL : REFRESH_INTERVAL;
                refreshHandler.postDelayed(refreshRunnable, interval);
            }
        }
    }

    private void setupBroadcastReceiver() {
        tableUpdateReceiver = new BroadcastReceiver() {
            @Override            public void onReceive(Context context, Intent intent) {
                if ("REFRESH_TABLES".equals(intent.getAction())) {
                    int tableNumber = intent.getIntExtra("tableNumber", -1);
                    String newStatus = intent.getStringExtra("newStatus");
                    
                    Log.d(TAG, "🔄 BROADCAST RECEIVED: Table " + tableNumber + " → " + newStatus);
                    
                    // IMMEDIATE REFRESH - bypass isRefreshing check for broadcast updates
                    if (apiKeyValidated) {
                        Log.d(TAG, "⚡ IMMEDIATE REFRESH triggered by broadcast (forcing refresh)");
                        
                        // Force refresh by temporarily bypassing isRefreshing
                        boolean wasRefreshing = isRefreshing;
                        isRefreshing = false;
                        fetchTablesFromApi();
                        
                        // Restore the original state if it was refreshing
                        if (wasRefreshing) {
                            isRefreshing = true;
                        }
                    } else {
                        Log.d(TAG, "⏳ Refresh skipped - API not validated");
                    }
                }
            }};
          // Register receiver for table update broadcasts
        IntentFilter filter = new IntentFilter("REFRESH_TABLES");
        ContextCompat.registerReceiver(this, tableUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "📡 BroadcastReceiver registered for table updates");
    }
}