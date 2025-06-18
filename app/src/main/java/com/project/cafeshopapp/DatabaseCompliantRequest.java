package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request model that ONLY includes fields that actually exist in database
 * Based on error analysis: only 'status' and 'updatedAt' exist
 */
public class DatabaseCompliantRequest {

    @SerializedName("status")
    private String status;

    // Only include updatedAt that actually exists in database
    @SerializedName("updatedAt")
    private String updatedAt;

    public DatabaseCompliantRequest() {
    }

    public DatabaseCompliantRequest(String status) {
        this.status = status;
        // DO NOT set updatedAt - let database trigger handle it automatically
        // this.updatedAt = java.time.Instant.now().toString();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
