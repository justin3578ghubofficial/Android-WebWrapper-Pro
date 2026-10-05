package at.cosmosinsurance.online;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationUtils extends ContextWrapper {

    // Use standard NotificationManager for channel creation
    private NotificationManager mManager; 
    private NotificationManagerCompat mCompatManager;

    public static final String PRIVATE_CHANNEL_ID = "at.cosmosinsurance.online.private";
    public static final String PUBLIC_CHANNEL_ID = "at.cosmosinsurance.online.public";
    public static final String PRIVATE_CHANNEL_NAME = "PRIVATE CHANNEL";
    public static final String PUBLIC_CHANNEL_NAME = "PUBLIC CHANNEL";

    public NotificationUtils(Context base) {
        super(base);
        createChannels();
    }

    public void createChannels() {
        // Notification channels are only available/required on Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create private channel 
            NotificationChannel androidChannel = new NotificationChannel(PRIVATE_CHANNEL_ID, PRIVATE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT); 
            androidChannel.enableLights(true); 
            androidChannel.enableVibration(true); 
            androidChannel.setLightColor(Color.BLUE); 
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE); 
            getManager().createNotificationChannel(androidChannel); 

            // Create public channel 
            NotificationChannel iosChannel = new NotificationChannel(PUBLIC_CHANNEL_ID, PUBLIC_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW); 
            iosChannel.enableLights(true); 
            iosChannel.enableVibration(true); 
            iosChannel.setLightColor(Color.GRAY); 
            iosChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); 
            getManager().createNotificationChannel(iosChannel); 
        }
    }

    // Native manager wrapper for channel management
    private NotificationManager getManager() { 
        if (mManager == null) { 
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); 
        } 
        return mManager; 
    }

     // Compat manager wrapper for posting/canceling notifications safely across versions
    private NotificationManagerCompat getCompatManager() { 
        if (mCompatManager == null) { 
            mCompatManager = NotificationManagerCompat.from(getApplicationContext()); 
        } 
        return mCompatManager; 
    } 

    public NotificationCompat.Builder getPrivateChannelNotification(String title, String body) { 
        return new NotificationCompat.Builder(getApplicationContext(), PRIVATE_CHANNEL_ID) 
                .setContentTitle(title) 
                .setContentText(body) 
                .setSmallIcon(android.R.drawable.stat_notify_more) 
                .setAutoCancel(true); 
    }

     public NotificationCompat.Builder getPublicChannelNotification(String title, String body) { 
        return new NotificationCompat.Builder(getApplicationContext(), PUBLIC_CHANNEL_ID) 
                .setContentTitle(title) 
                .setContentText(body) 
                .setSmallIcon(android.R.drawable.stat_notify_more) 
                .setAutoCancel(true); 
    } 

    public void showPrivateNotification(String title, String body, int notificationId) { 
        NotificationCompat.Builder privateNotificationBuilder = getPrivateChannelNotification(title, body); 
        getCompatManager().notify(notificationId, privateNotificationBuilder.build()); 
    } 

    public void showPublicNotification(String title, String body, int notificationId) { 
        NotificationCompat.Builder publicNotificationBuilder = getPublicChannelNotification(title, body); 
        getCompatManager().notify(notificationId, publicNotificationBuilder.build()); 
    } 

    public void cancelNotification(int notificationId) { 
        getCompatManager().cancel(notificationId); 
    } 

    public void cancelAllNotifications() { 
        getCompatManager().cancelAll(); 
    } 
}
