package com.danepowell.honeydew.activity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.danepowell.honeydew.R;
import com.danepowell.honeydew.helpers.ToolbarActivity;
import com.github.amlcurran.showcaseview.ShowcaseView;

public class ItemAddActivity extends ToolbarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showTutorial(false);
    }

    private void showTutorial(boolean force) {
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);
        ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setContentTitle("Adding an item to your list")
                .setContentText("➤ Start typing to find or add items.\n➤ Tap or swipe left to add an item.\n➤ Swipe right to delete")
                .setStyle(R.style.HoneydewShowcaseTheme);
        if (!force) {
            builder.singleShot(2);
        }
        ShowcaseView sv = builder.build();
        sv.setButtonPosition(lps);
        sv.setButtonText("Got it");
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_add_activity;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.item_add_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.help) {
            showTutorial(true);
            return true;
        }
        return false;
    }

}
