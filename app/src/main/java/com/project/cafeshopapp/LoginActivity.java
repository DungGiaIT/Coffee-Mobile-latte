package com.project.cafeshopapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private TextInputEditText codeInput;
    private MaterialButton loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupClickListeners(); // Check if user is already logged in
        checkIfAlreadyLoggedIn();
    }

    private void initViews() {
        codeInput = findViewById(R.id.codeInput);
        loginBtn = findViewById(R.id.loginBtn);
    }

    private void setupClickListeners() {
        loginBtn.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                authenticateStaff(code);
            } else {
                Toast.makeText(this, "Please enter your staff ID!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfAlreadyLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
        String staffCode = prefs.getString("staff_code", "");

        if (!staffCode.isEmpty()) {
            // User already logged in, go to MainActivity
            navigateToMainActivity();
        }
    }

    private void authenticateStaff(String code) {
        // Set loading state
        setLoadingState(true); // 🔑 ONLY USE DEMO CODES - NO API NEEDED
        if (isValidStaffCode(code)) {
            Log.d(TAG, "Valid staff code entered: " + code);
            handleStaffLogin(code);
        } else {
            setLoadingState(false);
            Toast.makeText(this, "Invalid staff ID!\n\nValid codes: 1234, admin, 0001, manager, staff, cashier",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidStaffCode(String code) {
        // 🎯 DEFINE VALID STAFF CODES
        return code.equals("1234") ||
                code.equals("admin") ||
                code.equals("0001") ||
                code.equals("manager") ||
                code.equals("staff") ||
                code.equals("cashier");
    }

    private void handleStaffLogin(String code) {
        try {
            String staffName;
            String staffPosition;
            String staffPhone; // 👤 STAFF INFORMATION BASED ON CODE
            switch (code) {
                case "1234":
                    staffName = "John Demo";
                    staffPosition = "Service Staff";
                    staffPhone = "0123456789";
                    break;
                case "admin":
                    staffName = "Admin Manager";
                    staffPosition = "Store Manager";
                    staffPhone = "0987654321";
                    break;
                case "0001":
                    staffName = "Sarah Test";
                    staffPosition = "Cashier";
                    staffPhone = "0369258147";
                    break;
                case "manager":
                    staffName = "Michael Manager";
                    staffPosition = "Shift Manager";
                    staffPhone = "0901234567";
                    break;
                case "staff":
                    staffName = "Emma Staff";
                    staffPosition = "Service Staff";
                    staffPhone = "0912345678";
                    break;
                case "cashier":
                    staffName = "Robert Cashier";
                    staffPosition = "Cashier";
                    staffPhone = "0923456789";
                    break;
                default:
                    staffName = "Staff";
                    staffPosition = "Staff";
                    staffPhone = "0000000000";
                    break;
            } // 💾 Save staff information
            saveStaffInfo(code, staffName, staffPosition, staffPhone);

            // ✅ Show success message
            Toast.makeText(this, "Login successful! Welcome " + staffName, Toast.LENGTH_SHORT).show();

            // 🏠 Navigate to MainActivity
            navigateToMainActivity();

        } catch (Exception e) {
            Log.e(TAG, "Error in staff login: " + e.getMessage(), e);
            setLoadingState(false);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveStaffInfo(String code, String name, String position, String phone) {
        try {
            SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("staff_code", code);
            editor.putString("staff_name", name);
            editor.putString("staff_position", position);
            editor.putString("staff_phone", phone);
            editor.putLong("login_time", System.currentTimeMillis());
            editor.apply();

            Log.d(TAG, "Staff info saved: " + name + " (" + code + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error saving staff info: " + e.getMessage(), e);
        }
    }

    private void navigateToMainActivity() {
        try {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

            // Add smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to MainActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Page navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginBtn.setEnabled(false);
            loginBtn.setText("Checking...");
            codeInput.setEnabled(false);
        } else {
            loginBtn.setEnabled(true);
            loginBtn.setText("Login");
            codeInput.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation or just finish app
        Toast.makeText(this, "Press again to exit the application", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
}