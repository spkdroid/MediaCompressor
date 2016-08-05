package com.freddieptf.meh.imagecompressor.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.freddieptf.meh.imagecompressor.services.CameraActionHandlerService;

/**
 * Created by freddieptf on 16/07/16.
 */
public class CameraActionReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraActionReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "OnReceive");
        if(intent != null && intent.getData() != null) {
            Log.d(TAG, intent.getData() + "\n" + intent.getAction());
            Intent compressServiceIntent = new Intent(context, CameraActionHandlerService.class);
            compressServiceIntent.putExtra(CameraActionHandlerService.MEDIA_URI, intent.getData().toString());
            compressServiceIntent.setAction(intent.getAction());
            context.startService(compressServiceIntent);
        }
    }
}
