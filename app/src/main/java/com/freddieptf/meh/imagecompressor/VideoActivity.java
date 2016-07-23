package com.freddieptf.meh.imagecompressor;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

/**
 * Created by freddieptf on 20/07/16.
 */
public class VideoActivity extends AppCompatActivity {

    boolean ffmpegEnabled = false;
    private static final String TAG = "CompressVidActivity";
    TextView tvStatus, tvMessage, tvVidName, tvVidDuration, tvVidResolution, tvVidSize;
    ImageView thumbnail;
    String[] videoDetails;
    final String VID_DETS = "vid_dets";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        thumbnail = (ImageView) findViewById(R.id.iv_thumbnail);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvMessage = (TextView) findViewById(R.id.tv_message);
        tvVidName = (TextView) findViewById(R.id.tv_videoName);
        tvVidDuration = (TextView) findViewById(R.id.tv_videoDuration);
        tvVidResolution = (TextView) findViewById(R.id.tv_videoResolution);
        tvVidSize = (TextView) findViewById(R.id.tv_videoSize);

        if(savedInstanceState != null && savedInstanceState.containsKey(VID_DETS)){
            videoDetails = savedInstanceState.getStringArray(VID_DETS);
        }else {
            videoDetails = getVideoDetailsFromUri(this,
                    Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
        }

        initVideoDetailView(videoDetails);

        try {
            CompressUtils.compressVid(this, new String[]{"-version"}, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    tvMessage.setText(message);
                }

                @Override
                public void onProgress(String message) {
                    tvStatus.setText(message);
                }

                @Override
                public void onFailure(String message) {

                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("ff")){
            ffmpegEnabled = savedInstanceState.getBoolean("ff");
        }else{
            initFfmpeg();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ff", ffmpegEnabled);
        if(videoDetails != null && videoDetails.length > 0) outState.putStringArray(VID_DETS, videoDetails);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
//        Uri mediaUri = Uri.parse("content://media/external/images/media");
        String[] columns = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.RESOLUTION,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
        };
//            if(picUri.getAuthority().equals("com.android.providers.media.documents")){
//                picUri = mediaUri.buildUpon().appendPath(picUri.getLastPathSegment().split(":")[1]).build();
//            }
        Cursor cursor = c.getContentResolver().query(vidUri, columns, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(c, "error loading video from path", Toast.LENGTH_LONG).show();
            return null;
        }
        cursor.moveToFirst();
        return new String[]{
                cursor.getString(cursor.getColumnIndex(columns[0])), //path 0
                cursor.getString(cursor.getColumnIndex(columns[1])), //display name 1
                cursor.getString(cursor.getColumnIndex(columns[2])), //duration 2
                cursor.getString(cursor.getColumnIndex(columns[3])), //resolution 3
                cursor.getString(cursor.getColumnIndex(columns[4])), //mime_type 4
                cursor.getString(cursor.getColumnIndex(columns[5])), //size 5
        };
    }

    public void initVideoDetailView(final String[] videoDetails){
        Bitmap bitmap = ImageCache.getInstance().getBitmapFromCache(videoDetails[0]);
        if(bitmap == null) {
            thumbnail.setAlpha(0f);
            Thread loadThumbnail = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap t = ThumbnailUtils.createVideoThumbnail(videoDetails[0],
                            MediaStore.Video.Thumbnails.MICRO_KIND);
                    ImageCache.getInstance().addBitmapToCache(videoDetails[0], t);
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

        tvVidName.setText(videoDetails[1]);
        StringBuilder stringBuilder = new StringBuilder();
        TimeUtils.formatDuration(Long.parseLong(videoDetails[2]), stringBuilder);
        tvVidDuration.setText(stringBuilder.toString());
        tvVidResolution.setText(videoDetails[3]);
        tvVidSize.setText(Long.parseLong(videoDetails[5])/1024000 + "mb");
    }

}
