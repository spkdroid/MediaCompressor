package com.freddieptf.meh.imagecompressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by freddieptf on 16/07/16.
 */
public class CameraActionReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraActionReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "OnReceive");
        if(intent != null && intent.getData() != null) {
            Log.d(TAG, intent.getData() + "");
            Intent compressServiceIntent = new Intent(context, CompressService.class);
            compressServiceIntent.putExtra(CompressService.PIC_URI, intent.getData().toString());
            context.startService(compressServiceIntent);
        }
    }
}
