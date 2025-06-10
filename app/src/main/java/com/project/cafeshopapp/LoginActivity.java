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
        setupClickListeners();

        // Kiểm tra xem user đã đăng nhập chưa
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
                Toast.makeText(this, "Vui lòng nhập mã nhân viên!", Toast.LENGTH_SHORT).show();
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
        setLoadingState(true);

        // 🔑 CHỈ SỬ DỤNG DEMO CODES - KHÔNG CẦN API
        if (isValidStaffCode(code)) {
            Log.d(TAG, "Valid staff code entered: " + code);
            handleStaffLogin(code);
        } else {
            setLoadingState(false);
            Toast.makeText(this, "Mã nhân viên không hợp lệ!\n\nMã hợp lệ: 1234, admin, 0001, manager, staff, cashier", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidStaffCode(String code) {
        // 🎯 ĐỊNH NGHĨA CÁC MÃ NHÂN VIÊN HỢP LỆ
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
            String staffPhone;

            // 👤 THÔNG TIN NHÂN VIÊN DỰA TRÊN MÃ
            switch (code) {
                case "1234":
                    staffName = "Nguyễn Văn Demo";
                    staffPosition = "Nhân viên phục vụ";
                    staffPhone = "0123456789";
                    break;
                case "admin":
                    staffName = "Quản lý Admin";
                    staffPosition = "Quản lý cửa hàng";
                    staffPhone = "0987654321";
                    break;
                case "0001":
                    staffName = "Lê Thị Test";
                    staffPosition = "Thu ngân";
                    staffPhone = "0369258147";
                    break;
                case "manager":
                    staffName = "Trần Văn Quản Lý";
                    staffPosition = "Quản lý ca";
                    staffPhone = "0901234567";
                    break;
                case "staff":
                    staffName = "Phạm Thị Nhân Viên";
                    staffPosition = "Nhân viên phục vụ";
                    staffPhone = "0912345678";
                    break;
                case "cashier":
                    staffName = "Hoàng Văn Thu Ngân";
                    staffPosition = "Thu ngân";
                    staffPhone = "0923456789";
                    break;
                default:
                    staffName = "Nhân viên";
                    staffPosition = "Nhân viên";
                    staffPhone = "0000000000";
                    break;
            }

            // 💾 Lưu thông tin nhân viên
            saveStaffInfo(code, staffName, staffPosition, staffPhone);

            // ✅ Hiển thị thông báo thành công
            Toast.makeText(this, "Đăng nhập thành công! Chào mừng " + staffName, Toast.LENGTH_SHORT).show();

            // 🏠 Chuyển đến MainActivity
            navigateToMainActivity();

        } catch (Exception e) {
            Log.e(TAG, "Error in staff login: " + e.getMessage(), e);
            setLoadingState(false);
            Toast.makeText(this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Lỗi chuyển trang: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginBtn.setEnabled(false);
            loginBtn.setText("Đang kiểm tra...");
            codeInput.setEnabled(false);
        } else {
            loginBtn.setEnabled(true);
            loginBtn.setText("Đăng nhập");
            codeInput.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation or just finish app
        Toast.makeText(this, "Nhấn lại để thoát ứng dụng", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
}