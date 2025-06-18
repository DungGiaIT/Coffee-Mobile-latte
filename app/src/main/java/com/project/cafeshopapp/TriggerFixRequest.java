package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request with common timestamp field variations to satisfy triggers
 */
public class TriggerFixRequest {
    
    @SerializedName("status")
    private String status;
    
    // Try different timestamp field variations that triggers might expect
    @SerializedName("updated_at")
    private String updatedAt;
    
    @SerializedName("updatedat") 
    private String updatedat;
    
    public TriggerFixRequest() {}
    
    public TriggerFixRequest(String status) {
        this.status = status;
        // Set timestamp fields to null - let database handle them
        this.updatedAt = null;
        this.updatedat = null;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
