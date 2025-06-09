package com.project.cafeshopapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // 🏠 ManagerTable APIs
    @GET("ManagerTable")
    Call<List<TableModel>> getAllTables();

    @GET("ManagerTable")
    Call<List<TableModel>> getAllTablesWithSelect(@Query("select") String select);

    @GET("ManagerTable")
    Call<List<TableModel>> getTableById(@Query("tableId") String tableIdFilter);

    @GET("ManagerTable")
    Call<List<TableModel>> getTablesByStatus(@Query("status") String statusFilter);

    @PATCH("ManagerTable")
    Call<List<TableModel>> updateTableStatusById(@Query("tableId") String tableIdFilter, @Body TableUpdateRequest updateRequest);

    @PATCH("ManagerTable")
    Call<List<TableModel>> updateTableStatusByRecordId(@Query("id") String idFilter, @Body TableUpdateRequest updateRequest);

    @POST("ManagerTable")
    Call<List<TableModel>> createTable(@Body TableModel tableModel);

    // 🛒 Product APIs (if needed)
    @GET("Product")
    Call<List<Product>> getProducts();

    // 📝 Order APIs (if needed)
    @GET("Order")
    Call<List<OrderItem>> getOrdersByTable(@Query("tableId") String tableIdFilter);

    // 🗑️ REMOVED: getStaffByCode - không cần vì không dùng Staff table
}