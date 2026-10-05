package at.cosmosinsurance.online.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.ProgressBar;

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
    private boolean pageLoaded = false;

    public UIManager(Activity activity) {
        this.activity = activity;
        this.progressBar = activity.findViewById(R.id.progressBar);
        this.progressSpinner = activity.findViewById(R.id.progressSpinner);
        this.swipeContainer = activity.findViewById(R.id.swipeContainer);
        this.webView = activity.findViewById(R.id.webView);
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
}
