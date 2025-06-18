package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Complete table update request that satisfies all database triggers
 * Includes ALL fields that triggers might expect
 */
public class CompleteTableRequest {
    
    @SerializedName("status")
    private String status;
    
    // Include all possible timestamp field variations
    @SerializedName("updatedAt")
    private String updatedAt;
    
    @SerializedName("updatedat") 
    private String updatedat;
    
    @SerializedName("updated_at")
    private String updated_at;
    
    // Include created timestamp too in case trigger checks it
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("createdat")
    private String createdat;
    
    @SerializedName("created_at") 
    private String created_at;
    
    public CompleteTableRequest() {}
    
    public CompleteTableRequest(String status) {
        this.status = status;
        
        // Set current timestamp for all timestamp variations
        String currentTime = java.time.Instant.now().toString();
        this.updatedAt = currentTime;
        this.updatedat = currentTime;
        this.updated_at = currentTime;
        
        // Don't touch created timestamps - leave as null
        this.createdAt = null;
        this.createdat = null;
        this.created_at = null;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
