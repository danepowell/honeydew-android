package com.danepowell.honeydew.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

class GroceryDbHelper extends SQLiteOpenHelper {

    public GroceryDbHelper(Context context) {
        super(context, GroceryContract.DATABASE_NAME, null, GroceryContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase database) {
        database.execSQL(GroceryContract.Item.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        database.execSQL(GroceryContract.Item.DELETE_TABLE);
        onCreate(database);
    }
}
