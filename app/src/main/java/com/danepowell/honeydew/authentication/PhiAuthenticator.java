package com.danepowell.honeydew.authentication;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.danepowell.honeydew.activity.AuthenticationActivity;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static com.danepowell.honeydew.authentication.AccountGeneral.*;

class PhiAuthenticator extends AbstractAccountAuthenticator {

    private final String TAG = "PhiAuthenticator";
    private final Context mContext;

    public PhiAuthenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        Log.d("honeydew", TAG + "> addAccount");

        final Intent intent = new Intent(mContext, AuthenticationActivity.class);
        intent.putExtra(AuthenticationActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthenticationActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticationActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @NonNull
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, @NonNull Account account, @NonNull String authTokenType, Bundle options) {

        Log.d("honeydew", TAG + "> getSessionToken");

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        Log.d("grocery", TAG + "> peekAuthToken returned - " + authToken);

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticationActivity.
        final Intent intent = new Intent(mContext, AuthenticationActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticationActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthenticationActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticationActivity.ARG_ACCOUNT_NAME, account.name);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @NonNull
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AUTH_TOKEN_TYPE_FULL_ACCESS.equals(authTokenType))
            return AUTH_TOKEN_TYPE_FULL_ACCESS_LABEL;
        else
            return authTokenType + " (Label)";
    }

    @NonNull
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Nullable
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Nullable
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        return null;
    }

    @Nullable
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        return null;
    }
}