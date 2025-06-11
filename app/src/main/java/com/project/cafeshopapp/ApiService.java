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

    // 📦 Order APIs - CẬP NHẬT THEO DATABASE STRUCTURE
    @GET("order")
    Call<List<Order>> getAllOrders();

    @GET("order")
    Call<List<Order>> getOrdersByTable(@Query("tableId") String tableIdFilter);

    @GET("order")
    Call<List<Order>> getOrdersByStatus(@Query("status") String statusFilter);

    // 🔧 THÊM SELECT PARAMETER ĐỂ KIỂM SOÁT DỮ LIỆU TRẢ VỀ
    @GET("order")
    Call<List<Order>> getAllOrdersWithSelect(@Query("select") String select);

    @GET("order")
    Call<List<Order>> getOrdersByTableWithSelect(@Query("tableId") String tableIdFilter, @Query("select") String select);

    @PATCH("order")
    Call<List<Order>> updateOrderStatus(@Query("id") String orderId, @Body OrderStatusUpdate statusUpdate);

    // 🛒 Product APIs (if needed)
    @GET("product")
    Call<List<Product>> getProducts();

    // 📋 Order Items API - để lấy chi tiết sản phẩm trong đơn hàng
    @GET("order_item")
    Call<List<OrderItem>> getOrderItemsByOrderId(@Query("orderId") String orderIdFilter);

    @GET("order_item")
    Call<List<OrderItem>> getAllOrderItems();
}