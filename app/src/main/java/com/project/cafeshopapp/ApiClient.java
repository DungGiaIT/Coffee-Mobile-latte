package com.project.cafeshopapp;

import android.util.Log;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://ufgxsicqlaraqaeziohf.supabase.co/rest/v1/";

    // 🔑 API KEY
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4";

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {

            // 🔧 OKHTTP CLIENT VỚI HEADERS VÀ LOGGING
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
                            Request originalRequest = chain.request();

                            Log.d(TAG, "🔗 Making request to: " + originalRequest.url());

                            // 🔑 THÊM SUPABASE HEADERS
                            Request newRequest = originalRequest.newBuilder()
                                    .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4")
                                    .addHeader("Authorization", "Bearer " + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4")
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=representation")
                                    .build();

                            Log.d(TAG, "🔑 Added headers: apikey, Authorization, Accept, Content-Type, Prefer");

                            okhttp3.Response response = chain.proceed(newRequest);
                            Log.d(TAG, "📊 Response code: " + response.code());

                            return response;
                        }
                    })
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "🚀 Retrofit client initialized with Supabase headers");
        }
        return retrofit;
    }
}