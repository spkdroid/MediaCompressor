package com.freddieptf.meh.imagecompressor.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.freddieptf.meh.imagecompressor.utils.CompressUtils;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;

/**
 * Created by freddieptf on 20/07/16.
 */
public class CompressService extends IntentService {

    public static final String EXTRA_WIDTH          = "twidth";
    public static final String EXTRA_HEIGHT         = "theight";
    public static final String EXTRA_IN_SAMPLE_SIZE = "sample_size";
    public static final String EXTRA_PIC_PATHS      = "pic_paths";
    public static final String EXTRA_VID_CMD        = "pic_paths";
    public static final String EXTRA_QUALITY        = "pic_quality";
    public static final String PROGRESS_UPDATE      = "progress_update";
    public static final String TASK_SUCCESS         = "success";
    public static final String ACTION_COMPRESS_PIC  = "compress_pic";
    public static final String ACTION_COMPRESS_VID  = "compress_vid";


    String[] paths;
    private static final String TAG = "CompressImgsService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     **/
    public CompressService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent.getAction() != null) {
            switch (intent.getAction()) {

                case ACTION_COMPRESS_PIC:
                    Log.d(TAG, ACTION_COMPRESS_PIC);
                    paths = intent.getStringArrayExtra(EXTRA_PIC_PATHS);
                    int sampleSize = intent.getIntExtra(EXTRA_IN_SAMPLE_SIZE, 0);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = sampleSize;

                    for (String path : paths) {
                        CompressUtils.compressPic(new File(path), options,
                                intent.getIntExtra(EXTRA_QUALITY, 0),
                                intent.getIntExtra(EXTRA_WIDTH, 0),
                                intent.getIntExtra(EXTRA_HEIGHT, 0));
                    }

                    Intent i = new Intent(PROGRESS_UPDATE);
                    i.putExtra("num_pics", paths.length);
                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
                    break;

                case ACTION_COMPRESS_VID:
                    Log.d(TAG, ACTION_COMPRESS_VID);
                    String[] command = intent.getStringArrayExtra(EXTRA_VID_CMD);
                    final Intent progressIntent = new Intent(PROGRESS_UPDATE);
                    try {
                        CompressUtils.compressVid(this, command, new FFmpegExecuteResponseHandler() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_LONG).show();
                                progressIntent.putExtra(TASK_SUCCESS, true);
                                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(progressIntent);
                                FFmpeg.getInstance(getBaseContext()).killRunningProcesses();
                            }

                            @Override
                            public void onProgress(String message) {
                                progressIntent.putExtra(PROGRESS_UPDATE, message);
                                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(progressIntent);
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(getBaseContext(), "failure! ", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onFinish() {
                                FFmpeg.getInstance(getBaseContext()).killRunningProcesses();
                            }
                        });
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                        FFmpeg.getInstance(getBaseContext()).killRunningProcesses();
                        Toast.makeText(getBaseContext(), "Stopped running process. Try again! ", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }

    }
}
