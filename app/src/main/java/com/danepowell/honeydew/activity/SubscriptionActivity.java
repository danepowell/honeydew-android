package com.danepowell.honeydew.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.helpers.ToolbarActivity;
import com.danepowell.honeydew.util.IabHelper;
import com.danepowell.honeydew.util.IabResult;
import com.danepowell.honeydew.util.Inventory;
import com.danepowell.honeydew.util.Purchase;

import java.util.ArrayList;
import java.util.Objects;

import static com.danepowell.honeydew.authentication.AccountGeneral.sServerAuthenticate;

public class SubscriptionActivity extends ToolbarActivity {

    private static final String LOG_TAG = "SubscriptionActivity";
    private IabHelper mHelper;
    private static final String SKU_SYNC = "sync";
    private static final int REQUEST_SUBSCRIBE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (sServerAuthenticate.isActiveSubscription()) {
            saveSubscriptionPreference();
            finish();
        }
        // Consensus seems to be that it's not worth too much trying to protect this key, especially since we validate all purchases server-side.
        @SuppressWarnings("SpellCheckingInspection") String base64EncodedPublicKey = "";
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh no, there was a problem.
                    Log.d(LOG_TAG, "Problem setting up In-app Billing: " + result);
                    finish();
                    return;
                }
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(LOG_TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            Log.d(LOG_TAG, "Query inventory was successful.");

            Purchase syncSubscription = inventory.getPurchase(SKU_SYNC);
            if (syncSubscription == null) {
                Log.d(LOG_TAG, "No active subscription found");
                // No active subscription.
                startSubscription();
            }
            else {
                Log.d(LOG_TAG, "Active subscription found");
                // We already have an active subscription, so the user should not have been able to start this activity. Probably the local install and server are out of sync, try activating subscription.
                sServerAuthenticate.activateSubscription(syncSubscription.getToken());
                saveSubscriptionPreference();
                finish();
            }
        }
    };

    // TODO: Unify handling of subscription statuses. Maybe put into sServerAuthenticate or new class, with a flag to force a refresh or something.
    private void saveSubscriptionPreference() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("activeSubscription", true);
        editor.apply();
    }

    private void startSubscription() {
        Log.d(LOG_TAG, "Launching purchase flow for sync subscription.");
        try {
            mHelper.launchPurchaseFlow(this, SKU_SYNC, IabHelper.ITEM_TYPE_SUBS, new ArrayList<String>(), REQUEST_SUBSCRIBE, mPurchaseFinishedListener, sServerAuthenticate.getUsername());
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: Fail more gracefully on virtual devices (seems like a bug that mHelper.dispose doesn't account for helper not being set up initially.)
        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.subscription_activity;
    }

    /** Verifies the developer payload of a purchase. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return Objects.equals(payload, sServerAuthenticate.getUsername());
    }

    // Callback for when a purchase is finished
    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(LOG_TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(LOG_TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_SYNC)) {
                Log.d(LOG_TAG, "Sync subscription purchased.");
                sServerAuthenticate.activateSubscription(purchase.getToken());
                saveSubscriptionPreference();
                Toast toast = Toast.makeText(getApplicationContext(), "Subscription successful! Now sign in on your other devices.", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        }
    };

    private void complain(String message) {
        Log.e(LOG_TAG, "**** Honeydew Error: " + message);
        alert("Error: " + message);
    }

    private void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(LOG_TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(LOG_TAG, "onActivityResult handled by IABUtil.");
        }

    }

}
