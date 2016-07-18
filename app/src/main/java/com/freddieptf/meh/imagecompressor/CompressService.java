package com.freddieptf.meh.imagecompressor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by freddieptf on 16/07/16.
 */
public class CompressService extends Service {

    public static final String PIC_URI         = "pic_uri";
    public static final String PIC_PATH        = "pic_path";
    public static final String ACTION_COMPRESS = "action_compress";
    public static final String ACTION_STOP     = "action_stop";
    public static final int NOTIFICATION_ID    = 232;
    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    NotificationCompat.BigPictureStyle bigPictureStyle;
    private static final String TAG = "CompressService";
    String picPath;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + startId);

        if(i.getAction() == null){
            init(i);
        }else {
            if(i.getAction().equals(ACTION_COMPRESS)){
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); //close the notifications drawer
                Intent intentDialogActivty = new Intent(this, CompressPicActivity.class);
                intentDialogActivty.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentDialogActivty.putExtra(PIC_PATH, picPath);
                startActivity(intentDialogActivty);
            }else if(i.getAction().equals(ACTION_STOP)){
                Log.d(TAG, "STOP");
                this.stopService(new Intent(this, CompressService.class));
            }
        }

        return START_NOT_STICKY;
    }

    private void init(Intent intent) {
        builder.setContentTitle("Image Compressor")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Expand to view compress options");

        bigPictureStyle = new NotificationCompat.BigPictureStyle(builder);
        bigPictureStyle.bigPicture(getBitMapForNotification(Uri.parse(intent.getStringExtra(PIC_URI))));
        bigPictureStyle.setSummaryText("Compress this image?");

        Intent intentCompress = new Intent(this, CompressService.class);
        intentCompress.setAction(ACTION_COMPRESS);
        intentCompress.putExtra(PIC_PATH, picPath);
        PendingIntent pendingIntentCompress =
                PendingIntent.getService(this, 3454, intentCompress, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentStopService = new Intent(this, CompressService.class);
        intentStopService.setAction(ACTION_STOP);
        PendingIntent pendingIntentStopService = PendingIntent.getService(this, 3534, intentStopService, PendingIntent.FLAG_ONE_SHOT);

        builder.addAction(R.drawable.ic_gavel_black_24dp, "compress", pendingIntentCompress);
        builder.setDeleteIntent(pendingIntentStopService);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public Bitmap getBitMapForNotification(Uri picUri){
        picPath = MainActivity.getPicPathFromPicUri(getBaseContext(), picUri);
        Log.d(TAG, "PIC_PATH: " + picPath);
        builder.setLargeIcon(CompressUtils.scaleImageForPreview(picPath, 100));
        return CompressUtils.scaleImageForPreview(picPath, 300);
    }
}
