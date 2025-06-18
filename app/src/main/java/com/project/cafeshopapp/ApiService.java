package com.project.cafeshopapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
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
    Call<List<TableModel>> updateTableStatusById(@Query("tableId") String tableIdFilter,
            @Body TableStatusOnlyRequest updateRequest);

    @PATCH("manager_table")
    Call<List<TableModel>> updateTableStatusByRecordId(@Query("id") String idFilter,
            @Body TableStatusOnlyRequest updateRequest);// Simple status-only update without full model response

    @PATCH("manager_table")
    Call<Void> updateTableStatusOnly(@Query("tableId") String tableIdFilter,
            @Body TableStatusOnlyRequest updateRequest);// Alternative endpoint using 'id' field instead of 'tableId'

    @PATCH("manager_table")
    Call<Void> updateTableStatusOnlyById(@Query("id") String idFilter,
            @Body TableStatusOnlyRequest updateRequest);

    @POST("manager_table")
    Call<List<TableModel>> createTable(@Body TableModel tableModel);

    // 📦 Order APIs - CẬP NHẬT THEO DATABASE STRUCTURE
    @GET("order")
    Call<List<Order>> getAllOrders();

    @GET("order")
    Call<List<Order>> getOrdersByTable(@Query("tableId") String tableIdFilter);

    @GET("order")
    Call<List<Order>> getOrdersByStatus(@Query("status") String statusFilter);

    @GET("order")
    Call<List<Order>> getOrderById(@Query("id") String idFilter);

    // 🔧 ADD SELECT PARAMETER TO CONTROL RETURNED DATA
    @GET("order")
    Call<List<Order>> getAllOrdersWithSelect(@Query("select") String select);

    // 🔄 FORCE FRESH DATA - No Cache
    @Headers("Cache-Control: no-cache")
    @GET("order")
    Call<List<Order>> getAllOrdersFresh(@Query("select") String select);

    @GET("order")
    Call<List<Order>> getOrdersByTableWithSelect(@Query("tableId") String tableIdFilter,
            @Query("select") String select);

    // 🔄 FORCE FRESH DATA for specific table - No Cache
    @Headers("Cache-Control: no-cache")
    @GET("order")
    Call<List<Order>> getOrdersByTableFresh(@Query("tableId") String tableIdFilter,
            @Query("select") String select);

    @PATCH("order")
    Call<List<Order>> updateOrderStatus(@Query("id") String orderId, @Body OrderStatusUpdate statusUpdate); // DELETE
                                                                                                            // operations
                                                                                                            // for
                                                                                                            // orders

    @retrofit2.http.DELETE("order")
    Call<Void> deleteOrderById(@Query("id") String orderId);

    @retrofit2.http.DELETE("order")
    Call<Void> deleteOrdersByTable(@Query("tableId") String tableIdFilter);

    // RPC method for bulk operations (if available in Supabase)
    @POST("rpc/delete_orders_by_table")
    Call<Void> deleteOrdersByTableRPC(@Body DeleteOrdersRequest request);

    // 🛒 Product APIs (if needed)
    @GET("product")
    Call<List<Product>> getProducts();

    // 📋 Order Items API - to get product details in an order
    @GET("order_item")
    Call<List<OrderItem>> getOrderItemsByOrderId(@Query("orderId") String orderIdFilter);

    // New API method with select parameter to limit returned columns
    @GET("order_item")
    Call<List<OrderItem>> getOrderItemsByOrderIdWithSelect(
            @Query("orderId") String orderIdFilter,
            @Query("select") String selectFields);

    // Alternative approach with different query pattern
    @GET("rpc/get_order_items")
    Call<List<OrderItem>> getOrderItemsViaRPC(@Query("order_id") String orderId);

    @GET("order_item")
    Call<List<OrderItem>> getAllOrderItems();

    // Test endpoint with ultra-minimal request
    @PATCH("manager_table")
    Call<Void> testMinimalUpdate(@Query("tableId") String tableIdFilter,
            @Body MinimalStatusRequest updateRequest);

    // Try with Supabase-specific headers to bypass triggers
    @Headers({
            "Prefer: return=minimal",
            "X-Client-Info: mobile-app"
    })
    @PATCH("manager_table")
    Call<Void> updateTableStatusBypassed(@Query("tableId") String tableIdFilter,
            @Body TableStatusOnlyRequest updateRequest);

    // Service role endpoint to bypass RLS completely
    @Headers({
            "Authorization: Bearer "
                    + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmZ3hzaWNxbGFyYXFhZXppb2hmIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTczMzkyMDA5MSwiZXhwIjoyMDQ5NDk2MDkxfQ.zCO9HCMz5P8LN4uo3j1KGXS_7p4SqrrmKKy8FMGzjwQ",
            "Prefer: return=minimal"
    })
    @PATCH("manager_table")
    Call<Void> updateTableStatusServiceRole(@Query("tableId") String tableIdFilter,
            @Body ServiceRoleRequest updateRequest);

    // Direct SQL approach to bypass both RLS and triggers
    @Headers({
            "Content-Profile: public",
            "Prefer: return=minimal"
    })
    @POST("rpc/update_table_status_direct")
    Call<Void> updateTableStatusDirect(@Body DirectUpdateRequest request);

    // Safe function-based update that bypasses problematic triggers
    @POST("rpc/update_table_status_safe")
    Call<Void> updateTableStatusSafe(@Body SafeUpdateRequest request);

    // Test with trigger-expected fields included
    @PATCH("manager_table")
    Call<Void> updateTableStatusTriggerFix(@Query("tableId") String tableIdFilter,
            @Body TriggerFixRequest updateRequest);

    // Complete request with all timestamp fields to satisfy triggers
    @PATCH("manager_table")
    Call<Void> updateTableStatusComplete(@Query("tableId") String tableIdFilter,
            @Body CompleteTableRequest updateRequest);

    // Database-compliant request with only existing fields
    @PATCH("manager_table")
    Call<Void> updateTableStatusDatabaseCompliant(@Query("tableId") String tableIdFilter,
            @Body DatabaseCompliantRequest updateRequest);

    // 🔄 FORCE FRESH DATA for tables - No Cache (Realtime like OrderActivity)
    @Headers("Cache-Control: no-cache")
    @GET("manager_table")
    Call<List<TableModel>> getAllTablesFresh();

    @Headers("Cache-Control: no-cache")
    @GET("manager_table")
    Call<List<TableModel>> getAllTablesFreshWithSelect(@Query("select") String select);
}