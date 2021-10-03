package com.danepowell.honeydew.fragment;

import android.app.Activity;
import android.content.ContentValues;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;
import com.danepowell.honeydew.helpers.RecyclerListFragment;

public class ItemAddFragment extends RecyclerListFragment {

    private EditText mNameText;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get layout elements.
        mNameText = getActivity().findViewById(R.id.add_name);

        mEmptyView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                addItemFromText();
            }
        });

        mNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                restart();
                String itemName = mNameText.getText().toString();
                if (itemName.isEmpty()) {
                    mEmptyTextView.setVisibility(View.GONE);
                }
                else {
                    String string = getActivity().getResources().getString(R.string.item_add_name);
                    String text = String.format(string, itemName);
                    mEmptyTextView.setText(text);
                    mEmptyTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        TextView.OnEditorActionListener editListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addItemFromText();
                }
                return true;
            }
        };

        mNameText.setOnEditorActionListener(editListener);
        mNameText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mNameText.setImeActionLabel("Add", EditorInfo.IME_ACTION_DONE);
    }

    @Override
    public int getLayoutId() { return R.layout.item_add_fragment; }

    @Override
    public int getResourceId() { return R.layout.item_row_add; }

    @NonNull
    @Override
    protected String getSelection() {
        return GroceryContract.Item.COLUMN_NAME_STATE + "=" + GroceryContract.Item.STATE_UNLISTED + " AND " + GroceryContract.Item.COLUMN_NAME_NAME + " LIKE ?";
    }

    @NonNull
    @Override
    protected String[] getSelectionArgs() {
        String name = "";
        if (mNameText != null && mNameText.getText().length() > 0) {
            name = mNameText.getText().toString();
        }
        return new String[] {"%" + name + "%"};
    }

    @NonNull
    @Override
    protected String[] getFrom() {
        return new String[] {
                GroceryContract.Item.COLUMN_NAME_NAME,
                GroceryContract.Item.COLUMN_NAME_SECTION};
    }

    @Override
    public void onSwipeLeft(Uri uri, Cursor cursor, int position) {
        // Add to list.
        ContentValues values = new ContentValues();
        values.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_LISTED);
        getActivity().getContentResolver().update(uri, values, null, null);
        String label = cursor.getString(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));
        makeToast(R.string.toast_item_added, label);
    }

    @Override
    public void onSwipeRight(Uri uri, Cursor cursor) {
        // Delete item.
        String label = cursor.getString(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));
        getActivity().getContentResolver().delete(uri, null, null);
        makeToast(R.string.toast_item_deleted, label);
    }

    @Override
    public void onClick(Uri uri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_LISTED);
        getActivity().getContentResolver().update(uri, contentValues, null, null);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.swapCursor(data);
    }

    private void insertItem(String name) {
        ContentValues itemValues = new ContentValues();
        itemValues.put(GroceryContract.Item.COLUMN_NAME_NAME, name);
        itemValues.put(GroceryContract.Item.COLUMN_NAME_STATE, GroceryContract.Item.STATE_LISTED);
        getActivity().getContentResolver().insert(GroceryContentProvider.CONTENT_URI_ITEM, itemValues);
        makeToast(R.string.toast_item_added, name);
    }

    private void addItemFromText() {
        String name = mNameText.getText().toString();
        if (TextUtils.isEmpty(name)) {
            makeToast(R.string.toast_item_empty, null);
            return;
        }
        insertItem(name);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }
}
