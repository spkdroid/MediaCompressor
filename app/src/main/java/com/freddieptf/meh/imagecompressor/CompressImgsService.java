package com.freddieptf.meh.imagecompressor;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.BitmapFactory;

import java.io.File;

/**
 * Created by freddieptf on 20/07/16.
 */
public class CompressImgsService extends IntentService {

    public static final String EXTRA_WIDTH          = "twidth";
    public static final String EXTRA_HEIGHT         = "theight";
    public static final String EXTRA_IN_SAMPLE_SIZE = "sample_size";
    public static final String EXTRA_PIC_PATHS      = "pic_paths";
    public static final String EXTRA_QUALITY        = "pic_quality";
    public static final String COMPRESSION_DONE     = "progress_update";
    String[] paths;
    private static final String TAG = "CompressImgsService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     **/
    public CompressImgsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        paths = intent.getStringArrayExtra(EXTRA_PIC_PATHS);
        int sampleSize = intent.getIntExtra(EXTRA_IN_SAMPLE_SIZE, 0);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        for(String path : paths) {
            CompressUtils.compressPic(new File(path), options,
                    intent.getIntExtra(EXTRA_QUALITY, 0),
                    intent.getIntExtra(EXTRA_WIDTH, 0),
                    intent.getIntExtra(EXTRA_HEIGHT, 0));
        }

        Intent i = new Intent(COMPRESSION_DONE);
        i.putExtra("num_pics", paths.length);
        sendBroadcast(i);

    }
}
