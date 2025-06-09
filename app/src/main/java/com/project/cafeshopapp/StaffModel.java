package com.project.cafeshopapp;

import com.google.gson.annotations.SerializedName;

public class StaffModel {
    @SerializedName("id")
    private int id;

    @SerializedName("code")
    private String code;

    @SerializedName("name")
    private String name;

    @SerializedName("position")
    private String position;

    @SerializedName("phone")
    private String phone;

    // Constructors
    public StaffModel() {}

    public StaffModel(int id, String code, String name, String position, String phone) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.position = position;
        this.phone = phone;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
