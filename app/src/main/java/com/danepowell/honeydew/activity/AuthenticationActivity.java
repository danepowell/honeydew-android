package com.danepowell.honeydew.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.helpers.AuthToolbarActivity;

import static com.danepowell.honeydew.authentication.AccountGeneral.sServerAuthenticate;

public class AuthenticationActivity extends AuthToolbarActivity {

    private static final String LOG_TAG = "AuthenticationActivity";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    private static final int INTENT_LOGIN = 0;
    private static final int INTENT_SIGN_UP = 1;
    private static final int INTENT_LOGOUT = 2;
    private static final int INTENT_SUBSCRIBE = 3;

    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void startLoginActivity() {
        startLoginActivity(INTENT_LOGIN);
    }

    private void startSignUpActivity() {
        startLoginActivity(INTENT_SIGN_UP);
    }

    private void startLogoutActivity() {startLoginActivity(INTENT_LOGOUT);}

    private void startLoginActivity(int intent) {
        Intent i = new Intent(this, LoginActivity.class);
        Bundle b = new Bundle();
        b.putInt("login_intent", intent);
        i.putExtras(b);
        startActivityForResult(i, intent);
    }

    private void startSubscriptionActivity() {
        Intent i = new Intent(this, SubscriptionActivity.class);
        startActivityForResult(i, INTENT_SUBSCRIBE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sServerAuthenticate.isLoggedIn()) {
            findViewById(R.id.account_info).setVisibility(View.VISIBLE);
            findViewById(R.id.login_buttons).setVisibility(View.GONE);
            TextView accountInfoView = findViewById(R.id.account_info_text);
            String text = String.format(getString(R.string.logged_in_as), sServerAuthenticate.getUsername());
            accountInfoView.setText(text);

            View cancelView = findViewById(R.id.cancel);
            cancelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=com.danepowell.honeydew"));
                    startActivity(intent);
                }
            });

            View logoutView = findViewById(R.id.logout);
            logoutView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sServerAuthenticate.userLogout();
                    startLogoutActivity();
                }
            });

            boolean activeSubscription = mSharedPreferences.getBoolean("activeSubscription", true);

            if (!activeSubscription) {
                cancelView.setVisibility(View.GONE);
                findViewById(R.id.sub_active).setVisibility(View.GONE);
                findViewById(R.id.subscribe).setVisibility(View.VISIBLE);
                View subscribeButton = findViewById(R.id.subscribe_button);
                if (subscribeButton != null) {
                    subscribeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startSubscriptionActivity();
                        }
                    });
                }
            }

        }
        else {
            findViewById(R.id.logout).setVisibility(View.GONE);
            View loginView = findViewById(R.id.sign_in);
            if (loginView != null) {
                loginView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startLoginActivity();
                    }
                });
            }

            View signUpView = findViewById(R.id.sign_up);
            if (signUpView != null) {
                signUpView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSignUpActivity();
                    }
                });
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.authentication_activity;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
            switch (requestCode) {
                case INTENT_LOGIN:
                case INTENT_LOGOUT:
                case INTENT_SIGN_UP:
                    if (sServerAuthenticate.isLoggedIn()) {
                        String toastText = "Logged in as " + sServerAuthenticate.getUsername();
                        if (requestCode == INTENT_LOGIN) {
                            toastText = toastText + ". The first sync may take a few minutes to complete.";
                        }
                        Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT);
                        toast.show();

                        // Launch subscription activity if no active subscription.
                        if (checkSubscription()) {
                            finishAuthentication();
                        }
                        else {
                            startSubscriptionActivity();
                        }
                    }
                    else {
                        Intent i = new Intent(this, ItemListActivity.class);
                        startActivity(i);
                        finish();
                    }
                    break;
                case INTENT_SUBSCRIBE:
                    if (checkSubscription()) {
                        finishAuthentication();
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }

    }

    private boolean checkSubscription() {
        Log.d(LOG_TAG, "Checking for active subscription...");
        if (sServerAuthenticate.isActiveSubscription()) {
            Log.d(LOG_TAG, "Subscription verified.");
            return true;
        }
        else {
            Log.d(LOG_TAG, "User does not have an active subscription.");
            return false;
        }
    }

    private void finishAuthentication() {
        // TODO: Make this act properly like an authenticator
        Bundle result = new Bundle();
        setAccountAuthenticatorResult(result);
        Intent i = new Intent(this, ItemListActivity.class);
        startActivity(i);
        finish();
    }
}
