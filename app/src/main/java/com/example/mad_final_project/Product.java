package com.example.mad_final_project;
import com.google.firebase.Timestamp;

public class Product {
    private String productName;
    private String barcode;
    private String userId;
    private Timestamp timestamp;


    public Product() {}

    public Product(String productName, String barcode, String userId, Timestamp timestamp) {
        this.productName = productName;
        this.barcode = barcode;
        this.userId = userId;
        this.timestamp = timestamp;
    }


    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
