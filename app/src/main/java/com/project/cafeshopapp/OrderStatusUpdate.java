package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class OrderStatusUpdate {
    @SerializedName("status")
    private String status;

    public OrderStatusUpdate(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}