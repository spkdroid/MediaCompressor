package com.freddieptf.meh.imagecompressor.services;

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

import com.freddieptf.meh.imagecompressor.CompressPicActivity;
import com.freddieptf.meh.imagecompressor.R;
import com.freddieptf.meh.imagecompressor.utils.CompressUtils;
import com.freddieptf.meh.imagecompressor.utils.MediaUtils;

/**
 * Created by freddieptf on 16/07/16.
 */
public class CameraActionHandlerService extends Service {

    private static final String TAG               = "CompressService";
    public static final String MEDIA_URI          = "media_uri";
    public static final String PIC_PATH           = "pic_path";
    public static final String ACTION_COMPRESS    = "action_compress";
    public static final String ACTION_STOP        = "action_stop";
    public static final int NOTIFICATION_ID       = 232;
    NotificationManager notificationManager;
    NotificationCompat.BigPictureStyle bigPictureStyle;
    String picPath;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        picPath = "";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + startId);
        notificationManager.cancel(NOTIFICATION_ID);
        if(i.getAction() != null){
            switch (i.getAction()){
                case "android.hardware.action.NEW_PICTURE":
                    initPicService(i);
                    break;
                case "android.hardware.action.NEW_VIDEO":
                    initVideoService(i);
                    break;
                case ACTION_COMPRESS:
                    sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); //close the notifications drawer
                    Intent intentDialogActivty = new Intent(this, CompressPicActivity.class);
                    intentDialogActivty.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.d(TAG, ACTION_COMPRESS + ": " + picPath);
                    intentDialogActivty.putExtra(PIC_PATH, new String[]{picPath});
                    startActivity(intentDialogActivty);
                case ACTION_STOP:
                    Log.d(TAG, "STOP");
                    this.stopService(new Intent(this, CameraActionHandlerService.class)); //stop the service when after any of our actions is consumed
                    break;
                default:
            }
        }
        return START_NOT_STICKY;
    }

    private void initPicService(Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Expand to view compress options");

        bigPictureStyle = new NotificationCompat.BigPictureStyle(builder);
        bigPictureStyle.bigPicture(getBitMapForNotification(Uri.parse(intent.getStringExtra(MEDIA_URI)), builder));
        bigPictureStyle.setSummaryText("Compress this image?");

        Intent intentCompress = new Intent(this, CameraActionHandlerService.class);
        intentCompress.setAction(ACTION_COMPRESS);
        intentCompress.putExtra(PIC_PATH, picPath);
        PendingIntent pendingIntentCompress =
                PendingIntent.getService(this, 3454, intentCompress, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentStopService = new Intent(this, CameraActionHandlerService.class);
        intentStopService.setAction(ACTION_STOP);
        PendingIntent pendingIntentStopService = PendingIntent.getService(this, 3534, intentStopService, PendingIntent.FLAG_ONE_SHOT);

        builder.addAction(R.drawable.ic_gavel_black_24dp, "compress", pendingIntentCompress);
        builder.setDeleteIntent(pendingIntentStopService);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public Bitmap getBitMapForNotification(Uri picUri, NotificationCompat.Builder builder){
        picPath = MediaUtils.getPicPathsFromPicUris(getBaseContext(), new Uri[]{picUri})[0];
        Log.d(TAG, "PIC_PATH: " + picPath);
        builder.setLargeIcon(CompressUtils.scaleImageForPreview(picPath, 100));
        return CompressUtils.scaleImageForPreview(picPath, 300);
    }

    private void initVideoService(Intent i){
        Log.d(TAG, i.getAction());
    }
}
