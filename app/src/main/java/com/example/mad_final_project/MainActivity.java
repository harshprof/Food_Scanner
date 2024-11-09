package com.example.mad_final_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        SharedPreferences prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (currentUser != null && isLoggedIn) {

            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        } else {

            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
            finish();
        }
    }
}
