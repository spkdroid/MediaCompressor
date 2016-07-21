package com.freddieptf.meh.imagecompressor;

import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by freddieptf on 20/07/16.
 */
public class CompressImgsService extends Service {

    ExecutorService executorService;
    public static final String EXTRA_WIDTH = "twidth";
    public static final String EXTRA_HEIGHT = "theight";
    public static final String EXTRA_IN_SAMPLE_SIZE = "sample_size";
    public static final String EXTRA_PIC_PATHS = "pic_paths";
    public static final String EXTRA_QUALITY = "pic_quality";
    String[] paths;
    private static final String TAG = "CompressImgsService";

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        paths = intent.getStringArrayExtra(EXTRA_PIC_PATHS);
        Log.d(TAG, "length: " + paths.length);
        for(String path : paths){
            executorService.submit(new CompressPic(intent.getIntExtra(EXTRA_WIDTH, 0),
                    intent.getIntExtra(EXTRA_HEIGHT, 0),
                    intent.getIntExtra(EXTRA_QUALITY, 0),
                    intent.getIntExtra(EXTRA_IN_SAMPLE_SIZE, 0),
                    path));
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    public class CompressPic extends Thread {
        int width, height, quality, sampleSize;
        String path;
        public CompressPic(int width, int height, int quality, int sampleSize, String path){
            this.width = width;
            this.height = height;
            this.quality = quality;
            this.sampleSize = sampleSize;
            this.path = path;
        }

        @Override
        public void run() {
            Log.d(TAG, path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            CompressUtils.compressPic(new File(path), options, quality, width, height);
        }

    }
}
