package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by osborne on 1/31/2017.
 */

public final class InventoryContract {

    /**
     * String for the content authority. Value is same as AndroidManifest.xml file.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";

    /** This URI will be used by URIs associated with the InventoryContract */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /** This path is for the entire inventory database */
    public static final String PATH_INVENTORY = "inventory";

    // This class should never be instantiated
    private InventoryContract() {}

    public static class InventoryEntry implements BaseColumns {

        /** Content URI to access the inventory data */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

        /** The MIME type of the CONTENT_URI for a list of inventory items */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" +
                        PATH_INVENTORY;

        /** The MIME type of the CONTENT_URI for an individual inventory item */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" +
                        PATH_INVENTORY;

        /** Name of database table for inventory items */
        public static final String TABLE_NAME = "inventory";

        /** Names of each column in the table. Note: _ID is assumed already by BaseColumns */
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PRICE = "price";
        public static final String COLUMN_NAME_STOCK = "stock";
        public static final String COLUMN_NAME_IMAGE = "image";

        /** Used to create the table */
        public static final String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " +
                InventoryEntry.TABLE_NAME + " (" +
                InventoryEntry._ID + " INTEGER PRIMARY KEY," +
                InventoryEntry.COLUMN_NAME_NAME + " TEXT NOT NULL," +
                InventoryEntry.COLUMN_NAME_IMAGE + " BLOB," +
                InventoryEntry.COLUMN_NAME_PRICE + " TEXT NOT NULL," +
                InventoryEntry.COLUMN_NAME_STOCK + " INTEGER DEFAULT 0)";

        /** Used to delete the table */
        public static final String SQL_DELETE_INVENTORY_TABLE =
                "DROP TABLE IF EXISTS " + InventoryEntry.TABLE_NAME;
    }
}
