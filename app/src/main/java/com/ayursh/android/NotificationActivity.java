package com.ayursh.android;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import com.ayursh.android.utils.ExtensionsKt;
import com.ayursh.android.utils.Restarter;

public class NotificationActivity extends Activity {
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExtensionsKt.RefuseRemoteInvitation(NotificationActivity.this, this,
                getIntent().getStringExtra("callType")+" Call", getIntent().getStringExtra("fcm"));
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(getIntent().getIntExtra(NOTIFICATION_ID, 1));
        if(Restarter.v!=null) {
            Restarter.v.cancel();
        }
        if(Restarter.mediaPlayer!=null) {
            Restarter.mediaPlayer.stop();
        }
        if(Restarter.Service_timer!=null) {
            Restarter.Service_timer.cancel();
        }
        if(Restarter.autoTime){
            Restarter.autoTime=false;
        }
        Restarter.screenLock.release();
        Log.e("NotificationActivity", "onCreate: End call through noti" );

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(Restarter.NOTIFICATION_ID_PRIMARY);
        Restarter.notification=false;
        finish(); // since finish() is called in onCreate(), onDestroy() will be called immediately

    }




    public static PendingIntent getDismissIntent(int notificationId, Context context, String callType, String fcm) {
        Log.e("NotificationActivity", "getDismissIntent: " );
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra("callType",callType);
        intent.putExtra("fcm",fcm);
        PendingIntent dismissIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return dismissIntent;
    }
}
