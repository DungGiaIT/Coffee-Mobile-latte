package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class TableUpdateRequest {
    @SerializedName("status")
    private String status;

    // Constructors
    public TableUpdateRequest() {}

    public TableUpdateRequest(String status) {
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}