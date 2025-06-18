package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request model with RLS bypass approach
 * Uses service_role headers to bypass RLS policies
 */
public class ServiceRoleRequest {
    
    @SerializedName("status")
    private String status;
    
    public ServiceRoleRequest() {}
    
    public ServiceRoleRequest(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
