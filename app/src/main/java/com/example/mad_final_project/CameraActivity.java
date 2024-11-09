package com.example.mad_final_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final String TAG = "CameraActivity";
    private static final long SCAN_DELAY_MS = 4000;

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private ExecutorService cameraExecutor;
    private String lastScannedBarcode = null;
    private long lastScanTime = 0;
    private Map<String, Map<String, Object>> medicalConditions = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_f);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }

        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build());

        loadMedicalConditions();
    }

    private void loadMedicalConditions() {
        FirebaseFirestore.getInstance().collection("medicalConditions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        medicalConditions.put(document.getId(), document.getData());
                    }
                    Log.d(TAG, "Loaded medical conditions: " + medicalConditions);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading medical conditions", e));
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String barcodeValue = barcode.getRawValue();
                        if (barcodeValue != null && isUniqueScan(barcodeValue)) {
                            runOnUiThread(() -> processBarcode(barcodeValue));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed: ", e))
                .addOnCompleteListener(task -> image.close());
    }

    private boolean isUniqueScan(String barcodeValue) {
        long currentTime = System.currentTimeMillis();
        boolean isNewScan = !barcodeValue.equals(lastScannedBarcode) || (currentTime - lastScanTime) > SCAN_DELAY_MS;
        if (isNewScan) {
            lastScannedBarcode = barcodeValue;
            lastScanTime = currentTime;
        }
        return isNewScan;
    }

    private void processBarcode(String barcodeValue) {
        String apiUrl = "https://world.openfoodfacts.org/api/v0/product/" + barcodeValue + ".json";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                response -> handleApiResponse(response, barcodeValue),
                error -> Toast.makeText(CameraActivity.this, "Error fetching product data", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void handleApiResponse(JSONObject response, String barcodeValue) {
        try {
            if (response.has("product")) {
                JSONObject product = response.getJSONObject("product");
                String productName = product.optString("product_name", "Unknown product");
                JSONObject nutriments = product.getJSONObject("nutriments");
                String sugarContent = nutriments.optString("sugars_100g", "0");
                String sodiumContent = nutriments.optString("sodium_100g", "0");
                processProductInfo(barcodeValue, productName, sugarContent, sodiumContent, response);
            } else {
                Toast.makeText(CameraActivity.this, "Product not found in database", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: ", e);
        }
    }

    private void processProductInfo(String barcode, String productName, String sugarContent, String sodiumContent, JSONObject fullProductData) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            checkMedicalConditions(productName, sugarContent, sodiumContent, fullProductData);
            saveProductScan(barcode, productName);
        } else {
            Toast.makeText(this, "Please sign in to save product scans", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkMedicalConditions(String productName, String sugarContent, String sodiumContent, JSONObject fullProductData) {
        StringBuilder warningMessages = new StringBuilder();
        boolean hasWarnings = false;

        for (Map.Entry<String, Map<String, Object>> entry : medicalConditions.entrySet()) {
            String conditionName = entry.getKey();
            Map<String, Object> conditionData = entry.getValue();

            if ("diabetes".equalsIgnoreCase(conditionName)) {
                hasWarnings |= checkDiabetes(productName, sugarContent, conditionData, warningMessages);
            } else if ("hypertension".equalsIgnoreCase(conditionName)) {
                hasWarnings |= checkHypertension(productName, sodiumContent, conditionData, warningMessages);
            }
        }

        if (hasWarnings) {
            showWarningDialog(warningMessages.toString(), fullProductData);
        } else {
            String okToEatMessage = "This product is within acceptable limits for your medical conditions.";
            showProductInfo(productName, sugarContent, sodiumContent, fullProductData, okToEatMessage);
        }
    }

    private boolean checkDiabetes(String productName, String sugarContent, Map<String, Object> conditionData, StringBuilder warningMessages) {
        double sugarLimit = (double) conditionData.get("sugarLimit");
        String warningMessageTemplate = (String) conditionData.get("warningMessage");

        if (Double.parseDouble(sugarContent) > sugarLimit) {
            String formattedWarningMessage = warningMessageTemplate
                    .replace("{productName}", productName)
                    .replace("{sugarContent}", sugarContent);
            warningMessages.append(formattedWarningMessage).append("\n\n");
            return true;
        }
        return false;
    }

    private boolean checkHypertension(String productName, String sodiumContent, Map<String, Object> conditionData, StringBuilder warningMessages) {
        double sodiumLimit = (double) conditionData.get("sodiumLimit");
        String warningMessageTemplate = (String) conditionData.get("warningMessage");

        if (Double.parseDouble(sodiumContent) > sodiumLimit) {
            String formattedWarningMessage = warningMessageTemplate
                    .replace("{productName}", productName)
                    .replace("{sodiumContent}", sodiumContent);
            warningMessages.append(formattedWarningMessage).append("\n\n");
            return true;
        }
        return false;
    }

    private void showWarningDialog(String warningMessages, JSONObject fullProductData) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage(warningMessages)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setNeutralButton("Details", (dialog, which) -> showFullProductDetails(fullProductData))
                .setCancelable(false)
                .show();
    }

    private void showProductInfo(String productName, String sugarContent, String sodiumContent, JSONObject fullProductData, String additionalMessage) {
        String message = String.format("Product: %s\nSugar Content: %sg per 100g\nSodium Content: %sg per 100g\n\n%s",
                productName, sugarContent, sodiumContent, additionalMessage);

        new AlertDialog.Builder(this)
                .setTitle("Product Information")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setNeutralButton("Details", (dialog, which) -> showFullProductDetails(fullProductData))
                .show();
    }

    private void showFullProductDetails(JSONObject fullProductData) {
        StringBuilder details = new StringBuilder();
        try {
            JSONObject product = fullProductData.getJSONObject("product");

            String allergens = product.optString("allergens", "No allergen information available");
            details.append("Allergens: ").append(allergens).append("\n\n");

            JSONObject nutriments = product.optJSONObject("nutriments");
            if (nutriments != null) {
                details.append("Nutrition Data:\n");
                appendNutrimentIfPresent(details, nutriments, "energy", "Energy");
                appendNutrimentIfPresent(details, nutriments, "carbohydrates", "Carbohydrates");
                appendNutrimentIfPresent(details, nutriments, "fat", "Fat");
                appendNutrimentIfPresent(details, nutriments, "proteins", "Proteins");
                appendNutrimentIfPresent(details, nutriments, "salt", "Salt");
                appendNutrimentIfPresent(details, nutriments, "sodium", "Sodium");
                appendNutrimentIfPresent(details, nutriments, "sugars", "Sugars");
                appendNutrimentIfPresent(details, nutriments, "fiber", "Fiber");
            } else {
                details.append("No nutrition data available");
            }

            String nutriscoreGrade = product.optString("nutriscore_grade", "Not available");
            details.append("\nNutri-Score: ").append(nutriscoreGrade.toUpperCase());

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing full product data", e);
            details.append("Error parsing product data");
        }

        new AlertDialog.Builder(this)
                .setTitle("Product Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void appendNutrimentIfPresent(StringBuilder details, JSONObject nutriments, String key, String label) throws JSONException {
        if (nutriments.has(key)) {
            details.append(label).append(": ")
                    .append(nutriments.get(key))
                    .append(nutriments.optString(key + "_unit", ""))
                    .append("\n");
        }
    }

    private void saveProductScan(String barcode, String productName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> scan = new HashMap<>();
            scan.put("barcode", barcode);
            scan.put("productName", productName);
            scan.put("timestamp", FieldValue.serverTimestamp());
            scan.put("userId", userId);

            db.collection("productScans")
                    .add(scan)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Product scan saved successfully with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding product scan", e));
        } else {
            Log.e(TAG, "No user is signed in");
            Toast.makeText(this, "Please sign in to save product scans", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}