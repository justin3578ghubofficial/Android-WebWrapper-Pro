package at.cosmosinsurance.online.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.JsResult;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import at.cosmosinsurance.online.Constants;
import at.cosmosinsurance.online.R;

public class UIManager {
    // Instance variables
    private Activity activity;
    private WebView webView;
    private ProgressBar progressSpinner;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeContainer;
    private AlertDialog.Builder alertDialog;
    private boolean pageLoaded = false;

    public UIManager(Activity activity) {
        this.activity = activity;
        this.progressBar = activity.findViewById(R.id.progressBar);
        this.progressSpinner = activity.findViewById(R.id.progressSpinner);
        this.swipeContainer = activity.findViewById(R.id.swipeContainer);
        this.webView = activity.findViewById(R.id.webView);
        this.alertDialog = new AlertDialog.Builder(activity);
    }

    /**
    * Setup swipe refresh functionality
    */
    public void setupSwipeContainer() {
        if (Constants.REFRESH) {
            swipeContainer.setOnRefreshListener(() -> {
                // Pass the current activity context to the pull_fresh method
                webView.reload();
                swipeContainer.setRefreshing(false);
            });
            // Only enable pull-to-refresh when at the top of the page
            webView.getViewTreeObserver().addOnScrollChangedListener(() -> swipeContainer.setEnabled(webView.getScrollY() == 0));
        } else {
            swipeContainer.setRefreshing(false);
            swipeContainer.setEnabled(false);
        }
    }

    // Set Loading Progress for ProgressBar
    public void setLoadingProgress(int progress) {
        // set progress in UI
        progressBar.setProgress(progress, true);
        // hide ProgressBar if not applicable
        if (progress >= 0 && progress < 100) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
        // get app screen back if loading is almost complete
        if (progress >= Constants.PROGRESS_THRESHOLD && !pageLoaded) {
            setLoading(false);
        }
    }

    // Show loading animation screen while app is loading/caching the first time
    public void setLoading(boolean isLoading) {
        if (isLoading) {
            progressSpinner.setVisibility(View.VISIBLE);
            webView.animate().translationX(Constants.SLIDE_EFFECT).alpha(0.5F).setInterpolator(new AccelerateInterpolator()).start();
        } else {
            webView.setTranslationX(Constants.SLIDE_EFFECT * -1);
            webView.animate().translationX(0).alpha(1F).setInterpolator(new DecelerateInterpolator()).start();
            progressSpinner.setVisibility(View.INVISIBLE);
        }
        pageLoaded = !isLoading;
    }


    // set icon in recent activity view to a white one to be visible in the app bar
    public void changeRecentAppsIcon() {
        String label = activity.getString(R.string.app_name);
        Bitmap icon = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
        int color = ContextCompat.getColor(activity, R.color.white);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 28+: Icon is governed by the manifest launcher icon;
            // TaskDescription.Builder configures the header bar color and label
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription.Builder().build();
            ActivityManager.TaskDescription.Builder taskDescriptionBuilder = new ActivityManager.TaskDescription.Builder();
            taskDescriptionBuilder.setLabel(label);
            taskDescriptionBuilder.setPrimaryColor(color);
            activity.setTaskDescription(taskDescription);
        }else{
            // Legacy implementation (API 21 to 29) using deprecated constructor
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(label, icon, color);
            activity.setTaskDescription(taskDescription);
        }
    }

    // show "no app found" dialog
    public void showNoAppDialog() {
        alertDialog.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_dialog_alert_24));
        alertDialog.setTitle(R.string.no_app_found_title);
        alertDialog.setMessage(R.string.no_app_found_message);
        alertDialog.create();
        alertDialog.show();
    }

    public void showJsConfirmDialog(String message, final JsResult result) {
        alertDialog.setMessage(message);
        alertDialog.setCancelable(true);
        // Action if user selects 'yes'
        alertDialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> result.confirm());
        // Actions if user selects 'no'
        alertDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> result.cancel());
        // Create the alert dialog using alert dialog builder
        alertDialog.create();
        // Finally, display the dialog when user press back button
        alertDialog.show();
    }

     // Exit app
    public void exitApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public void showExitDialog() {
        alertDialog.setTitle(R.string.exit_title);
        alertDialog.setMessage(R.string.exit_message);
        alertDialog.setCancelable(true);
        // Action if user selects 'yes'
        alertDialog.setPositiveButton(R.string.yes, (dialogInterface, i) -> exitApp());
        // Actions if user selects 'no'
        alertDialog.setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss());
        // Create the alert dialog using alert dialog builder
        alertDialog.create();
        // Finally, display the dialog when user press back button
        alertDialog.show();
    }
}
