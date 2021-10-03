package com.danepowell.honeydew.activity;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.danepowell.honeydew.GroceryApplication;
import com.danepowell.honeydew.R;
import com.danepowell.honeydew.authentication.AccountGeneral;
import com.danepowell.honeydew.database.GroceryContentProvider;
import com.danepowell.honeydew.helpers.ToolbarActivity;
import com.danepowell.honeydew.sync.ItemObserver;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/*
 * ItemListActivity displays the existing grocery items in a list.
 */
public class ItemListActivity extends ToolbarActivity implements View.OnClickListener {

    private ShowcaseView sv;
    private int counter = 0;
    private SwipeRefreshLayout mSwipeRefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View addButton = findViewById(R.id.add_button);
        if (addButton != null) {
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addItem();
                }
            });
        }

        mSwipeRefresh = findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GroceryApplication application = (GroceryApplication) getApplicationContext();
                application.requestSync(true);
                mSwipeRefresh.setRefreshing(false);
            }
        });
        // Register content observer.
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String accountName = sharedPreferences.getString("accountName", null);
        if (accountName != null) {
            registerContentObservers();

        }
        showTutorial(false);
    }

    private void showTutorial(boolean force) {
        // Set up showcase for first run.
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);

        Target itemTarget = new Target() {
            @Override
            public Point getPoint() {
                ActionBar actionBar = getSupportActionBar();
                int actionBarSize = 0;
                if (actionBar != null) {
                    actionBarSize = actionBar.getHeight();
                }
                return new Point(actionBarSize - 40, actionBarSize * 2 + 30);
            }
        };

        ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setContentTitle("Honeydew is your grocery list")
                .blockAllTouches()
                .setContentText("➤ Swipe items right to remove them.\n➤ Tap items to edit them.")
                .setStyle(R.style.HoneydewShowcaseTheme)
                .setOnClickListener(this);
        if (!force) {
            builder.singleShot(1);
        }
        sv = builder.build();
        sv.setButtonText("Next");
        sv.setButtonPosition(lps);
        sv.setShowcase(itemTarget, true);
    }

    private void registerContentObservers() {
        GroceryApplication application = (GroceryApplication) getApplicationContext();
        ItemObserver mObserver = new ItemObserver(application.getAccount());
        getContentResolver().registerContentObserver(GroceryContentProvider.CONTENT_URI_ITEM, true, mObserver);
        application.requestSync(true);
    }

    @Override
    public void onClick(View v) {
        switch (counter) {
            case 0:
                ViewTarget addButtonTarget = new ViewTarget(R.id.add_button, this);
                sv.setContentTitle("Adding items");
                sv.setContentText("➤ Tap to add items to your list.");
                sv.setShowcase(addButtonTarget, true);
                break;
            case 1:
                ViewTarget syncButtonTarget = new ViewTarget(R.id.sync, this);
                sv.setContentTitle("Shopping with your family");
                sv.setContentText("➤ Tap to sync with your family.\n\n\nStill have questions or comments? Want to talk to a human being? Email honeydew@danepowell.com");
                sv.setShowcase(syncButtonTarget, true);
                sv.setButtonText("Let's go!");
                break;
            case 2:
                sv.hide();
        }
        counter++;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_list_activity;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.item_list_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sync:
                addNewAccount();
                return true;
            case R.id.help:
                showTutorial(true);
                return true;
        }
        return false;
    }

    private void addItem() {
        Intent i = new Intent(this, ItemAddActivity.class);
        startActivity(i);
    }

    private void addNewAccount() {
        AccountManager.get(getBaseContext()).addAccount(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTH_TOKEN_TYPE_FULL_ACCESS, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(@NonNull AccountManagerFuture<Bundle> future) {
                try {
                    future.getResult();
                    finish();
                    startActivity(getIntent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

}
