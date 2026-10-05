package at.cosmosinsurance.online.webview;

import static android.content.Context.DOWNLOAD_SERVICE;
import static at.cosmosinsurance.online.MainActivity.FILECHOOSER_RESULTCODE;
import static at.cosmosinsurance.online.MainActivity.REQUEST_SELECT_FILE;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.webResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.cosmosinsurance.online.Constants;
import at.cosmosinsurance.online.R;
import at.cosmosinsurance.online.ui.UIManager;

public class WebViewHelper {
    // Instance variables
    private Activity activity;
    private UIManager uiManager;
    private WebView webView;
    private WebSettings webSettings;
    private AlertDialog.Builder alertDialog;
    public ValueCallback<Uri[]> uploadMessage;
    public ValueCallback<Uri> mUploadMessage;

    public WebViewHelper(Activity activity, UIManager uiManager) {
        this.activity = activity;
        this.uiManager = uiManager;
        this.webView = (WebView) activity.findViewById(R.id.webView);
        this.webSettings = webView.getSettings();
        WebView.setWebContentsDebuggingEnabled(false);
    }

    public WebView getWebView() {
        return this.webView;
    }

   /**
     * Checks if the device has an active network connection.
     * Compatible with both legacy and API 23+ NetworkCapabilities.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    // manipulate cache settings to make sure our PWA gets updated
    private void useCache(Boolean use) {
        if (use) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
    }

    // public method changing cache settings according to network availability.
    // retrieve content from cache primarily if not connected,
    // allow fetching from web too otherwise to get updates.
    public void forceCacheIfOffline() {
        useCache(!isNetworkAvailable());
    }

    // handles initial setup of webview
    @SuppressLint("WrongConstant")
    public void initWebView() {
        // accept cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        
        // enable JS
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // must be set for our js-popup-blocker:
        webSettings.setSupportMultipleWindows(true);
        
        // PWA settings
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);

        // enable mixed content mode conditionally
        if (Constants.ENABLE_MIXED_CONTENT) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }else{
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        // retrieve content from cache primarily if not connected
        forceCacheIfOffline();

        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // set User Agent
        if (Constants.OVERRIDE_USER_AGENT || Constants.POSTFIX_USER_AGENT) {
            String userAgent = webSettings.getUserAgentString();
            if (Constants.OVERRIDE_USER_AGENT) {
                userAgent = Constants.USER_AGENT;
            }
            if (Constants.POSTFIX_USER_AGENT) {
                userAgent = userAgent + " " + Constants.USER_AGENT_POSTFIX;
            }
            webSettings.setUserAgentString(userAgent);
        }

        // enable HTML5-support
        webView.setWebChromeClient(new WebChromeClient() {
            //simple yet effective redirect/popup blocker
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // 1. Initialize the new WebView
                WebView mWebView = new WebView(view.getContext());
                // 2. IMPORTANT: Configure WebSettings for the new WebView if needed
                mWebView.getSettings().setJavaScriptEnabled(true); 
                // 3. IMPORTANT: Add the new WebView to your layout container so it becomes visible
                // If your main layout is a FrameLayout or RelativeLayout, add it to that container.
                // As a simple example, we can add it directly to the host view if it supports child views:
                view.addView(mWebView); 
                // 4. Pass the new WebView back to the host system transport thread
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(mWebView);
                resultMsg.sendToTarget();
                // 5. Correctly handle the URL overrides within the popup window
                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        // Use 'view' (which references newWebView here) to load the URL
                        view.loadUrl(url);
                        return true;
                    }
                });
                return true;
            }

            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                activity.startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    intent = fileChooserParams.createIntent();
                    List<String> validMimeTypes = extractValidMimeTypes(fileChooserParams.getAcceptTypes());
                    if (validMimeTypes.isEmpty()) {
                        intent.setType("*/*");
                    } else {
                        intent.setType(String.join(" ", validMimeTypes));
                    }
                }
                try {
                    activity.startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(activity.getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }


            public void showFileChooser(ValueCallback<String[]> filePathCallback, String acceptType, boolean paramBoolean) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            public void showFileChooser(ValueCallback<String[]> uploadFileCallback, FileChooserParams fileChooserParams) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            // update ProgressBar
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                uiManager.setLoadingProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                alertDialog.setMessage(message);
                alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
                alertDialog.create();
                alertDialog.show();
                return true;
            }
        });

        // Set up Webview client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                onExternalPageRequest(view, url);
            }

            // handle loading error by showing the offline screen
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                handleLoadError(view, request, error);
            } 

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                 handleHttpLoadError(view, request, errorResponse);
            }     
                
            //Handle if request comes from same hostname. If not, it may be an intent
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl(); 
                String url = uri.toString(); 
                // Checks if the URL belongs to your web app's host
                if (url.contains(Constants.WEBAPP_HOST)) { 
                    view.loadUrl(url); 
                } else { 
                    // Opens external links in the device's default browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
                    view.getContext().startActivity(intent); 
                } 
                return true; 
            }
        }):
            
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,String mimeType, long contentLength) {
                DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(url));
                // Set MIME type based on file extension
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(url);
                String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if (contentType == null) {
                    // If the MIME type is still null, fallback to the provided mimeType
                    contentType = mimeType;
                }
                downloadRequest.setMimeType(contentType);
                String cookies = CookieManager.getInstance().getCookie(url);
                downloadRequest.addRequestHeader("cookie", cookies);
                downloadRequest.addRequestHeader("User-Agent", userAgent);
                downloadRequest.setDescription(activity.getString(R.string.dl_downloading));
                downloadRequest.setTitle(URLUtil.guessFileName(url, contentDisposition, contentType));
                downloadRequest.allowScanningByMediaScanner();
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager downloadManager = (DownloadManager) activity.getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
                assert downloadManager != null;
                downloadManager.enqueue(downloadRequest);
                Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Lifecycle callbacks
    public void onPause() {
        webView.onPause();
    }

    @SuppressLint("WrongConstant")
    public void onResume() {
        webView.onResume();
    }

    // show "no app found" dialog
    private void showNoAppDialog() {
        alertDialog.setTitle(R.string.noapp_heading);
        alertDialog.setMessage(R.string.noapp_description);
        alertDialog.create();
        alertDialog.show();
    }

    // handle load errors
    private void handleLoadError(WebView view, WebResource request, webResourceError error) {
         // Check if the error happened on the main frame page request
        if (request.isForMainFrame()) {
            // Get details about the error
            int errorCode = error.getErrorCode();
            CharSequence description = error.getDescription();
            view.loadUrl("file:///android_asset/error_page.html");
    }

    private void handleHttpLoadError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        // Check if the server-side HTTP error belongs to the primary page
        if (request.isForMainFrame()) {
            int statusCode = errorResponse.getStatusCode(); // e.g., 404, 500
            String reasonPhrase = errorResponse.getReasonPhrase(); // e.g., "Not Found"
            // Handle specific server states
            if (statusCode == 404) {
                view.loadUrl("file:///android_asset/not_found.html");
            } else if (statusCode >= 500) {
                view.loadUrl("file:///android_asset/server_error.html");
        }
    }
        

    private List<String> extractValidMimeTypes(String[] mimeTypes) {
        List<String> results = new ArrayList<String>();
        List<String> mimes;
        if (mimeTypes.length == 1 && mimeTypes[0].contains(",")) {
            mimes = Arrays.asList(mimeTypes[0].split(","));
        } else {
            mimes = Arrays.asList(mimeTypes);
        }
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        for (String mime : mimes) {
            if (mime != null && mime.trim().startsWith(".")) {
                String extensionWithoutDot = mime.trim().substring(1, mime.trim().length());
                String derivedMime = mtm.getMimeTypeFromExtension(extensionWithoutDot);
                if (derivedMime != null && !results.contains(derivedMime)) {
                    // adds valid mime type derived from the file extension
                    results.add(derivedMime);
                }
            } else if (mimeTypeMap.getExtensionFromMimeType(mime) != null && !results.contains(mime)) {
                // adds valid mime type checked agains file extensions mappings
                results.add(mime);
            }
        }
        return results;
    }

    // handle external urls
   private boolean onExternalPageRequest(WebView view, String url) { 
        // 1. Precise domain validation to prevent phishing or subdomain bypasses
        boolean isInternal = url.startsWith(Constants.WEBAPP_URL) || url.contains(Constants.WEBAPP_DOMAIN);
        if (!isInternal) {
            // 2. Open external URL in a 3rd party app or browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                // Add flag to ensure external links don't launch inside your own task stack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(intent);
                } else {
                    showNoAppDialog();
                }
            } catch (Exception e) {
                showNoAppDialog();
            }
            // 3. DO NOT call view.loadUrl() here. Returning true successfully 
            // aborts the external load, leaving the WebView safely on its current internal page.
            return true; 
        } else {
            // 4. Handle internal navigation safely
            uiManager.setLoading(true);
            return false;
        }
     }
    
    // Will take a url such as http://www.stackoverflow.com and return www.stackoverflow.com
    public static String getHost(String url){
        if(url == null || url.isEmpty())
            return "";

        int doubleslash = url.indexOf("//");
        if(doubleslash == -1)
            doubleslash = 0;
        else
            doubleslash += 2;

        int end = url.indexOf('/', doubleslash);
        end = end >= 0 ? end : url.length();

        int port = url.indexOf(':', doubleslash);
        end = (port > 0 && port < end) ? port : end;

        return url.substring(doubleslash, end);
    }

    // Get the base domain for a given host or url. E.g. mail.google.com will return google.com
    public static String getBaseDomain(String url) {
        String host = getHost(url);
        int startIndex = 0;
        int nextIndex = host.indexOf('.');
        int lastIndex = host.lastIndexOf('.');
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1;
            nextIndex = host.indexOf('.', startIndex);
        }
        if (startIndex > 0) {
            return host.substring(startIndex);
        } else {
            return host;
        }
    }


    // handle back button press
    public boolean goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    // load app startpage
    public void loadHome() {
        webView.loadUrl(Constants.WEBAPP_URL);
    }

    // load URL from intent
    public void loadIntentUrl(String url) {
        if (!url.equals("") && url.contains(Constants.WEBAPP_HOST)) {
            webView.loadUrl(url);
        } else {
            // Fallback
            loadHome();
        }
    }

}
