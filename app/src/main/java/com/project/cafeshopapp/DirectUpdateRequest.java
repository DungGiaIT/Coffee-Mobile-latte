package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

/**
 * Request for direct SQL function call
 * Bypasses both RLS and triggers
 */
public class DirectUpdateRequest {
    
    @SerializedName("table_id")
    private int tableId;
    
    @SerializedName("new_status") 
    private String newStatus;
    
    public DirectUpdateRequest() {}
    
    public DirectUpdateRequest(int tableId, String newStatus) {
        this.tableId = tableId;
        this.newStatus = newStatus;
    }
    
    public int getTableId() {
        return tableId;
    }
    
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
