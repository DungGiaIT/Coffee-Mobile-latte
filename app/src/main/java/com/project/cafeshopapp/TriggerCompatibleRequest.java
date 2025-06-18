package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request model that matches database trigger expectations
 * Uses lowercase 'updatedat' to match trigger requirements
 */
public class TriggerCompatibleRequest {
    
    @SerializedName("status")
    public String status;
    
    // Include updatedat field to satisfy triggers (will be auto-populated by DB)
    @SerializedName("updatedat") 
    public String updatedat;
    
    public TriggerCompatibleRequest(String status) {
        this.status = status;
        // Don't set updatedat - let database handle it
        this.updatedat = null;
    }
}
