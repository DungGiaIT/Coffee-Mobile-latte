package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class TableModel {
    @SerializedName("id")
    private String id;

    @SerializedName("tableId")
    private int tableId;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Constructors
    public TableModel() {}

    public TableModel(int tableId, String status) {
        this.tableId = tableId;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Validation method to ensure tableId and status are never null
    public boolean isValid() {
        return status != null && !status.isEmpty();
    }

    // Get a default status if the current one is invalid
    public String getStatusSafe() {
        return status != null && !status.isEmpty() ? status : "available";
    }
}