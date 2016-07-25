package com.freddieptf.meh.imagecompressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by freddieptf on 18/07/16.
 */
public class CompressPicActivity extends AppCompatActivity {

    SeekBar seekBar;
    TextView tvQuality, tvDetailText, tvDone;
    EditResolutionView resolutionView;
    ProgressBar progressBar;
    String[] picPaths;
    private int          outWidth      = -1;
    private int          outHeight     = -1;
    private int          targetWidth   = -1;
    private int          targetHeight  = -1;
    private final String OUT_WIDTH     = "ot";
    private final String OUT_HEIGHT    = "oh";
    private final String TARGET_WIDTH  = "tw";
    private final String TARGET_HEIGHT = "th";
    private static final String TAG    = "DialogActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity);

        resolutionView = (EditResolutionView) findViewById(R.id.editResolution);
        seekBar        = (SeekBar) findViewById(R.id.seekbar_quality);
        tvQuality      = (TextView) findViewById(R.id.tv_quality);
        tvDetailText   = (TextView)findViewById(R.id.tv_detailText);
        tvDone         = (TextView) findViewById(R.id.tvDone);
        progressBar    = (ProgressBar) findViewById(R.id.progress);

        if(savedInstanceState != null && savedInstanceState.containsKey(CameraActionHandlerService.PIC_PATH)){
            picPaths     = savedInstanceState.getStringArray(CameraActionHandlerService.PIC_PATH);
            outWidth     = savedInstanceState.getInt(OUT_WIDTH);
            outHeight    = savedInstanceState.getInt(OUT_HEIGHT);
            targetWidth  = savedInstanceState.getInt(TARGET_WIDTH);
            targetHeight = savedInstanceState.getInt(TARGET_HEIGHT);
            resolutionView.setResolution(targetHeight, targetWidth);
        }else {
            init(getIntent());
        }

    }

    private void init(Intent intent) {
        picPaths = intent.getStringArrayExtra(CameraActionHandlerService.PIC_PATH);
        Log.d(TAG, "init: " + picPaths.length);
        if(picPaths.length > 1) tvDetailText.setText(R.string.compress_multiple_images_message);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPaths[0], options); //different thread..maybe?
        outWidth  = options.outWidth;
        outHeight = options.outHeight;
        resolutionView.setResolution(outHeight, outWidth);
        initSeekBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra(CameraActionHandlerService.PIC_PATH));
        init(intent);
        setIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(picPaths != null && picPaths.length > 0){
            outState.putStringArray(CameraActionHandlerService.PIC_PATH, picPaths);
            outState.putInt(OUT_WIDTH,     outWidth);
            outState.putInt(OUT_HEIGHT,    outHeight);
            outState.putInt(TARGET_WIDTH,  resolutionView.getResWidth());
            outState.putInt(TARGET_HEIGHT, resolutionView.getResHeight());
        }
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

    private void initSeekBar(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvQuality.setText("Quality: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void cancel(View view){
        finish();
    }

    public void compress(View view){
        progressBar.setVisibility(View.VISIBLE);
        tvDone.setText("");
        if(picPaths.length == 1){
            new compress(resolutionView.getResWidth(), resolutionView.getResHeight(), seekBar.getProgress()).execute();
        }else if(picPaths.length > 1){
            targetWidth  = resolutionView.getResWidth();
            targetHeight = resolutionView.getResHeight();
            Intent i     = new Intent(CompressPicActivity.this, CompressService.class);
            i.setAction(CompressService.ACTION_COMPRESS_PIC);
            i.putExtra(CompressService.EXTRA_PIC_PATHS, picPaths);
            i.putExtra(CompressService.EXTRA_HEIGHT, targetHeight);
            i.putExtra(CompressService.EXTRA_WIDTH, targetWidth);
            i.putExtra(CompressService.EXTRA_QUALITY, seekBar.getProgress());
            i.putExtra(CompressService.EXTRA_IN_SAMPLE_SIZE, Math.min(outWidth/targetWidth, outHeight/targetHeight));
            startService(i);
        }
    }

    private class compress extends AsyncTask<Void, Void, Void> {
        int targetWidth;
        int targetHeight;
        int quality;

        public compress(int targetWidth, int targetHeight, int quality){
            this.targetHeight = targetHeight;
            this.targetWidth = targetWidth;
            this.quality = quality;
        }
        @Override
        protected Void doInBackground(Void... params) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.min(outWidth/targetWidth, outHeight/targetHeight);
            CompressUtils.compressPic(new File(picPaths[0]), options, quality, targetWidth, targetHeight);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(CompressPicActivity.this, "Compression/Resizing done", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
            int i = intent.getIntExtra("num_pics", -1);
            tvDone.setText(i == -1 ? "Failed" : i + " compressed");
        }
    };
}
