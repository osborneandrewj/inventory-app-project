package com.example.android.inventoryapp;

import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.example.android.inventoryapp.data.InventoryContract;

import java.text.NumberFormat;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRICE_CONVERSION_FACTOR = 100;

    private Uri mCurrentItemUri;
    private EditText mNameEditText;
    private EditText mPriceEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mCurrentItemUri = getIntent().getData();
        Log.v("Editor", "Uri: " + mCurrentItemUri);

        mNameEditText = (EditText)findViewById(R.id.item_name);
        mPriceEditText = (EditText)findViewById(R.id.item_price);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Projection used to perform the query
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_NAME_NAME,
                InventoryContract.InventoryEntry.COLUMN_NAME_PRICE};

        return new CursorLoader(getApplicationContext(),
                mCurrentItemUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        // Move to the first row of the cursor and read the data there
        // Note: There should be only one row in the cursor
        if (cursor.moveToFirst()) {
            int nameColIndex = cursor.getColumnIndexOrThrow(
                    InventoryContract.InventoryEntry.COLUMN_NAME_NAME);
            // Now set the data to the appropriate text fields
            mNameEditText.setText(cursor.getString(nameColIndex));

            // Convert the price int stored in the database into the currency format for display
            // The int was multiplied by 100 when it was entered to store it as cents,
            // so we undo this operation here
            int priceInt = cursor.getInt(cursor.getColumnIndexOrThrow(
                    InventoryContract.InventoryEntry.COLUMN_NAME_PRICE));
            double priceDouble = (double) priceInt / PRICE_CONVERSION_FACTOR;
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
            mPriceEditText.setText(currencyFormatter.format(priceDouble));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Reset the text fields
        mNameEditText.setText("");

    }
}
