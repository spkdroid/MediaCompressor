package com.freddieptf.meh.imagecompressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
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

import com.freddieptf.meh.imagecompressor.services.CameraActionHandlerService;
import com.freddieptf.meh.imagecompressor.services.CompressService;
import com.freddieptf.meh.imagecompressor.views.EditResolutionView;

/**
 * Created by freddieptf on 18/07/16.
 */
public class CompressPicActivity extends AppCompatActivity{

    SeekBar seekBar;
    TextView tvQuality, tvDetailText;
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
        progressBar    = (ProgressBar) findViewById(R.id.progress);

        if(savedInstanceState != null && savedInstanceState.containsKey(CameraActionHandlerService.PIC_PATH)){
            picPaths     = savedInstanceState.getStringArray(CameraActionHandlerService.PIC_PATH);
            outWidth     = savedInstanceState.getInt(OUT_WIDTH);
            outHeight    = savedInstanceState.getInt(OUT_HEIGHT);
            targetWidth  = savedInstanceState.getInt(TARGET_WIDTH);
            targetHeight = savedInstanceState.getInt(TARGET_HEIGHT);
            resolutionView.setResolution(targetWidth, targetHeight);
            tvDetailText.setVisibility(savedInstanceState.getBoolean("dtv") ? View.VISIBLE : View.GONE);
        }else {
            init(getIntent());
        }

    }

    private void init(Intent intent) {
        picPaths = intent.getStringArrayExtra(CameraActionHandlerService.PIC_PATH);
        Log.d(TAG, "init: " + picPaths.length);
        if(picPaths.length > 1){
            tvDetailText.setVisibility(View.VISIBLE);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPaths[0], options);
        outWidth  = options.outWidth;
        outHeight = options.outHeight;
        resolutionView.setResolution(outWidth, outHeight);
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
        outState.putBoolean("dtv", tvDetailText.getVisibility() == View.VISIBLE);
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

        targetWidth = resolutionView.getResWidth();
        targetHeight = resolutionView.getResHeight();
        Intent i = new Intent(CompressPicActivity.this, CompressService.class);
        i.setAction(CompressService.ACTION_COMPRESS_PIC);
        i.putExtra(CompressService.EXTRA_PIC_PATHS, picPaths);
        i.putExtra(CompressService.EXTRA_HEIGHT, targetHeight);
        i.putExtra(CompressService.EXTRA_WIDTH, targetWidth);
        i.putExtra(CompressService.EXTRA_QUALITY, seekBar.getProgress());
        i.putExtra(CompressService.EXTRA_IN_SAMPLE_SIZE, Math.min(outWidth / targetWidth, outHeight / targetHeight));
        startService(i);

    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
            int i = intent.getIntExtra("num_pics", -1);
            Toast.makeText(context, "Success: " + i + " pics compressed!", Toast.LENGTH_LONG).show();
        }
    };
}
