package com.danepowell.honeydew.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.view.View;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.activity.ItemEditActivity;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;
import com.danepowell.honeydew.helpers.RecyclerListFragment;

public class ItemListFragment extends RecyclerListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (sharedPref.getBoolean("first_run", true)) {
            ContentValues values = new ContentValues();
            values.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_LISTED);
            values.put(GroceryContract.Item.COLUMN_NAME_SECTION, "Produce");
            values.put(GroceryContract.Item.COLUMN_NAME_NAME, "apples");
            getActivity().getContentResolver().insert(GroceryContentProvider.CONTENT_URI_ITEM, values);
            values.put(GroceryContract.Item.COLUMN_NAME_NAME, "bananas");
            getActivity().getContentResolver().insert(GroceryContentProvider.CONTENT_URI_ITEM, values);
            values.put(GroceryContract.Item.COLUMN_NAME_NAME, "carrots");
            values.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_UNLISTED);
            getActivity().getContentResolver().insert(GroceryContentProvider.CONTENT_URI_ITEM, values);
            sharedPref.edit().putBoolean("first_run", false).apply();
        }
    }

    @Override
    public int getLayoutId() { return R.layout.item_list_fragment; }

    @Override
    public int getResourceId() { return R.layout.item_row_list; }

    @NonNull
    @Override
    protected String getSelection() {
        return GroceryContract.Item.COLUMN_NAME_STATE + "=" + GroceryContract.Item.STATE_LISTED;
    }

    @NonNull
    @Override
    protected String[] getFrom() {
        return new String[] {
                GroceryContract.Item.COLUMN_NAME_NAME,
                GroceryContract.Item.COLUMN_NAME_SECTION,
                GroceryContract.Item.COLUMN_NAME_QUANTITY};
    }

    public void onSwipeLeft(Uri uri, Cursor cursor, int position) {
        // Increment quantity.
        int quantity = cursor.getInt(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_QUANTITY));
        ContentValues contentValues = new ContentValues();
        contentValues.put(GroceryContract.Item.COLUMN_NAME_QUANTITY, quantity + 1);
        getActivity().getContentResolver().update(uri, contentValues, null, null);
        mAdapter.notifyItemChanged(position);
    }

    public void onSwipeRight(Uri uri, Cursor cursor) {
        // Remove from list.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_UNLISTED);
        contentValues.put(GroceryContract.Item.COLUMN_NAME_QUANTITY, 1);
        getActivity().getContentResolver().update(uri, contentValues, null, null);
        String label = cursor.getString(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));

        makeToast(R.string.toast_item_removed, label);
        View view = getView();
        if (view != null) {
            mEmptyTextView.setText(R.string.done);
            // I wasn't getting any feedback anyway, might as well stop bugging users.
            // AppRater.app_launched(getActivity(), this);
        }
    }

    @Override
    public void onClick(Uri uri) {
        Intent i = new Intent(getActivity(), ItemEditActivity.class);
        i.putExtra(GroceryContentProvider.CONTENT_ITEM_TYPE_ITEM, uri);
        getActivity().startActivity(i);
    }

}
