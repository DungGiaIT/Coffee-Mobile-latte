package com.project.cafeshopapp;

public class DeleteOrdersRequest {
    private String table_id;

    public DeleteOrdersRequest(String tableId) {
        this.table_id = tableId;
    }

    public String getTable_id() {
        return table_id;
    }

    public void setTable_id(String table_id) {
        this.table_id = table_id;
    }
}
