package com.danepowell.honeydew.authentication;

import androidx.annotation.NonNull;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;

public class ParseComServerAuthenticate {

    private static final String LOG_TAG = "ParseComServerAuth";

    public boolean userSignUp(@NonNull String username, @NonNull String password) {
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(username);
        try {
            user.signUp();
        }
        catch (ParseException e) {
            return false;
        }
        return true;
    }

    public boolean userLogin(@NonNull String username, @NonNull String password) {
        Log.d(LOG_TAG, "userLogin");
        try {
            ParseUser.logIn(username, password);
        }
        catch (ParseException e) {
            return false;
        }
        return true;
    }

    public void userLogout() {
        Log.d(LOG_TAG, "userLogout");
        ParseUser.logOut();
    }

    public String getSessionToken() {
        return ParseUser.getCurrentUser().getSessionToken();
    }
    public String getUsername() { return ParseUser.getCurrentUser().getUsername(); }

    public boolean isLoggedIn() {
        ParseUser user = ParseUser.getCurrentUser();
        return user != null;
    }

    public boolean isActiveSubscription() {
        try {
            HashMap<String, Object> params = new HashMap<>();
            Boolean result = ParseCloud.callFunction("isActiveSubscription", params);
            if (result == null) {
                Log.d(LOG_TAG, "Did not get a response from Parse.");
            } else {
                return result;
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        // Be on the safe side, return true if all else fails. This will still be verified server-side on sync.
        return true;
    }

    public void activateSubscription(String purchaseToken) {
        Log.d(LOG_TAG, "Activating subscription...");
        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("purchaseToken", purchaseToken);
            ParseCloud.callFunction("activateSubscription", params);
            Log.d(LOG_TAG, "Activated subscription.");
        }
        catch (ParseException e) {
            Log.d(LOG_TAG, "Error activating subscription.");
            e.printStackTrace();
        }
    }
}