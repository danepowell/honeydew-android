package com.danepowell.honeydew.database;

import android.provider.BaseColumns;

public final class GroceryContract {
    // TODO: Can these members be package-private?
    // Database name and version.
    public static final String DATABASE_NAME = "Honeydew.db";
    public static final int DATABASE_VERSION = 1;

    // SQLite types.
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    public static abstract class Item implements BaseColumns {
        public static final String TABLE_NAME = "item";

        // Column names.
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SECTION = "section";
        public static final String COLUMN_NAME_STATE = "state";
        public static final String COLUMN_NAME_SEQUENCE = "sequence";
        public static final String COLUMN_NAME_QUANTITY = "quantity";

        // Column values.
        public static final int STATE_DELETED = 0;
        public static final int STATE_UNLISTED = 1;
        public static final int STATE_LISTED = 3;
        public static final String SECTION_UNCATEGORIZED = "Uncategorized";

        // Table create / delete.
        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_NAME_SECTION + TEXT_TYPE + "NOT NULL DEFAULT '" + SECTION_UNCATEGORIZED + "'" + COMMA_SEP
                + COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP
                + COLUMN_NAME_STATE + INT_TYPE + "NOT NULL DEFAULT " + STATE_LISTED + COMMA_SEP
                + COLUMN_NAME_SEQUENCE + INT_TYPE + "NOT NULL" + COMMA_SEP
                + COLUMN_NAME_QUANTITY + INT_TYPE + "NOT NULL DEFAULT 1" + COMMA_SEP
                + "UNIQUE(" + COLUMN_NAME_NAME + ")"
                + ");";
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

}
