package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Minimal model for table status updates only
 * This avoids timestamp field conflicts with Supabase triggers
 */
public class TableStatusOnlyRequest {
    
    @SerializedName("status")
    private String status;
    
    public TableStatusOnlyRequest() {}
    
    public TableStatusOnlyRequest(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
