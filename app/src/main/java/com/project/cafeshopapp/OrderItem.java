package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class OrderItem {
    @SerializedName("id")
    private String id;

    @SerializedName("orderId") // Using camelCase as per the database schema
    private String orderId;

    @SerializedName("title")
    private String title;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private double price;

    @SerializedName("size")
    private String size;

    @SerializedName("milk")
    private String milk;

    @SerializedName("note")
    private String note;

    // Legacy field for backward compatibility
    private String name;

    // Default constructor for Gson
    public OrderItem() {
    }

    // Constructor for legacy code
    public OrderItem(String name, String note, int quantity) {
        this.name = name;
        this.title = name; // Map legacy name to title
        this.note = note;
        this.quantity = quantity;
    }

    // Complete constructor
    public OrderItem(String id, String orderId, String title, int quantity, double price, String size, String milk,
            String note) {
        this.id = id;
        this.orderId = orderId;
        this.title = title;
        this.name = title; // For backward compatibility
        this.quantity = quantity;
        this.price = price;
        this.size = size;
        this.milk = milk;
        this.note = note;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.name = title; // Keep name synchronized with title
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getMilk() {
        return milk;
    }

    public void setMilk(String milk) {
        this.milk = milk;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // Legacy getter for backward compatibility
    public String getName() {
        return title != null ? title : name;
    }

    // Legacy setter for backward compatibility
    public void setName(String name) {
        this.name = name;
        this.title = name; // Keep title synchronized with name
    }
}