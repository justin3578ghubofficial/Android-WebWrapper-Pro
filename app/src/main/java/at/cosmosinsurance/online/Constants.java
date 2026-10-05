package at.cosmosinsurance.online;

import static at.cosmosinsurance.online.webview.WebViewHelper.getHost;
import static at.cosmosinsurance.online.webview.WebViewHelper.getBaseDomain;

public class Constants {
    public Constants(){}
    // Root page
	public static String DEFAULT_URL = "https://www.sacrt.com";
	public static String WEBAPP_URL = DEFAULT_URL;
	public static String WEBAPP_HOST = getHost(WEBAPP_URL);
	public static String WEBAPP_DOMAIN = getBaseDomain(WEBAPP_URL);
	

	public static boolean REFRESH = true;
    public static boolean EXIT_ON_BACK = true;
    public static boolean SHOW_EXIT_DIALOG = true;


	// User Agent tweaks
    public static boolean POSTFIX_USER_AGENT = true; // set to true to append USER_AGENT_POSTFIX to user agent
    public static boolean OVERRIDE_USER_AGENT = false; // set to true to use USER_AGENT instead of default one
    public static String USER_AGENT_POSTFIX = "CosmosOnlineApp"; // useful for identifying traffic, e.g. in Google Analytics
    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36";
	
	// Constants
    // window transition duration in ms
    public static int SLIDE_EFFECT = 2200;
    // show your app when the page is loaded XX %.
    // lower it, if you've got server-side rendering (e.g. to 35),
    // bump it up to ~98 if you don't have SSR or a loading screen in your web app
    public static int PROGRESS_THRESHOLD = 65;
    // turn on/off mixed content (both https+http within one page) for API >= 21
    public static boolean ENABLE_MIXED_CONTENT = true;
}
