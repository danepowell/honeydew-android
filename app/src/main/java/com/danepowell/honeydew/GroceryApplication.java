package com.danepowell.honeydew;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import com.danepowell.honeydew.authentication.AccountGeneral;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseInstallation;

public class GroceryApplication extends Application {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String PARSE_APP_ID = "QHLvhsHweaxwZV5hLLs81vxpm5vYIrDa0twi6M1j";
    private static final String PARSE_SERVER_URL = BuildConfig.SERVER_URI;
    // Debug tag, for logging
    private static final String LOG_TAG = "GroceryApplication";

    // TODO: Weird things happen if you delete the app data without uninstalling / removing the parse account.
    // Probably because you have a valid auth token but a new install id.
    @Override
    public void onCreate() {
        super.onCreate();

        // Set up Parse. This has to happen as part of the application onCreate.
        Parse.enableLocalDatastore(this);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(PARSE_APP_ID)
                .server(PARSE_SERVER_URL)
                .build()
        );
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParseACL.setDefaultACL(new ParseACL(), true);
    }

    public void requestSync(boolean expedite) {
        Log.d(LOG_TAG, "Sync requested.");
        Account account = getAccount();
        Bundle settingsBundle = new Bundle();
        if (expedite) {
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        }
        ContentResolver.requestSync(account, GroceryContentProvider.AUTHORITY, settingsBundle);
    }

    public Account getAccount() {
        Account account = null;
        AccountManager accountManager = AccountManager.get(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT <= 22) {
            // API versions 22- should get this permission via the manifest, so something has gone seriously wrong. Abort, abort!
            Log.wtf(LOG_TAG, "Permissions check failed in unexpected way.");
        }
        // API versions 23+ shouldn't need the GET_ACCOUNTS permission in order to get the matching signed account.
        Account[] accounts = accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            account = accounts[0];
        }
        return account;
    }
}
