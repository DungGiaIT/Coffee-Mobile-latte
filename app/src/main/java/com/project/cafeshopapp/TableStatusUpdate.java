package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Simple model for updating only table status, without timestamp fields.
 * This avoids Supabase trigger issues with automatic updatedAt handling.
 */
public class TableStatusUpdate {
    
    @SerializedName("status")
    private String status;
    
    // Constructor
    public TableStatusUpdate(String status) {
        this.status = status;
    }
    
    // Getter and Setter
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
