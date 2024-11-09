package com.example.mad_final_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListView scannedItemsList;
    private Button cameraButton;
    private ScannedItemsAdapter adapter;
    private ArrayList<Product> scannedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        scannedItemsList = findViewById(R.id.scanItemsList);
        cameraButton = findViewById(R.id.cameraButton);

        scannedItems = new ArrayList<>();
        adapter = new ScannedItemsAdapter(this, scannedItems);
        scannedItemsList.setAdapter(adapter);

        findViewById(R.id.userLogo).setOnClickListener(v -> navigateToUserDashboard());
        cameraButton.setOnClickListener(v -> openCamera());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserScannedProducts();
    }

    private void fetchUserScannedProducts() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("productScans")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            scannedItems.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Product product = document.toObject(Product.class);
                                scannedItems.add(product);
                            }
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Fetched " + scannedItems.size() + " products for user: " + userId);
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFirestoreException &&
                                    ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                                Log.w(TAG, "Index not yet ready. Fetching without ordering.");

                                fetchUserScannedProductsWithoutOrdering(userId);
                            } else {
                                Log.w(TAG, "Error getting documents.", e);
                                Toast.makeText(HomeActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserScannedProductsWithoutOrdering(String userId) {
        db.collection("productScans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        scannedItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            scannedItems.add(product);
                        }

                        scannedItems.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Fetched " + scannedItems.size() + " products for user: " + userId);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(HomeActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToUserDashboard() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            startActivity(new Intent(HomeActivity.this, UserDashboardActivity.class));
        } else {
            Toast.makeText(HomeActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        startActivity(new Intent(this, CameraActivity.class));
    }
}