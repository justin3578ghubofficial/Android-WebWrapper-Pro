package at.cosmosinsurance.online;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import at.cosmosinsurance.online.ui.UIManager;
import at.cosmosinsurance.online.webview.WebViewHelper;

public class MainActivity extends AppCompatActivity {
    // Globals
    private UIManager uiManager;
    private WebViewHelper webViewHelper;
    private boolean intentHandled = false;

    public static final int REQUEST_SELECT_FILE = 100;
    public static final int FILECHOOSER_RESULTCODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Theme
        setTheme(R.style.Theme_AppTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Setup Helpers
        uiManager = new UIManager(this);
        webViewHelper = new WebViewHelper(this, uiManager);

        // Setup App
        uiManager.setupSwipeContainer();
        webViewHelper.initWebView();
        uiManager.changeRecentAppsIcon();
        checkAndRequestPermissions();

         // Modern Back Press Handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Constants.EXIT_ON_BACK) {
                    if (Constants.SHOW_EXIT_DIALOG) {
                        uiManager.showExitDialog();
                    } else {
                        finish();
                    }
                    return;
                }
                if (!webViewHelper.goBack()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                } else {
                    if (Config.SHOW_EXIT_DIALOG) {
                        uiManager.showExitDialog();
                    } else {
                        finish();
                    }
                }
            }
        });

        // Check for Intents
        try {
            Intent intent = getIntent();
            String intentAction = (intent != null) ? intent.getAction() : null;
            if (!intentHandled && Intent.ACTION_VIEW.equals(intentAction)) {
                Uri intentUri = (intent != null) ? intent.getData() : null;
                if (intentUri != null && !TextUtils.isEmpty(intentUri.toString())) {
                    intentHandled = true;
                    webViewHelper.loadIntentUrl(intentUri.toString());
                    return;
                }
            }
            // Only load home if nothing has been handled yet
            if (!intentHandled) {
                webViewHelper.loadHome();
            }
        } catch (Exception e) {
            if (!intentHandled) {
                webViewHelper.loadHome();
            }
        }
    }    

    @Override
    protected void onPause() {
        webViewHelper.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        webViewHelper.onResume();
        // retrieve content from cache primarily if not connected
        webViewHelper.forceCacheIfOffline();
        super.onResume();
    }

    // Handle back-press in browser
    @Override
    public void onBackPressed() {
        if (!webViewHelper.goBack()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
//                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            if (!checkPermission(permission)) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (requestCode == REQUEST_SELECT_FILE)
            {
                if (webViewHelper.uploadMessage == null)
                    return;
                webViewHelper.uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                webViewHelper.uploadMessage = null;
            }
        }
        else if (requestCode == FILECHOOSER_RESULTCODE)
        {
            if (null == webViewHelper.mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            webViewHelper.mUploadMessage.onReceiveValue(result);
            webViewHelper.mUploadMessage = null;
        }
        else
            Toast.makeText(getApplicationContext(), "Failed to File", Toast.LENGTH_LONG).show();
    }
}
