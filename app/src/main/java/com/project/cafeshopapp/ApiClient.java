package com.project.cafeshopapp;

import android.util.Log;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://ufgxsicqlaraqaeziohf.supabase.co/rest/v1/";

    // 🔑 API KEY
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4";

    private static Retrofit retrofit;
    private static Cache cache;

    // 🚀 TĂNG TIMEOUT VÀ TỐI ƯU CONNECTION
    private static final long CACHE_SIZE = 10 * 1024 * 1024; // 10 MB cache
    private static final int CACHE_MAX_AGE = 2; // Cache valid for 2 minutes
    private static final int CACHE_MAX_STALE = 7 * 24 * 60; // Cache acceptable when offline for 1 week

    public static void initCache(File cacheDir) {
        if (cache == null) {
            cache = new Cache(cacheDir, CACHE_SIZE);
            Log.d(TAG, "📦 Cache initialized with size: " + CACHE_SIZE + " bytes");
        }
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    // 🚀 TĂNG TIMEOUT VÀ TỐI ƯU HÓA PERFORMANCE
                    .connectTimeout(60, TimeUnit.SECONDS)    // Tăng từ 30s lên 60s
                    .readTimeout(60, TimeUnit.SECONDS)       // Tăng từ 30s lên 60s
                    .writeTimeout(60, TimeUnit.SECONDS)      // Thêm write timeout
                    .callTimeout(120, TimeUnit.SECONDS)      // Thêm call timeout tổng

                    // 🔗 TỐI ƯU CONNECTION POOL
                    .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))

                    // 🔄 RETRY CONFIGURATION
                    .retryOnConnectionFailure(true);

            // Add cache if initialized
            if (cache != null) {
                builder.cache(cache);

                // Add cache control interceptor
                builder.addNetworkInterceptor(chain -> {
                    Request request = chain.request();
                    okhttp3.Response originalResponse = chain.proceed(request);

                    // If request is GET, add cache headers
                    if (request.method().equalsIgnoreCase("GET")) {
                        return originalResponse.newBuilder()
                                .header("Cache-Control", "public, max-age=" + CACHE_MAX_AGE * 60)
                                .build();
                    } else {
                        return originalResponse;
                    }
                });

                // Add offline cache interceptor
                builder.addInterceptor(chain -> {
                    Request request = chain.request();

                    // If no network, use cached response if available
                    if (!isNetworkAvailable()) {
                        request = request.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=" + CACHE_MAX_STALE * 60)
                                .build();
                        Log.d(TAG, "📱 Network unavailable, using cache for: " + request.url());
                    }

                    return chain.proceed(request);
                });
            }

            // 🔧 Add headers and logging interceptor với performance logging
            builder.addInterceptor(new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
                    Request originalRequest = chain.request();

                    // 📊 PERFORMANCE TRACKING
                    long startTime = System.currentTimeMillis();
                    Log.d(TAG, "🔗 Starting request to: " + originalRequest.url());                    // 🔑 THÊM SUPABASE HEADERS với bypass RLS
                    Request newRequest = originalRequest.newBuilder()
                            .addHeader("apikey", API_KEY)
                            .addHeader("Authorization", "Bearer " + API_KEY)
                            .addHeader("Accept", "application/json")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=representation")                            // 🚀 Thêm headers tối ưu performance
                            .addHeader("User-Agent", "CoffeeShopApp/1.0")
                            .addHeader("Connection", "keep-alive")
                            // 🔓 Bypass RLS for testing (remove in production) - TEMPORARILY DISABLED
                            // .addHeader("Role", "service_role")
                            .build();

                    // 📝 LOG REQUEST DETAILS  
                    if (originalRequest.body() != null) {
                        try {
                            okio.Buffer buffer = new okio.Buffer();
                            originalRequest.body().writeTo(buffer);
                            String requestBody = buffer.readUtf8();
                            Log.d(TAG, "📤 Request body: " + requestBody);
                        } catch (Exception e) {
                            Log.w(TAG, "Could not log request body: " + e.getMessage());
                        }
                    }

                    okhttp3.Response response = chain.proceed(newRequest);

                    // 📊 LOG PERFORMANCE
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    Log.d(TAG, "📊 Request completed in " + duration + "ms - Response code: " + response.code());

                    // ⚠️ Warning if request is slow
                    if (duration > 5000) { // 5 seconds
                        Log.w(TAG, "🐌 SLOW REQUEST WARNING: " + originalRequest.url() + " took " + duration + "ms");
                    }

                    return response;
                }
            });

            OkHttpClient okHttpClient = builder.build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "🚀 Retrofit client initialized with optimized performance settings" +
                    (cache != null ? " and caching" : ""));
        }
        return retrofit;
    }

    // Helper method to check network availability
    private static boolean isNetworkAvailable() {
        // This is a placeholder. In a real app, you'd check network connectivity.
        // For example, using ConnectivityManager:
        // ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // For now, we'll assume network is available
        return true;
    }
}