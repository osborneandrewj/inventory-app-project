package com.example.android.inventoryapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    /** Used for logging messages */
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    /** Used to set the item price */
    private static final int PRICE_CONVERSION_FACTOR = 100;

    /** URI information about the current item */
    private Uri mCurrentItemUri;

    /** URI for the item image */
    private Uri mCurrentItemImageUri;

    /** Used to setting image dimensions */
    private static final int STANDARD_IMAGE_DIMENSION = 500;

    /** The "request code" for identifying the image selection request */
    private static final int PICK_IMAGE_REQUEST = 1;

    /** Used for getting permissions */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /** These are the fields used to edit the item */
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private TextView mStockLevelText;
    private View mSelectImageView;
    private ImageView mItemImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);



        mNameEditText = (EditText)findViewById(R.id.item_name);
        mPriceEditText = (EditText)findViewById(R.id.item_price);
        mStockLevelText = (TextView)findViewById(R.id.stock_level_text);
        mItemImage = (ImageView)findViewById(R.id.item_image);

        // Button to increase the stock level of an item
        Button increaseStockButton = (Button)findViewById(R.id.button_increase);
        increaseStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int stockLevel = Integer.parseInt(mStockLevelText.getText().toString());
                stockLevel = stockLevel + 1;
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

        // Button to create an order for more product
        // This should be handled by an email client
        Button orderButton = (Button)findViewById(R.id.button_order);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this intent
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                        // TODO: enter a valid email address for supplier
                        "supplier@example.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        R.string.email_subject);
                intent.putExtra(Intent.EXTRA_TEXT,
                        R.string.email_body);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        // Button to select an image to display
        mSelectImageView = findViewById(R.id.select_image_view);
        mSelectImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        // Get the item URI from the intent, if URI exists
        mCurrentItemUri = getIntent().getData();

        // Set the title for this activity based on whether or not the user
        // is creating an inventory item or editing an existing one
        if (mCurrentItemUri == null) {
            setTitle(R.string.title_editor_add);
        } else {
            setTitle(R.string.title_editor_edit);
            getSupportLoaderManager().initLoader(0, null, this);
        }
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
                showDeleteConfirmationDialog();
                return true;
            // User selected the "save" button
            case R.id.action_save:
                if (validateData()) {
                    saveItem();
                    finish();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show the delete confirmation dialog box
     */
    private void showDeleteConfirmationDialog() {
        // Instantiate the dialog builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Chain together setter methods to set the dialog characteristics
        builder.setMessage(R.string.delete_button_message);
        builder.setPositiveButton(R.string.delete_button_positive,
                // User clicked the "delete" button. Delete the item
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteItem();
                        finish();
                    }
                });
        builder.setNegativeButton(R.string.delete_button_negative,
                // User clicked cancel. Do nothing and return to activity
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       // Do nothing
                    }
                });
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

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
        if (mCurrentItemImageUri != null) {
            values.put(InventoryContract.InventoryEntry.COLUMN_NAME_IMAGE, mCurrentItemImageUri.toString());
        }
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
            toastMessage = getString(R.string.error_saving_item);
        } else {
            toastMessage = getString(R.string.item_saved_success);
        }
        Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Convert the currency-formatted price EditText to a database-ready int
     *
     * @return the price as an int
     */
    private int convertPriceToInt() {

        // Get the value from the EditText. This will have a "$#.##" format
        // eg. "$1.05
        String priceString = mPriceEditText.getText().toString();
        if (!TextUtils.isEmpty(priceString)) {
            // Remove the "$" from the String
            priceString = priceString.replace("$", "");
            // Convert the string into a double and multiply by 100 to convert from dollars to cents
            Double priceDouble = Double.parseDouble(priceString) * PRICE_CONVERSION_FACTOR;
            // Use the Double class to convert this number into an int
            int priceInt = priceDouble.intValue();
            return priceInt;
        } else {
            return 0;
        }
    }

    /**
     * Check the user input fields to ensure that they are not blank. This method is usually called
     * before the data is saved.
     *
     * @return true is the data is ready for database entry
     */
    private boolean validateData() {
        // Validate the data to ensure no blanks are left
        if (TextUtils.isEmpty(mNameEditText.getText())) {
            Toast.makeText(getApplicationContext(), R.string.error_no_name,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(mPriceEditText.getText())) {
            Toast.makeText(getApplicationContext(), R.string.error_no_name,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        // If all checks out, return true
        return true;
    }

    /**
     * Delete a row (item) from the database
     */
    private void deleteItem(){

        long rowsAffected = getContentResolver().delete(mCurrentItemUri, null, null);

        if (rowsAffected != -1) {
            // Item deleted successfully
            Toast.makeText(this, R.string.item_deleted_success, Toast.LENGTH_SHORT).show();
        } else {
            // Something went wrong with the deletion
            Toast.makeText(this, R.string.error_item_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open an image gallery with correct permissions and allow user to select and image
     */
    private void chooseImage(){

        // Build the intent
        Intent intent = new Intent();
        // For API level 19+ we need to allow persistable URI permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Only allow the user to see images
        intent.setType("image/*");

        // Use startActivityForResult() instead of startActivity() because we want to get
        // a result back from the activity (the image URI)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    /**
     * Set a selected image to the image view in the editor
     */
    private void setSelectedImageToItem() {

        // Set the image from the content URI
        mItemImage.setImageBitmap(getBitmapFromUri(mCurrentItemImageUri));

        // Now hide the "Select an image" text
        TextView selectPrompt = (TextView)findViewById(R.id.select_an_image_text);
        selectPrompt.setVisibility(View.INVISIBLE);
    }

    /**
     * Get a bitmap image from provided URI
     *
     * Note: This method is from: https://github.com/crlsndrsjmnz
     */
    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mSelectImageView.getWidth();
        if (targetW == 0) {
            targetW = STANDARD_IMAGE_DIMENSION; // Do not allow this value to be 0
        }
        int targetH = mSelectImageView.getHeight();
        if (targetH == 0) {
            targetH = STANDARD_IMAGE_DIMENSION; // Do not allow this value to be 0
        }

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            Log.v(LOG_TAG, "targets: " + targetW + " " + targetH + " "
            + photoW + " " + photoH);

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
            Log.v(LOG_TAG, "scaleFactor: " + scaleFactor);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    /**
     * Capture the user selected image URI
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we are responding to:
        if (requestCode == PICK_IMAGE_REQUEST) {
            // Check that the request was successful
            if (resultCode == RESULT_OK) {

                mCurrentItemImageUri = data.getData();
                // Set persistent permission for this URI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    ContentResolver resolver = this.getContentResolver();
                    resolver.takePersistableUriPermission(mCurrentItemImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                // If the request was successful, store the URI in the database
                setSelectedImageToItem();
            }
        }
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Projection used to perform the query
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_NAME_NAME,
                InventoryContract.InventoryEntry.COLUMN_NAME_PRICE,
                InventoryContract.InventoryEntry.COLUMN_NAME_STOCK,
                InventoryContract.InventoryEntry.COLUMN_NAME_IMAGE};

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

            // Set the image if a uri is stored in the database
            if (cursor.getString(cursor.getColumnIndexOrThrow(
                    InventoryContract.InventoryEntry.COLUMN_NAME_IMAGE)) != null) {
                mCurrentItemImageUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(
                        InventoryContract.InventoryEntry.COLUMN_NAME_IMAGE)));
                mItemImage = (ImageView)findViewById(R.id.item_image);
                mItemImage.setImageBitmap(getBitmapFromUri(mCurrentItemImageUri));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Reset the text fields
        mNameEditText.setText("");

    }
}
