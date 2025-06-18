package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class Order {    @SerializedName("id")
    private String id;

    @SerializedName("tableId")  // Match database: tableId (text)
    private String tableId;

    @SerializedName("total")
    private double total;

    @SerializedName("status")
    private String status;

    @SerializedName("customerName")  // Match database: customerName (text)
    private String customerName;

    @SerializedName("customerEmail")  // Match database: customerEmail (text)
    private String customerEmail;

    @SerializedName("customerPhone")  // Match database: customerPhone (text)
    private String customerPhone;

    @SerializedName("note")
    private String note;

    @SerializedName("updatedAt")  // Match database: updatedAt
    private String createdAt;// Constructors
    public Order() {}

    public Order(String id, String tableId, double total, String status) {
        this.id = id;
        this.tableId = tableId;
        this.total = total;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // Helper method to get formatted total
    public String getFormattedTotal() {
        return String.format("%.2f€", total);
    }

    // Helper method to get table display text
    public int getTableNumber() {
        if (tableId != null && tableId.startsWith("table")) {
            try {
                return Integer.parseInt(tableId.substring(5));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}