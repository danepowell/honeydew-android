package com.danepowell.honeydew.activity;

import androidx.loader.app.LoaderManager;
import android.content.ContentValues;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import android.widget.TextView;
import android.widget.Toast;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;
import com.danepowell.honeydew.helpers.ToolbarActivity;

public class ItemEditActivity extends ToolbarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private Spinner mSectionSpinner;
    private EditText mSectionText;
    private TextView mQuantityText;
    private Button mQuantityPlus;
    private Button mQuantityMinus;
    private String mSectionName;
    private SimpleCursorAdapter mSpinnerAdapter;
    private static final int LOADER_SPINNER = 1;
    private static final int LOADER_ITEM = 2;
    @Nullable
    private Uri mItemUri;
    private String mItemName;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setLayoutElements();
        setItemUri(bundle);
        setSpinnerAdapter();

        mSectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mSectionText.setVisibility(View.VISIBLE);
                    mSectionText.requestFocus();
                } else {
                    mSectionText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mQuantityMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentQuantity = Integer.parseInt(mQuantityText.getText().toString());
                String newQuantity = Integer.toString(currentQuantity - 1);
                mQuantityText.setText(newQuantity);
            }
        });

        mQuantityPlus.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int currentQuantity = Integer.parseInt(mQuantityText.getText().toString());
                String newQuantity = Integer.toString(currentQuantity + 1);
                mQuantityText.setText(newQuantity);
            }
        });

        // Initialize loaders.
        LoaderManager.getInstance(this).initLoader(LOADER_SPINNER, null, this);
        LoaderManager.getInstance(this).initLoader(LOADER_ITEM, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.item_edit_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
            case android.R.id.home:
                saveItem();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        saveItem();
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = null;
        String[] projection = null;
        String selection = null;
        switch (id) {
            case LOADER_SPINNER:
                // Configure spinner.
                uri = GroceryContentProvider.CONTENT_URI_ITEM_DISTINCT;
                projection = new String[]{
                        GroceryContract.Item._ID,
                        GroceryContract.Item.COLUMN_NAME_SECTION
                };
                selection = GroceryContract.Item.COLUMN_NAME_STATE + "!="
                        + GroceryContract.Item.STATE_DELETED;
                break;
            case LOADER_ITEM:
                // Set item values.
                uri = mItemUri;
                projection = new String[]{
                        GroceryContract.Item.COLUMN_NAME_NAME,
                        GroceryContract.Item.COLUMN_NAME_SECTION,
                        GroceryContract.Item.COLUMN_NAME_QUANTITY
                };
                break;
        }
        return new CursorLoader(ItemEditActivity.this, uri, projection, selection, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @NonNull Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_SPINNER:
                // Get allowed values for spinner.
                MatrixCursor extrasCursor = new MatrixCursor(
                        new String[]{"_id", GroceryContract.Item.COLUMN_NAME_SECTION}
                );
                extrasCursor.addRow(new String[]{"-1", getString(R.string.new_category)});
                Cursor[] cursors = {extrasCursor, cursor};
                Cursor extendedCursor = new MergeCursor(cursors);
                mSpinnerAdapter.swapCursor(extendedCursor);
                if (mSectionName != null) {
                    setSpinnerSelection();
                }
                break;
            case LOADER_ITEM:
                cursor.moveToFirst();
                mSectionName = cursor.getString(cursor
                        .getColumnIndexOrThrow(GroceryContract.Item.COLUMN_NAME_SECTION));
                mItemName = cursor.getString(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));
                mQuantityText.setText(cursor.getString(cursor
                        .getColumnIndex(GroceryContract.Item.COLUMN_NAME_QUANTITY)));
                if (mSpinnerAdapter.getCursor() != null) {
                    setSpinnerSelection();
                }
                setTitle("Edit " + mItemName);
                break;
        }
    }

    /**
     * Sets active spinner option.
     */
    private void setSpinnerSelection() {
        Cursor spinnerCursor = mSpinnerAdapter.getCursor();
        spinnerCursor.moveToFirst();
        while (spinnerCursor.moveToNext()) {
            String s = spinnerCursor.getString(spinnerCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION));
            if (s.equalsIgnoreCase(mSectionName)) {
                mSectionSpinner.setSelection(spinnerCursor.getPosition());
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() == LOADER_SPINNER) {
            mSpinnerAdapter.swapCursor(null);
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_edit_activity;
    }

    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveItem();
        savedInstanceState.putParcelable(GroceryContentProvider.CONTENT_ITEM_TYPE_ITEM, mItemUri);
    }

    private void saveItem() {
        String section;
        if (mSectionText.getVisibility() == View.VISIBLE) {
            section = mSectionText.getText().toString();
        }
        else {
            Cursor cursor = (Cursor) mSectionSpinner.getSelectedItem();
            section = cursor.getString(cursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION));
        }
        int quantity = Integer.parseInt(mQuantityText.getText().toString());

        if (section.length() == 0) {
            return;
        }
        if (quantity == 0) {
            return;
        }

        if (mItemUri != null) {
            // Update item.
            ContentValues values = new ContentValues();
            values.put(GroceryContract.Item.COLUMN_NAME_SECTION, section);
            values.put(GroceryContract.Item.COLUMN_NAME_QUANTITY, quantity);
            getContentResolver().update(mItemUri, values, null, null);

            // Make toast.
            String string = getApplicationContext().getResources().getString(R.string.toast_item_updated);
            String text = String.format(string, mItemName);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
        finish();
    }

    private void setLayoutElements() {
        mSectionSpinner = findViewById(R.id.section_spinner);
        mQuantityText = findViewById(R.id.quantity_text);
        mSectionText = findViewById(R.id.section_text);
        mQuantityMinus = findViewById(R.id.quantity_minus);
        mQuantityPlus = findViewById(R.id.quantity_plus);
    }

    private void setItemUri(@Nullable Bundle bundle) {
        Bundle extras = getIntent().getExtras();
        mItemUri = (bundle == null) ? null : (Uri) bundle
                .getParcelable(GroceryContentProvider.CONTENT_ITEM_TYPE_ITEM);
        if (extras != null) {
            mItemUri = extras.getParcelable(GroceryContentProvider.CONTENT_ITEM_TYPE_ITEM);
        }
    }

    private void setSpinnerAdapter() {
        mSpinnerAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                null,
                new String[]{GroceryContract.Item.COLUMN_NAME_SECTION},
                new int[]{android.R.id.text1},
                0
        );
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSectionSpinner.setAdapter(mSpinnerAdapter);
    }
}
