package com.danepowell.honeydew.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class GrocerySyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    @Nullable
    private static GrocerySyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null)
                sSyncAdapter = new GrocerySyncAdapter(getApplicationContext());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (sSyncAdapter == null) {
            return null;
        }
        return sSyncAdapter.getSyncAdapterBinder();
    }
}