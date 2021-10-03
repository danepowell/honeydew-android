package com.danepowell.honeydew.sync;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

import com.danepowell.honeydew.database.GroceryContract;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class GrocerySyncItem {
    public final String name;
    public final long updatedAt;
    public final int sequenceId;
    private final String section;
    private final int state;
    private final int quantity;

    private GrocerySyncItem(String name, String section, int state, int quantity, long updatedAt, int sequenceId) {
        this.name = name;
        this.section = section;
        this.state = state;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
        this.sequenceId = sequenceId;
    }

    public ParseObject getParseObject() {
        try {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
            query.whereEqualTo("name", this.name);
            ParseObject item = query.getFirst();
            item.put("name", this.name);
            item.put("section", this.section);
            item.put("state", this.state);
            item.put("quantity", this.quantity);
            return item;
        } catch (ParseException e) {
            ParseObject item = new ParseObject("Item");
            item.put("name", this.name);
            item.put("section", this.section);
            item.put("state", this.state);
            item.put("quantity", this.quantity);
            return item;
        }
    }

    @NonNull
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(GroceryContract.Item.COLUMN_NAME_NAME, this.name);
        values.put(GroceryContract.Item.COLUMN_NAME_SECTION, this.section);
        values.put(GroceryContract.Item.COLUMN_NAME_STATE, this.state);
        values.put(GroceryContract.Item.COLUMN_NAME_QUANTITY, this.quantity);
        return values;
    }

    @NonNull
    public static GrocerySyncItem fromParseObject(@NonNull ParseObject parseGroceryItem) {
        String name = parseGroceryItem.getString("name");
        String section = parseGroceryItem.getString("section");
        int state = parseGroceryItem.getInt("state");
        int quantity = parseGroceryItem.getInt("quantity");
        long updatedAt = parseGroceryItem.getUpdatedAt().getTime();
        int sequenceId = 0;
        return new GrocerySyncItem(name, section, state, quantity, updatedAt, sequenceId);
    }

    @NonNull
    public static GrocerySyncItem fromCursor(@NonNull Cursor curGroceryItem) {
        String name = curGroceryItem.getString(curGroceryItem.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));
        String section = curGroceryItem.getString(curGroceryItem.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION));
        int state = curGroceryItem.getInt(curGroceryItem.getColumnIndex(GroceryContract.Item.COLUMN_NAME_STATE));
        int quantity = curGroceryItem.getInt(curGroceryItem.getColumnIndex(GroceryContract.Item.COLUMN_NAME_QUANTITY));
        long updatedAt = 0;
        int sequenceId = curGroceryItem.getInt(curGroceryItem.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SEQUENCE));
        return new GrocerySyncItem(name, section, state, quantity, updatedAt, sequenceId);
    }

}
