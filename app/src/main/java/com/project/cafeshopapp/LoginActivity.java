package com.project.cafeshopapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText codeInput;
    private MaterialButton loginBtn;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        codeInput = findViewById(R.id.codeInput);
        loginBtn = findViewById(R.id.loginBtn);

        apiService = ApiClient.getClient().create(ApiService.class);

        loginBtn.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                authenticateStaff(code);
            } else {
                Toast.makeText(this, "Vui lòng nhập mã nhân viên!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateStaff(String code) {
        loginBtn.setEnabled(false);
        loginBtn.setText("Đang kiểm tra...");

        Call<List<StaffModel>> call = apiService.getStaffByCode("*", "eq." + code);
        call.enqueue(new Callback<List<StaffModel>>() {
            @Override
            public void onResponse(Call<List<StaffModel>> call, Response<List<StaffModel>> response) {
                loginBtn.setEnabled(true);
                loginBtn.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    StaffModel staff = response.body().get(0);

                    // Lưu thông tin nhân viên vào SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("staff_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("staff_name", staff.getName());
                    editor.putString("staff_code", staff.getCode());
                    editor.putString("staff_position", staff.getPosition());
                    editor.putString("staff_phone", staff.getPhone());
                    editor.apply();

                    // Chuyển đến MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Mã nhân viên không đúng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<StaffModel>> call, Throwable t) {
                loginBtn.setEnabled(true);
                loginBtn.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}