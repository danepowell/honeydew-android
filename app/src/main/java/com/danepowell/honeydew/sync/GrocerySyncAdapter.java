package com.danepowell.honeydew.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.util.Log;

import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.database.GroceryContract;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.danepowell.honeydew.authentication.AccountGeneral.sServerAuthenticate;

class GrocerySyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String LOG_TAG = "GrocerySyncAdapter";

    private SharedPreferences mSharedPref;

    GrocerySyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              @NonNull ContentProviderClient provider, SyncResult syncResult) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        Date lastSyncTime = new Date(mSharedPref.getLong("lastItemSyncTime:" + account.name, 0));
        int lastSequenceId = mSharedPref.getInt("lastSequenceId:" + account.name, 0);

        if (!isActiveSubscription()) {
            Log.d(LOG_TAG, "No active subscription, aborting sync.");
            return;
        }

        // Get items for sync.
        Log.d(LOG_TAG, "Getting items for sync...");
        ArrayList<GrocerySyncItem> remoteChanges = getRemoteChanges(lastSyncTime);
        ArrayList<GrocerySyncItem> localChanges = getLocalChanges(provider, lastSequenceId);
        int totalChanges = remoteChanges.size() + localChanges.size();
        if (totalChanges == 0) {
            Log.d(LOG_TAG, "Nothing to sync.");
            return;
        }

        // Create and update items.
        Log.d(LOG_TAG, "Syncing changes...");
        updateLocalItems(remoteChanges, provider);
        updateRemoteItems(localChanges);

        // Check for changes that occurred during sync.
        boolean rerunSync = false;
        ArrayList<GrocerySyncItem> newRemoteChanges = getRemoteChanges(lastSyncTime);
        ArrayList<GrocerySyncItem> newLocalChanges = getLocalChanges(provider, lastSequenceId);

        // Look for remote changes.
        // TODO: this heuristic check is greedy and fails to detect when an item is updated,
        // as by another device at the same time or during an after-save hook (section guessing).
        long newLastSyncTime = lastSyncTime.getTime();
        if (newRemoteChanges.size() <= totalChanges) {
            // The sync is sterile.
            if (newRemoteChanges.size() > 0) {
                newLastSyncTime = newRemoteChanges.get(0).updatedAt;
            }
        }
        else {
            // The sync was dirty.
            if (remoteChanges.size() > 0) {
                newLastSyncTime = remoteChanges.get(0).updatedAt;
            }
            rerunSync = true;
        }
        updateLastSyncTime(newLastSyncTime, account);

        // Look for local changes.
        int newLastSequenceId = lastSequenceId;
        if (newLocalChanges.size() <= totalChanges) {
            // The sync is sterile.
            if (newLocalChanges.size() > 0) {
                newLastSequenceId = newLocalChanges.get(0).sequenceId;
            }
        }
        else {
            // The sync was dirty.
            if (localChanges.size() > 0) {
                newLastSequenceId = localChanges.get(0).sequenceId;
            }
            rerunSync = true;
        }
        updateLastSequenceId(newLastSequenceId, account);

        // Rerun the sync if necessary.
        if (rerunSync) {
            Log.d(LOG_TAG, "Changes detected during sync, rerunning.");
            onPerformSync(account, extras, authority, provider, syncResult);
        }
        else {
            Log.d(LOG_TAG, "Finished sync.");
        }
    }

    @NonNull
    private ArrayList<GrocerySyncItem> getRemoteChanges(Date lastSyncTime) {
        ArrayList<GrocerySyncItem> items = new ArrayList<>();
        try {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
            query.setLimit(100000);
            query.whereGreaterThan("updatedAt", lastSyncTime);
            query.orderByDescending("updatedAt");
            List<ParseObject> parseItems = query.find();
            for (ParseObject item : parseItems) {
                items.add(GrocerySyncItem.fromParseObject(item));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return items;
    }

    @NonNull
    private ArrayList<GrocerySyncItem> getLocalChanges(@NonNull ContentProviderClient provider, int lastSequenceId) {
        ArrayList<GrocerySyncItem> items = new ArrayList<>();
        try {
            String selection = GroceryContract.Item.COLUMN_NAME_SEQUENCE + ">" + lastSequenceId;
            String sortOrder = GroceryContract.Item.COLUMN_NAME_SEQUENCE + " DESC";
            Cursor curItems = provider.query(GroceryContentProvider.CONTENT_URI_ITEM, null, selection, null, sortOrder);
            if (curItems != null) {
                while (curItems.moveToNext()) {
                    items.add(GrocerySyncItem.fromCursor(curItems));
                }
                curItems.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return items;
    }

    private void updateLocalItems(@NonNull ArrayList<GrocerySyncItem> items, @NonNull ContentProviderClient provider) {
        try {
            for (GrocerySyncItem item : items) {
                ContentValues values = item.getContentValues();
                provider.insert(GroceryContentProvider.CONTENT_URI_ITEM, values);
                Log.d(LOG_TAG, "pulled " + item.name);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateRemoteItems(@NonNull ArrayList<GrocerySyncItem> items) {
        try {
            for (GrocerySyncItem item : items) {
                ParseObject parseRemoteItem = item.getParseObject();
                parseRemoteItem.save();
                Log.d(LOG_TAG, "pushed " + item.name);
            }
        } catch (ParseException e) {
            if (Objects.equals(e.getMessage(), "No active subscription")) {
                deactivateSubscription();
            }
            else {
                e.printStackTrace();
            }
        }
    }

    private void updateLastSyncTime(long lastSyncTime, Account account) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putLong("lastItemSyncTime:" + account.name, lastSyncTime);
        editor.apply();
    }

    private void updateLastSequenceId(int lastSequenceId, Account account) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("lastSequenceId:" + account.name, lastSequenceId);
        editor.apply();
    }

    private boolean isActiveSubscription() {
        boolean activeSubscription = mSharedPref.getBoolean("activeSubscription", true);
        if (!activeSubscription) {
            return false;
        }
        if (!sServerAuthenticate.isActiveSubscription()) {
            deactivateSubscription();
            return false;
        }
        return true;
    }
    private void deactivateSubscription() {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean("activeSubscription", false);
        editor.apply();
    }

    @Override
    public void onSyncCanceled() {
        Log.d(LOG_TAG, "Sync was cancelled.");
    }
}

