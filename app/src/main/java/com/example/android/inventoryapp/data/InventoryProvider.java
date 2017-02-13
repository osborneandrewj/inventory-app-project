package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.example.android.inventoryapp.data.InventoryContract.CONTENT_AUTHORITY;

/**
 * Created by osborne on 1/31/2017.
 *
 */

public class InventoryProvider extends ContentProvider {

    /** Tag for any log messages */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the inventory table */
    private static final int INVENTORY = 100;
    /** URI matcher code for a single item in the inventory table */
    private static final int INVENTORY_ID = 101;

    /** Create and initialize an InventoryDbHelper object to gain access to the Inventory database */
    private InventoryDbHelper mInventoryDbHelper;

    /** Creates a UriMatcher object. */
    // Note: 's' means static variable
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this provider class
    static {
        /** Sets the integer value for multiple rows in the inventory table to 100 */
        sUriMatcher.addURI(CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);
        /** Sets the integer value for a single row in the inventory table to 101 */
        sUriMatcher.addURI(CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /**
     * Initialize this provider and the database helper object
     *
     * Note: A provider is not created until a ContentResolver tries to access it.
     */
    @Override
    public boolean onCreate() {
        // Create and initialize the InventoryDbHelper object
        mInventoryDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform query for the given URI. Implements ContentProvider.query()
     *
     * @return cursor for the query.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection,
                        @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        // Get the readable inventory database
        SQLiteDatabase databaseReadable = mInventoryDbHelper.getReadableDatabase();

        // Cursor to hold the result of the query
        Cursor cursor;

        // Let's figure out if the UriMatcher can actually match the given URI (uri)
        // to a specific code.
        int match = sUriMatcher.match(uri);
        // Choose the table to query
        switch (match) {
            case INVENTORY:
                // Here to user has requested the entire database. Selection and selection
                // arguments are null.
                cursor = databaseReadable.query(InventoryContract.InventoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case INVENTORY_ID:
                // Here the user is requesting a specific item
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = databaseReadable.query(InventoryContract.InventoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                // The URI was not recognized
                throw new IllegalArgumentException("Cannot query unknown URI: " + uri);
        }

        // Register to watch for changes
        // Updates the cursor if changes are detected
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Defines the MIME type of the data at the given URI.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Figure out which MIME type at the given URI
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                    return InventoryContract.InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:  
                return InventoryContract.InventoryEntry.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " +
                uri + "with match: " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        // TODO: Error handling here
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertItem(uri, contentValues);
            default:
                // We do not want this case. It would cause a database error
                throw new IllegalArgumentException("Insertion is now allowed for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case INVENTORY:
                // Here the user has requested to delete the entire inventory table
                return deleteItem(uri, selection, selectionArgs);
            case INVENTORY_ID:
                // Here the user has requested to delete a single row from
                // the inventory table
                selection = InventoryContract.InventoryEntry._ID + "=?";
                // Extract the row ID out from this URI object and set it to selectionArgs variable
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return deleteItem(uri, selection, selectionArgs);
            default:
                // We do not want this case. No URI was matched
                throw new IllegalArgumentException("Deletion failed for " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        // Get a database reference
        SQLiteDatabase database = mInventoryDbHelper.getWritableDatabase();

        int rowsAffected = database.update(InventoryContract.InventoryEntry.TABLE_NAME,
                contentValues,
                InventoryContract.InventoryEntry._ID + "=?",
                new String[] {String.valueOf(ContentUris.parseId(uri))});

        // Notify listeners that rows have been changed
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    private Uri insertItem(Uri uri, ContentValues values) {
        SQLiteDatabase database = mInventoryDbHelper.getWritableDatabase();

        long row_id = database.insert(InventoryContract.InventoryEntry.TABLE_NAME, null, values);
        // If the row_id = -1 than an error occurred
        if (row_id == -1) {
            Log.e(LOG_TAG, "Failed to insert data: " + uri);
        }

        // Notify the listener that a changes has just been made
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the new URI with new row ID of the inserted item
        return ContentUris.withAppendedId(uri, row_id);
    }

    /**
     * Deletes a row or rows in the database
     *
     * @return the number of rows affected (deleted)
     */
    private int deleteItem(Uri uri, String selection, String[] selectionArgs) {
        // Get a reference to the writable database
        SQLiteDatabase database = mInventoryDbHelper.getWritableDatabase();

        // Delete the item and
        int numberOfRowsAffected = database.delete(InventoryContract.InventoryEntry.TABLE_NAME,
                selection, selectionArgs);

        // Notify the listener that a rows has been deleted
        getContext().getContentResolver().notifyChange(uri, null);

        return numberOfRowsAffected;
    }
}
