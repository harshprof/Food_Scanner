package com.example.mad_final_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private TextView emailTextView, medicalConditionTextView;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailTextView = findViewById(R.id.emailTextView);
        medicalConditionTextView = findViewById(R.id.medicalConditionTextView);
        logoutButton = findViewById(R.id.logoutButton);

        loadUserInfo();

        // Logout button functionality
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(UserDashboardActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        if (currentUser != null) {
            String email = currentUser.getEmail();
            emailTextView.setText("Email: " + email);

            // Load user medical condition from Firestore
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String medicalCondition = documentSnapshot.getString("medicalCondition");
                    medicalConditionTextView.setText("Medical Condition: " + medicalCondition);
                }
            }).addOnFailureListener(e -> {
                medicalConditionTextView.setText("Medical Condition: Error loading data");
            });
        }
    }
}