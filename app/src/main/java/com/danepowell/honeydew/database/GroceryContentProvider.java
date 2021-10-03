package com.danepowell.honeydew.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

public class GroceryContentProvider extends ContentProvider {

    @Nullable
    private GroceryDbHelper mDatabase;

    private static final int ITEMS = 10;
    private static final int ITEM_ID = 20;
    private static final int DISTINCT = 30;

    public static final String AUTHORITY = "com.danepowell.honeydew.ContentProvider";

    private static final String BASE_PATH_ITEM = "item";

    public static final Uri CONTENT_URI_ITEM = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_ITEM);
    public static final Uri CONTENT_URI_ITEM_DISTINCT = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_ITEM + "/distinct");

    public static final String CONTENT_ITEM_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + BASE_PATH_ITEM;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ITEM, ITEMS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ITEM + "/#", ITEM_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ITEM + "/distinct", DISTINCT);
    }

    @Override
    public boolean onCreate() {
        mDatabase = new GroceryDbHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int uriType = sURIMatcher.match(uri);
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(GroceryContract.Item.TABLE_NAME);
        String groupBy = null;

        switch (uriType) {
            case ITEMS:
                break;
            case ITEM_ID:
                queryBuilder.appendWhere(GroceryContract.Item._ID + "="
                        + uri.getLastPathSegment());
                break;
            case DISTINCT:
                queryBuilder.setDistinct(true);
                groupBy = GroceryContract.Item.COLUMN_NAME_SECTION;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (mDatabase == null) {
            return null;
        }
        SQLiteDatabase db = mDatabase.getWritableDatabase();

        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, groupBy, null, sortOrder);

        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                cursor.setNotificationUri(contentResolver, uri);
            }
        }
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        Uri _uri = null;
        if (values == null) {
            values = new ContentValues();
        }
        values.put("sequence", getSequenceId() + 1);

        if (mDatabase == null) {
            return null;
        }
        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
        long id;
        if (uriType == ITEMS) {
            id = insertOrUpdate(sqlDB, uri, values);
            if (id > 0) {
                _uri = ContentUris.withAppendedId(CONTENT_URI_ITEM, id);
            }
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return _uri;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        if (mDatabase == null) {
            return 0;
        }
        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
        int rowsUpdated;
        if (values == null) {
            values = new ContentValues();
        }
        values.put(GroceryContract.Item.COLUMN_NAME_SEQUENCE, getSequenceId() + 1);
        String id;
        switch (uriType) {
            case ITEMS:
                rowsUpdated = sqlDB.update(GroceryContract.Item.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case ITEM_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(GroceryContract.Item.TABLE_NAME, values,
                            GroceryContract.Item._ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(GroceryContract.Item.TABLE_NAME, values,
                            GroceryContract.Item._ID + "=" + id + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        if (mDatabase == null) {
            return 0;
        }
        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
        int rowsDeleted;
        String id;
        ContentValues values = new ContentValues();
        values.put(GroceryContract.Item.COLUMN_NAME_SEQUENCE, getSequenceId() + 1);
        values.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_DELETED);
        values.put(GroceryContract.Item.COLUMN_NAME_SECTION, GroceryContract.Item.SECTION_UNCATEGORIZED);
        switch (uriType) {
            case ITEMS:
                rowsDeleted = sqlDB.update(GroceryContract.Item.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case ITEM_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.update(GroceryContract.Item.TABLE_NAME, values,
                            GroceryContract.Item._ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.update(GroceryContract.Item.TABLE_NAME, values,
                            GroceryContract.Item._ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return rowsDeleted;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    /**
     * Try to insert an item, or update the item if it already exists.
     * @param db The database instance to connect to.
     * @param uri The URI to use.
     * @param values Values to insert or update.
     * @return ID of created or updated row.
     */
    private long insertOrUpdate(@NonNull SQLiteDatabase db, @NonNull Uri uri, @NonNull ContentValues values) {
        long id = 0;
        try {
            id = db.insertOrThrow(GroceryContract.Item.TABLE_NAME, null, values);
        } catch (SQLiteConstraintException e) {
            String selection = GroceryContract.Item.COLUMN_NAME_NAME + "=?";
            String[] selectionArgs = new String[]{values.getAsString(GroceryContract.Item.COLUMN_NAME_NAME)};
            String[] projection = new String[]{GroceryContract.Item._ID};
            int nrRows = update(uri, values, selection, selectionArgs);
            if (nrRows != 1)
                throw e;
            Cursor cursor = query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                id = cursor.getLong(cursor.getColumnIndex(GroceryContract.Item._ID));
                cursor.close();
            }
        }
        return id;
    }

    private int getSequenceId() {
        int lastSequenceId = 0;
        Cursor cursor = query(GroceryContentProvider.CONTENT_URI_ITEM, new String[]{"MAX(sequence) AS max_sequence"}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                lastSequenceId = cursor.getInt(cursor.getColumnIndex("max_sequence"));
            }
            cursor.close();
        }
        return lastSequenceId;
    }

    private void notifyChange(@NonNull Uri uri) {
        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                contentResolver.notifyChange(uri, null);
            }
        }
    }
}
