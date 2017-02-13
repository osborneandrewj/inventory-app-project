package com.example.android.inventoryapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /** Used to set the item price */
    private static final int PRICE_CONVERSION_FACTOR = 100;

    /** Contains URI information about the current item */
    private Uri mCurrentItemUri;

    /** These are the EditText fields used to edit the item */
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private TextView mStockLevelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Get the item URI from the intent, if URI exists
        mCurrentItemUri = getIntent().getData();

        // Set the title for this activity based on whether or not the user
        // is creating an inventory item or editing an existing one
        if (mCurrentItemUri == null) {
            setTitle("Add Inventory Item");
        } else {
            setTitle("Edit Item");
            getSupportLoaderManager().initLoader(0, null, this);
        }

        mNameEditText = (EditText)findViewById(R.id.item_name);
        mPriceEditText = (EditText)findViewById(R.id.item_price);
        mStockLevelText = (TextView)findViewById(R.id.stock_level_text);

        // Button to increase the stock level of an item
        Button increaseStockButton = (Button)findViewById(R.id.button_increase);
        increaseStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int stockLevel = Integer.parseInt(mStockLevelText.getText().toString());
                stockLevel = stockLevel+1;
                mStockLevelText.setText(Integer.toString(stockLevel));
            }
        });

        // Button to decrease the stock level of an item
        Button decreaseStockButton = (Button)findViewById(R.id.button_decrease);
        decreaseStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int stockLevel = Integer.parseInt(mStockLevelText.getText().toString());
                if (stockLevel > 0) {
                    stockLevel = stockLevel - 1;
                } else {
                    // The stock level cannot go below 0. Inform the user of this.
                    Toast.makeText(getApplicationContext(), R.string.error_stock_level,
                            Toast.LENGTH_SHORT).show();
                }
                mStockLevelText.setText(Integer.toString(stockLevel));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Here we hide the delete menu option if this is a new entry
        if (mCurrentItemUri == null) {
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            deleteItem.setVisible(false);
        }
        return true;
    }

    /**
     * Prepare actions for user selections in the app bar menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            // User selected the "delete" button
            case R.id.action_delete:
                // something
                return true;
            // User selected the "save" button
            case R.id.action_save:
                saveItem();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Get user input and either create a new item or edit and existing one
     */
    private void saveItem() {

        // Gather user input from the edit fields
        String name = mNameEditText.getText().toString().trim();
        int price = convertPriceToInt();
        int stock = Integer.parseInt(mStockLevelText.getText().toString());

        // int which will be used to detect database changes
        int rowsAffected = 0;

        // Save input in a ContentValues object
        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_NAME_NAME, name);
        values.put(InventoryContract.InventoryEntry.COLUMN_NAME_STOCK, stock);
        values.put(InventoryContract.InventoryEntry.COLUMN_NAME_PRICE, price);

        if (mCurrentItemUri == null) {
            // User is creating a new item
            getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI,
                    values);
            // Manually update the rowsAffected int to reflect a database change
            rowsAffected = 1;
        } else {
            // User is editing an existing item
            rowsAffected = getContentResolver().update(mCurrentItemUri, values,
                    null, null);
        }

        String toastMessage;
        if (rowsAffected == 0) {
            toastMessage = "Error editing item";
        } else {
            toastMessage = "Item saved";
        }
        Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Convert the currency-formatted price EditText to a database-ready int
     * @return the price as an int
     */
    private int convertPriceToInt() {

        // Get the value from the EditText. This will have a "$#.##" format
        // eg. "$1.05
        String priceString = mPriceEditText.getText().toString();
        // Remove the "$" from the String
        priceString = priceString.replace("$", "");
        // Convert the string into a double and multiply by 100 to convert from dollars to cents
        Double priceDouble = Double.parseDouble(priceString) * PRICE_CONVERSION_FACTOR;
        // Use the Double class to convert this number into an int
        int priceInt = priceDouble.intValue();

        Log.v("Editor", "price: " + priceInt);
        return priceInt;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Projection used to perform the query
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_NAME_NAME,
                InventoryContract.InventoryEntry.COLUMN_NAME_PRICE,
                InventoryContract.InventoryEntry.COLUMN_NAME_STOCK};

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

            // Set the stock level TextView
            mStockLevelText.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(
                    InventoryContract.InventoryEntry.COLUMN_NAME_STOCK))));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Reset the text fields
        mNameEditText.setText("");

    }
}
