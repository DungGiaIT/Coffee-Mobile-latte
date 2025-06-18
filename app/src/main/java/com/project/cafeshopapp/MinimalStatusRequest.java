package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Ultra-minimal model for table status updates only
 * No constructors, no extra fields, just the bare minimum
 */
public class MinimalStatusRequest {
    
    @SerializedName("status")
    public String status;
    
    public MinimalStatusRequest() {}
    
    public MinimalStatusRequest(String status) {
        this.status = status;
    }
}
