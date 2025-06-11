package com.project.cafeshopapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // 🏠 ManagerTable APIs - Updated table name to match Supabase
    @GET("manager_table")  // Matches the actual table name in Supabase
    Call<List<TableModel>> getAllTables();

    @GET("manager_table")
    Call<List<TableModel>> getAllTablesWithSelect(@Query("select") String select);

    @GET("manager_table")
    Call<List<TableModel>> getTableById(@Query("tableId") String tableIdFilter);

    @GET("manager_table")
    Call<List<TableModel>> getTablesByStatus(@Query("status") String statusFilter);

    @PATCH("manager_table")
    Call<List<TableModel>> updateTableStatusById(@Query("tableId") String tableIdFilter, @Body TableUpdateRequest updateRequest);

    @PATCH("manager_table")
    Call<List<TableModel>> updateTableStatusByRecordId(@Query("id") String idFilter, @Body TableUpdateRequest updateRequest);

    @POST("manager_table")
    Call<List<TableModel>> createTable(@Body TableModel tableModel);

    // 🛒 Product APIs (if needed)
    @GET("product")
    Call<List<Product>> getProducts();

    // 📝 Order APIs (if needed)
    @GET("order")
    Call<List<OrderItem>> getOrdersByTable(@Query("tableId") String tableIdFilter);
}