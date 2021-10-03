package com.danepowell.honeydew.helpers;

import android.app.AlertDialog;

import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.danepowell.honeydew.R;

public class AppRater {
    private final static long DAYS_UNTIL_PROMPT = 30;
    private final static long LAUNCHES_UNTIL_PROMPT = 4;

    private static SharedPreferences.Editor mEditor;

    public static void app_launched(Context mContext, Fragment fragment) {
        SharedPreferences prefs = mContext.getSharedPreferences("honeydew", 0);
        if (prefs.getBoolean("hide_rater", false)) { return ; }

        mEditor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        mEditor.putLong("launch_count", launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_first_launch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            mEditor.putLong("date_first_launch", date_firstLaunch);
        }

        mEditor.apply();

        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            long interval = DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() >= date_firstLaunch + interval) {
                DialogFragment dialog = new RateDialogFragment();
                dialog.show(fragment.getFragmentManager(), "RateDialogFragment");
            }
        }
    }

    private static void showGoodbyeToast(final Context context) {
        Toast.makeText(context, R.string.go_away, Toast.LENGTH_SHORT).show();
    }

    private static void hide() {
        if (mEditor != null) {
            mEditor.putBoolean("hide_rater", true);
            mEditor.commit();
        }
    }

    private static void snooze(final Context context) {
        if (mEditor != null) {
            mEditor.remove("launch_count");
            mEditor.remove("date_first_launch");
            mEditor.commit();
        }
        Toast.makeText(context, "No worries, we'll check back later!", Toast.LENGTH_SHORT).show();
    }

    public static class RateDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.quick)
                    .setMessage(R.string.howdy_feedback)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            DialogFragment dialog = new HappyDialogFragment();
                            dialog.show(getFragmentManager(), "HappyDialogFragment");

                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            DialogFragment dialog = new UnhappyDialogFragment();
                            dialog.show(getFragmentManager(), "UnhappyDialogFragment");
                        }
                    });
            return builder.create();
        }
    }

    public static class HappyDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.fabulous);
            builder.setMessage(R.string.rate_now)
                    .setPositiveButton(R.string.rate, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            hide();
                            getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.danepowell.honeydew")));
                        }
                    })
                    .setNeutralButton(R.string.not_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            snooze(getActivity());
                        }
                    })
                    .setNegativeButton(R.string.never, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            hide();
                            showGoodbyeToast(getActivity());
                        }
                    });
            return builder.create();
        }
    }

    public static class UnhappyDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.oh_no);
            builder.setMessage(R.string.better)
                    .setPositiveButton("Email support", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            hide();
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("message/rfc822");
                            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"honeydew@danepowell.com"});
                            i.putExtra(Intent.EXTRA_SUBJECT, "Problem with Honeydew");
                            try {
                                getActivity().startActivity(Intent.createChooser(i, "Send mail..."));
                            } catch (ActivityNotFoundException ex) {
                                Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNeutralButton(R.string.not_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            snooze(getActivity());
                        }
                    })
                    .setNegativeButton(R.string.never, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            hide();
                            showGoodbyeToast(getActivity());
                        }
                    });
            return builder.create();
        }
    }
}
