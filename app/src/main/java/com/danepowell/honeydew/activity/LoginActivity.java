package com.danepowell.honeydew.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.authentication.AccountGeneral;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.helpers.ToolbarActivity;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import static com.danepowell.honeydew.authentication.AccountGeneral.ACCOUNT_TYPE;
import static com.danepowell.honeydew.authentication.AccountGeneral.sServerAuthenticate;



/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends ToolbarActivity {

    // TODO: Convert this to two or three subclasses of a root activity that can be launched separately from AuthenticationActivity rather than passing a requestCode.

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private AccountManager mAccountManager;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private int mLoginIntent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        if (b != null)
            mLoginIntent = b.getInt("login_intent");
        mAccountManager = AccountManager.get(getBaseContext());
        // Set up the login form.
        mEmailView = findViewById(R.id.email);

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == 123 || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        String buttonText = getString(R.string.sign_in_short);
        final TextView infoText = findViewById(R.id.login_info);
        switch (mLoginIntent) {
            case 0:
                buttonText = getString(R.string.sign_in);
                infoText.setText(getString(R.string.reset_password));
                infoText.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = mEmailView.getText().toString();
                        // Check for a valid email address.
                        if (TextUtils.isEmpty(email)) {
                            mEmailView.setError(getString(R.string.error_field_required));
                            mEmailView.requestFocus();
                            return;
                        } else if (isEmailInvalid(email)) {
                            mEmailView.setError(getString(R.string.error_invalid_email));
                            mEmailView.requestFocus();
                            return;
                        }

                        infoText.setText(R.string.password_reset_working);

                        ParseUser.requestPasswordResetInBackground(email, new RequestPasswordResetCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    infoText.setText(getString(R.string.password_reset_success));
                                } else {
                                    infoText.setText(getString(R.string.password_reset_fail));
                                }
                            }
                        });

                    }
                });
                break;
            case 1:
                buttonText = getString(R.string.sign_up);
                infoText.setText(getString(R.string.email_disclosure));
                break;
            case 2:
                removeAllAccounts();
                finish();
                break;
        }
        mEmailSignInButton.setText(buttonText);
        setTitle(buttonText);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    private void removeAllAccounts() {
        // First remove from account manager.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT <= 22) {
            // API versions 22- should get this permission via the manifest, so something has gone seriously wrong. Abort, abort!
            return;
        }
        // API versions 23+ shouldn't need the GET_ACCOUNTS permission in order to get the matching signed account.
        Account[] accounts = mAccountManager.getAccountsByType (AccountGeneral.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            final Handler handler = new Handler();
            for (Account a : accounts) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> arg0) {
                            // nada
                        }
                    };
                    mAccountManager.removeAccount(a, null, callback, handler);
                }
                else {
                    AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> accountManagerFuture) {
                            // nada
                        }
                    };
                    mAccountManager.removeAccount(a, callback, handler);
                }
            }
        }

        // Then clear our internal record.
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("accountName", null);
        editor.apply();
    }

    private void createAndroidAccount(String authToken) {
        // Remove existing accounts.
        removeAllAccounts();

        // Store new account name.
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("accountName", authToken);
        editor.apply();

        // Actually create account.
        final Account account = new Account(authToken, ACCOUNT_TYPE);
        mAccountManager.addAccountExplicitly(account, null, null);
        mAccountManager.setAuthToken(account, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, authToken);
        ContentResolver.setIsSyncable(account, GroceryContentProvider.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, GroceryContentProvider.AUTHORITY, true);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError("This field is required");
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (isEmailInvalid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailInvalid(String email) {
        return !email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            switch (mLoginIntent) {
                case 0:
                    if (sServerAuthenticate.userLogin(mEmail, mPassword)) {
                        createAndroidAccount(sServerAuthenticate.getSessionToken());
                        return true;
                    }
                    break;
                case 1:
                    if (sServerAuthenticate.userSignUp(mEmail, mPassword)) {
                        createAndroidAccount(sServerAuthenticate.getSessionToken());
                        return true;
                    }
                    break;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

