package com.danepowell.honeydew.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;

import com.danepowell.honeydew.database.GroceryContentProvider;

public class ItemObserver extends ContentObserver {

    private final Account mAccount;
    public ItemObserver(Account account) {
        super(null);
        this.mAccount = account;
    }

    @Override
    public void onChange(boolean selfChange) {
            /*
             * Invoke the method signature available as of
             * Android platform version 4.1, with a null URI.
             */
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri changeUri) {
        Bundle settingsBundle = new Bundle();
        ContentResolver.requestSync(mAccount, GroceryContentProvider.AUTHORITY, settingsBundle);
    }
}
