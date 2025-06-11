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
    @GET("manager_table")
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

    // 📦 Order APIs - mới thêm vào
    @GET("order")
    Call<List<Order>> getAllOrders();

    @GET("order")
    Call<List<Order>> getOrdersByTable(@Query("tableId") String tableIdFilter);

    @GET("order")
    Call<List<Order>> getOrdersByStatus(@Query("status") String statusFilter);

    @PATCH("order")
    Call<List<Order>> updateOrderStatus(@Query("id") String orderId, @Body OrderStatusUpdate statusUpdate);

    // 🛒 Product APIs (if needed)
    @GET("product")
    Call<List<Product>> getProducts();

    // Phương thức cũ - giữ lại để tương thích với code hiện có
    @GET("order")
    Call<List<OrderItem>> getOrderItemsByTable(@Query("tableId") String tableIdFilter);
}