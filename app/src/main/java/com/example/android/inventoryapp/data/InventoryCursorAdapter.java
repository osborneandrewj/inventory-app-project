package com.example.android.inventoryapp.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.R;

/**
 * Created by Zark on 2/3/2017.
 *
 * This is an adapter for a list which uses the inventory database as it's data source.
 */

public class InventoryCursorAdapter extends CursorAdapter {


    /**
     * Constructs a new InventoryCursorAdapter
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. Note: No data is set (or bound) to the views at this point.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
    }

    /**
     * Binds the data to the given list item layout.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in the inflated view
        TextView name = (TextView)view.findViewById(R.id.name);
        TextView price = (TextView)view.findViewById(R.id.price);
        // Extract properties from cursor
        String nameString = cursor.getString(cursor.getColumnIndexOrThrow(
                InventoryContract.InventoryEntry.COLUMN_NAME_NAME));
        String priceString = cursor.getString(cursor.getColumnIndexOrThrow(
                InventoryContract.InventoryEntry.COLUMN_NAME_PRICE));

        // Populate the fields using the extracted data
        name.setText(nameString);
        price.setText(priceString);
    }
}
