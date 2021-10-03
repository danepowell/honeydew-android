package com.danepowell.honeydew.helpers;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;

import java.util.TreeMap;

public class ItemRecyclerAdapter extends RecyclerView.Adapter<ItemRecyclerAdapter.ViewHolder> {
    private Cursor mCursor;
    private final RecyclerListFragment mParentFragment;
    private final Activity mContext;
    private final int mLayout;
    private final DataSetObserver mDataSetObserver;
    private int mRowIDColumn;
    private boolean mDataValid;
    private final TreeMap<Integer, Boolean> mHeaderIndices = new TreeMap<>();

    ItemRecyclerAdapter(Activity context, int layout, RecyclerListFragment fragment) {
        mParentFragment = fragment;
        mContext = context;
        mLayout = layout;
        mDataValid = mCursor != null;
        super.setHasStableIds(true);
        mRowIDColumn = mDataValid ? mCursor.getColumnIndex(GroceryContract.Item._ID) : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public void swapCursor(Cursor newCursor){
        if (newCursor == mCursor) {
            return;
        }
        Cursor oldCursor = mCursor;
        if (oldCursor != null) {
            if (mDataSetObserver != null) oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (newCursor != null) {
            if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);
            mRowIDColumn = newCursor.getColumnIndexOrThrow(GroceryContract.Item._ID);
            mDataValid = true;
            // notify the observers about the new cursor
            notifyDataSetChanged();
            recomputeHeaderIndices();
        } else {
            mRowIDColumn = -1;
            mDataValid = false;
        }
    }

    Cursor getCursor() {
        return mCursor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RelativeLayout c = (RelativeLayout) LayoutInflater.from(mContext).inflate(mLayout, parent, false);
        return new ViewHolder(c, new ViewHolder.IMyViewHolderClicks() {
            public void onClick(long id) {
                Uri uri = Uri.parse(GroceryContentProvider.CONTENT_URI_ITEM + "/" + id);
                mParentFragment.onClick(uri);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        String label = mCursor.getString(mCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_NAME));
        viewHolder.mNameTextView.setText(label);
        viewHolder.mSeparatorView.setText(mCursor.getString(mCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION)));
        int quantityColumnIndex = mCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_QUANTITY);
        // If this is ItemAddActivity, there will be no quantity.
        if (quantityColumnIndex != -1) {
            int quantity = mCursor.getInt(quantityColumnIndex);
            if (quantity > 1) {
                Resources res = mContext.getResources();
                String labelText = String.format(res.getString(R.string.list_row), label, quantity);
                viewHolder.mNameTextView.setText(labelText);
            }
        }
        if (mHeaderIndices.size() > 0) {
            if (mHeaderIndices.get(position)) {
                viewHolder.mSeparatorView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mSeparatorView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mRowIDColumn);
        }
        return 0;
    }

    private void recomputeHeaderIndices() {
        if (mCursor.getCount() == 0) {
            return;
        }
        mCursor.moveToFirst();
        mHeaderIndices.put(0, true);
        String prevName = mCursor.getString(mCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION));
        while (mCursor.moveToNext()) {
            String name = mCursor.getString(mCursor.getColumnIndex(GroceryContract.Item.COLUMN_NAME_SECTION));
            if (name.equals(prevName)) {
                mHeaderIndices.put(mCursor.getPosition(), false);
            } else {
                mHeaderIndices.put(mCursor.getPosition(), true);
            }
            prevName = name;
        }
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
            mHeaderIndices.clear();
            recomputeHeaderIndices();
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final IMyViewHolderClicks mListener;
        final TextView mNameTextView;
        final TextView mSeparatorView;
        private final View mRemovableView;
        private final View mSwipeRight;
        private final View mSwipeLeft;

        ViewHolder(View itemView, IMyViewHolderClicks listener) {
            super(itemView);
            mListener = listener;
            mNameTextView = itemView.findViewById(R.id.label);
            mSeparatorView = itemView.findViewById(R.id.separator);
            mNameTextView.setOnClickListener(this);
            mRemovableView = itemView.findViewById(R.id.item_main);
            mSwipeLeft = itemView.findViewById(R.id.item_swipe_left);
            mSwipeRight = itemView.findViewById(R.id.item_swipe_right);
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(this.getItemId());
        }

        interface IMyViewHolderClicks {
            void onClick(long id);
        }

        View getSwipeView() {
            return mRemovableView;
        }

        void onSwipeLeft() {
            mSwipeLeft.setVisibility(View.VISIBLE);
            mSwipeRight.setVisibility(View.GONE);
        }

        void onSwipeRight() {
            mSwipeLeft.setVisibility(View.GONE);
            mSwipeRight.setVisibility(View.VISIBLE);
        }

    }
}
