package com.danepowell.honeydew.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PhiAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {

        PhiAuthenticator authenticator = new PhiAuthenticator(this);
        return authenticator.getIBinder();
    }
}