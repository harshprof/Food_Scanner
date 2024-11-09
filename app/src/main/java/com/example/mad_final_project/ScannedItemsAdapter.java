package com.example.mad_final_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ScannedItemsAdapter extends ArrayAdapter<Product> {

    public ScannedItemsAdapter(Context context, List<Product> products) {
        super(context, 0, products);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_product, parent, false);
        }

        Product product = getItem(position);

        TextView productNameText = convertView.findViewById(R.id.productNameText);
        TextView productBarcodeText = convertView.findViewById(R.id.productBarcodeText);
        TextView productTimestampText = convertView.findViewById(R.id.productTimestampText);

        productNameText.setText(product.getProductName());
        productBarcodeText.setText(product.getBarcode());
        productTimestampText.setText(product.getTimestamp().toDate().toString());

        return convertView;
    }
}
