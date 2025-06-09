package com.project.cafeshopapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface ApiService {

    @Headers({
            "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4",
            "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MTk0ODIsImV4cCI6MjA2NDI5NTQ4Mn0.scTWf1VRknpvZ4WcDzswtWRPa9EmuJOpcsy86emIUP4",
            "Accept: application/json",
            "Content-Type: application/json",
            "Prefer: return=representation"
    })

    @GET("ManagerTable")
    Call<List<TableModel>> getTables(@Query("select") String select);

    @PATCH("ManagerTable")
    Call<List<TableModel>> updateTableStatus(@Query("tableId") String filter, @Body TableModel tableModel);

    @GET("Staff")
    Call<List<StaffModel>> getStaffByCode(@Query("select") String select, @Query("code") String filter);

    @GET("products") // Giả sử bạn có bảng products
    Call<List<Product>> getProducts(@Query("select") String select);

    @GET("orders") // Giả sử bạn có bảng orders
    Call<List<OrderItem>> getOrdersByTable(@Query("select") String select, @Query("tableId") String filter);
}