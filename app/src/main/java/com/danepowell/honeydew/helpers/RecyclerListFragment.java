package com.danepowell.honeydew.helpers;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract public class RecyclerListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    protected ItemRecyclerAdapter mAdapter;
    private RecyclerView mRecyclerView;
    protected RelativeLayout mEmptyView;
    protected TextView mEmptyTextView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    protected void restart() {
        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mRootView = inflater.inflate(getLayoutId(), container, false);
        mRecyclerView = mRootView.findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        mAdapter = new ItemRecyclerAdapter(getActivity(), getResourceId(), this);
        mRecyclerView.setAdapter(mAdapter);
        mEmptyView = mRootView.findViewById(R.id.empty);
        mEmptyTextView = mRootView.findViewById(R.id.empty_text);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                Cursor cursor = mAdapter.getCursor();
                cursor.moveToPosition(position);
                Uri uri = Uri.parse(GroceryContentProvider.CONTENT_URI_ITEM + "/" + viewHolder.getItemId());
                if (swipeDir == ItemTouchHelper.LEFT) {
                    onSwipeLeft(uri, cursor, position);
                }
                else {
                    onSwipeRight(uri, cursor);
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView1, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
                return false;
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                getDefaultUIUtil().clearView(((ItemRecyclerAdapter.ViewHolder) viewHolder).getSwipeView());
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder != null) {
                    getDefaultUIUtil().onSelected(((ItemRecyclerAdapter.ViewHolder) viewHolder).getSwipeView());
                }
            }

            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                getDefaultUIUtil().onDraw(c, recyclerView, ((ItemRecyclerAdapter.ViewHolder) viewHolder).getSwipeView(), dX, dY,    actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX > 0) { // swiping right
                        ((ItemRecyclerAdapter.ViewHolder) viewHolder).onSwipeRight();
                    }
                    else {
                        ((ItemRecyclerAdapter.ViewHolder) viewHolder).onSwipeLeft();
                    }
                }
            }

            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                getDefaultUIUtil().onDrawOver(c, recyclerView, ((ItemRecyclerAdapter.ViewHolder) viewHolder).getSwipeView(), dX, dY,    actionState, isCurrentlyActive);
            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        return mRootView;
    }



    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = GroceryContentProvider.CONTENT_URI_ITEM;
        String sortOrder = GroceryContract.Item.COLUMN_NAME_SECTION + "," + GroceryContract.Item.COLUMN_NAME_NAME;
        return new CursorLoader(getActivity(), uri, getProjection(), getSelection(), getSelectionArgs(), sortOrder);
    }

    private String[] getProjection() {
        List<String> projection = new ArrayList<>();
        projection.add(GroceryContract.Item._ID);
        Collections.addAll(projection, getFrom());
        String[] projectionArray = new String[projection.size()];
        return projection.toArray(projectionArray);
    }

    @NonNull
    protected abstract String getSelection();
    protected abstract void onSwipeLeft(Uri uri, Cursor cursor, int position);
    protected abstract void onSwipeRight(Uri uri, Cursor cursor);
    protected abstract void onClick(Uri uri);

    @Nullable
    protected String[] getSelectionArgs() {
        return null;
    }
    protected abstract int getLayoutId();
    @NonNull
    abstract protected String[] getFrom();
    protected abstract int getResourceId();


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.swapCursor(data);
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
        else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.swapCursor(null);
    }

    protected void makeToast(int stringResource, String name) {
        String string = getActivity().getResources().getString(stringResource);
        String text = String.format(string, name);
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), text, duration);
        toast.show();
    }
}
