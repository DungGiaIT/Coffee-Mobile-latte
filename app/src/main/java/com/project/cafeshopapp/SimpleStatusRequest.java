package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Simple status-only request - trigger will handle updatedAt automatically
 */
public class SimpleStatusRequest {

    @SerializedName("status")
    private String status;

    public SimpleStatusRequest() {
    }

    public SimpleStatusRequest(String status) {
        this.status = status;
        // Don't set updatedAt - let database trigger handle it
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
