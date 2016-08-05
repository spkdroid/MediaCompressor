package com.freddieptf.meh.imagecompressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.freddieptf.meh.imagecompressor.adapters.VPagerAdapter;
import com.freddieptf.meh.imagecompressor.services.CameraActionHandlerService;
import com.freddieptf.meh.imagecompressor.services.CompressService;
import com.freddieptf.meh.imagecompressor.utils.ImageCache;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by freddieptf on 20/07/16.
 */
public class VideoActivity extends AppCompatActivity {

    boolean ffmpegEnabled = false;
    private static final String TAG = "CompressVidActivity";
    TextView tvVidName, tvVidDuration, tvVidResolution, tvVidSize;
    ViewPager viewPager;
    TabLayout tabLayout;
    ImageView thumbnail;
    String[] videoDetails;
    public static final String VID_DETS = "vid_dets";

    public static final int KEY_PATH       = 0;
    public static final int KEY_TITLE      = 1;
    public static final int KEY_DURATION   = 2;
    public static final int KEY_ROTATION   = 3;
    public static final int KEY_RESOLUTION = 4;
    public static final int KEY_MIMETYPE   = 5;
    public static final int KEY_SIZE       = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        thumbnail       = (ImageView) findViewById(R.id.iv_thumbnail);
        tvVidName       = (TextView) findViewById(R.id.tv_videoName);
        tvVidDuration   = (TextView) findViewById(R.id.tv_videoDuration);
        tvVidResolution = (TextView) findViewById(R.id.tv_videoResolution);
        tvVidSize       = (TextView) findViewById(R.id.tv_videoSize);
        viewPager       = (ViewPager) findViewById(R.id.pager);
        tabLayout       = (TabLayout) findViewById(R.id.tabLayout);

        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("ff")) ffmpegEnabled = savedInstanceState.getBoolean("ff");
            else initFfmpeg();
            if(savedInstanceState.containsKey(VID_DETS)) {
                videoDetails = savedInstanceState.getStringArray(VID_DETS);
            }
            else {
                videoDetails = getVideoDetailsFromUri(this,
                        Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
            }
        }else {
            initFfmpeg();
            videoDetails = getVideoDetailsFromUri(this,
                    Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
        }

        initVideoDetailView(videoDetails);
        VPagerAdapter vPagerAdapter   = new VPagerAdapter(getSupportFragmentManager());
        ArrayList<Fragment> fragments = new ArrayList<>();

        Bundle b = new Bundle();
        b.putStringArray(VID_DETS, videoDetails);

        FragmentVidScaling f = new FragmentVidScaling();
        FragmentVidConversion f2 = new FragmentVidConversion();
        f.setArguments(b);
        f2.setArguments(b);
        fragments.add(f);
        fragments.add(f2);

        vPagerAdapter.setFrags(fragments);

        viewPager.setAdapter(vPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ff", ffmpegEnabled);
        if(videoDetails != null && videoDetails.length > 0) outState.putStringArray(VID_DETS, videoDetails);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(CompressService.PROGRESS_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void initFfmpeg(){
        final FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.d(TAG, "start");
                }

                @Override
                public void onFailure() {
                    ffmpegEnabled = false;

                }

                @Override
                public void onSuccess() {
                    ffmpegEnabled = true;
//                    runCommand();
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            Log.d(TAG, e.getMessage());
        }
    }

    public static String[] getVideoDetailsFromUri(Context c, Uri vidUri) {
        String[] columns = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        Cursor cursor = c.getContentResolver().query(vidUri, columns, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(c, "error loading video from path", Toast.LENGTH_LONG).show();
            return null;
        }
        cursor.moveToFirst();
        String[] dets = new String[7];
        dets[0] = cursor.getString(cursor.getColumnIndex(columns[0])); //path 0

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(dets[0]);

        dets[1] = cursor.getString(cursor.getColumnIndex(columns[2]));
        dets[2] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        dets[3] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if(Integer.parseInt(dets[3]) == 0) { //when the phone is in landscape mode.."normal video"
            dets[4] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    + "x" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        }else if (Integer.parseInt(dets[3]) >= 90) { //when the phone is in portrait mode.."vertical video"..i think..
            dets[4] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    + "x" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        }
        dets[5] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        dets[6] = cursor.getString(cursor.getColumnIndex(columns[1]));

        if(!cursor.isClosed()) cursor.close();
        retriever.release();

        return dets;
    }

    public void initVideoDetailView(final String[] videoDetails){
        Bitmap bitmap = ImageCache.getInstance().getBitmapFromCache(videoDetails[KEY_PATH]);
        if(bitmap == null) {
            thumbnail.setAlpha(0f);
            Thread loadThumbnail = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap t = ThumbnailUtils.createVideoThumbnail(videoDetails[KEY_PATH],
                            MediaStore.Video.Thumbnails.MICRO_KIND);
                    ImageCache.getInstance().addBitmapToCache(videoDetails[KEY_PATH], t);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thumbnail.setImageBitmap(t);
                            thumbnail.animate().alpha(1f).setDuration(250);
                        }
                    });
                }
            });
            loadThumbnail.start();
        }else {
            thumbnail.setImageBitmap(bitmap);
        }

        tvVidName.setText(videoDetails[KEY_TITLE]);
        tvVidDuration.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(videoDetails[KEY_DURATION]))) + "seconds");
        tvVidResolution.setText(videoDetails[KEY_RESOLUTION]);
        tvVidSize.setText(Long.parseLong(videoDetails[KEY_SIZE])/1024000 + "mb");
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getStringExtra(CompressService.PROGRESS_UPDATE));
            progressListener.update(intent.getStringExtra(CompressService.PROGRESS_UPDATE));
        }
    };

    ProgressListener progressListener;

    public void setProgressListener(ProgressListener progressListener){
        this.progressListener = progressListener;
    }

    public interface ProgressListener {
        void update(String progress);
    }
}
