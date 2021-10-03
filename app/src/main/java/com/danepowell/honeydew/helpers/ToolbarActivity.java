package com.danepowell.honeydew.helpers;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBar;

import com.danepowell.honeydew.R;

public abstract class ToolbarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(getLayoutId());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Intent intent = getParentActivityIntent();

        if (actionBar != null) {
            actionBar.setElevation(4);
        }

        if (actionBar != null && intent != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.backarrow);
        }
    }

    protected abstract int getLayoutId();
}
