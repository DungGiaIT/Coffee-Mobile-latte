package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class Order {
    @SerializedName("id")
    private String id;

    @SerializedName("tableId")
    private String tableId;

    @SerializedName("total")
    private double total;

    @SerializedName("status")
    private String status;

    @SerializedName("deliveryMethod")
    private String deliveryMethod;

    @SerializedName("deliveryAddress")
    private String deliveryAddress;

    // Constructors
    public Order() {}

    public Order(String id, String tableId, double total, String status, String deliveryMethod, String deliveryAddress) {
        this.id = id;
        this.tableId = tableId;
        this.total = total;
        this.status = status;
        this.deliveryMethod = deliveryMethod;
        this.deliveryAddress = deliveryAddress;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    // Utility method to format total as currency
    public String getFormattedTotal() {
        return String.format("%,.0fđ", total * 1000); // Assuming your prices are stored in thousands
    }

    // For validation
    public boolean isValid() {
        return id != null && !id.isEmpty() && status != null;
    }
}