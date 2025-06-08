package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class TableModel {
    @SerializedName("tableId")
    private int tableId;

    @SerializedName("status")
    private String status;

    // Getter
    public int getTableId() {
        return tableId;
    }

    public String getStatus() {
        return status;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
