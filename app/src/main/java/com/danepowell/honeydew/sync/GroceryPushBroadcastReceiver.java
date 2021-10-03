package com.danepowell.honeydew.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.danepowell.honeydew.GroceryApplication;
import com.parse.ParsePushBroadcastReceiver;

public class GroceryPushBroadcastReceiver extends ParsePushBroadcastReceiver {
    public void onPushReceive(Context context, Intent intent) {
        Log.d("Honeydew", "Remote has been updated, syncing...");
        GroceryApplication application = (GroceryApplication) context.getApplicationContext();
        application.requestSync(false);
    }
}
