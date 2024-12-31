package com.ayursh.android.utils;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.ayursh.android.R;
import com.ayursh.android.activities.agora.EngineEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Restarter extends JobIntentService  { // IEventListener,


    public static String NOTIFICATION_CHANNEL_PRIMARY = "notification_channel_primary" ;
    private String TAG = "Restarter";

    public static int JOB_ID = 100433;
    Context ctx;

    public static Integer NOTIFICATION_ID = 110;
    public static Integer NOTIFICATION_ID_PRIMARY = 1100;
    public static Boolean notification = false;
    public static Boolean CallCancelled = false;
    public static Boolean incoming = false;
    public static Boolean audvid = false;
    public static HashMap<String, Integer> vdomap = new HashMap<String, Integer>();
    public static HashMap<String, Integer> audmap= new HashMap<String, Integer>();
    public static String appState = "Background";
    public static Double time=0.0;
    public static Vibrator v;
    public static MediaPlayer mediaPlayer;
    public static Timer Service_timer=new Timer();
    public static PowerManager.WakeLock screenLock;
    public static Boolean autoTime=false;
    public static Intent CallIntent;



    public String CUSTOM_INTENT = "com.test.intent.action.ALARM";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
            Log.e("home_button","home button");

        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("SmartTracker is Running...")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            Notification notification = builder.build();
            startForeground(1, notification);
            Log.e("home_button_value","home_button_value");

        }
        return super.onStartCommand(intent, flags, startId);

    }

    private void startMyOwnForeground(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: " );
        ctx = this;

        EngineEventListener engineEventListener=new EngineEventListener();
//        engineEventListener.registerEventListener(this);



    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {


    }

    public void enqueueWork(Context ctx, Intent intent) {
        JobIntentService.enqueueWork(ctx, AlarmReceiver.class, JOB_ID, intent);
    }






    private PendingIntent pendingIntent(){
        Intent alarmIntent = new Intent(ctx,AlarmReceiver.class );
        alarmIntent.setAction(CUSTOM_INTENT);

        return PendingIntent.getBroadcast(ctx,0,alarmIntent,PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @RequiresApi(Build.VERSION_CODES.M)
    public void setAlarm(boolean force) {
        //cancelAlarm();
        AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        long delay = 2000 ;

        long currentTimeMillisWhen = System.currentTimeMillis();
        if (!force) {
            currentTimeMillisWhen += delay;
        }

         int SDK_INT = Build.VERSION.SDK_INT;
        switch(SDK_INT) {

            case Build.VERSION_CODES.KITKAT :
                alarm.set(AlarmManager.RTC_WAKEUP, currentTimeMillisWhen, pendingIntent());
                break;
            case Build.VERSION_CODES.M :
                alarm.setExact(AlarmManager.RTC_WAKEUP, currentTimeMillisWhen, pendingIntent());
                break;
            default:
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTimeMillisWhen, pendingIntent());

        }
    }


}
