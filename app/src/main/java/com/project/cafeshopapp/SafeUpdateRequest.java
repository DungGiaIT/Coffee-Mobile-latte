package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request for safe RPC function that bypasses problematic triggers
 */
public class SafeUpdateRequest {
    
    @SerializedName("target_table_id")
    private int targetTableId;
    
    @SerializedName("new_status")
    private String newStatus;
    
    public SafeUpdateRequest() {}
    
    public SafeUpdateRequest(int targetTableId, String newStatus) {
        this.targetTableId = targetTableId;
        this.newStatus = newStatus;
    }
    
    public int getTargetTableId() {
        return targetTableId;
    }
    
    public void setTargetTableId(int targetTableId) {
        this.targetTableId = targetTableId;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
