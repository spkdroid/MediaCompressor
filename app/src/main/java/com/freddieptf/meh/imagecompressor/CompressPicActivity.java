package com.freddieptf.meh.imagecompressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by freddieptf on 18/07/16.
 */
public class CompressPicActivity extends AppCompatActivity {

    EditText height, width;
    SeekBar seekBar;
    TextView tvQuality, tvDetailText, tvDone;
    ProgressBar progressBar;
    float factor;
    HeightTextWatcher heightTextWatcher;
    WidthTextWatcher widthTextWatcher;
    String[] picPaths;
    private int outWidth            = -1;
    private int outHeight           = -1;
    private final String OUT_WIDTH  = "ot";
    private final String OUT_HEIGHT = "oh";
    private static final String TAG = "DialogActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity);

        height = (EditText) findViewById(R.id.et_height);
        width = (EditText) findViewById(R.id.et_width);
        seekBar = (SeekBar) findViewById(R.id.seekbar_quality);
        tvQuality = (TextView) findViewById(R.id.tv_quality);
        tvDetailText = (TextView)findViewById(R.id.tv_detailText);
        tvDone = (TextView) findViewById(R.id.tvDone);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        if(savedInstanceState != null && savedInstanceState.containsKey(CameraActionHandlerService.PIC_PATH)){
            picPaths = savedInstanceState.getStringArray(CameraActionHandlerService.PIC_PATH);
            outWidth = savedInstanceState.getInt(OUT_WIDTH);
            outHeight = savedInstanceState.getInt(OUT_HEIGHT);
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
        outWidth = options.outWidth;
        outHeight = options.outHeight;
        height.setText(outHeight + "");
        width.setText(outWidth + "");
        factor = (float) options.outHeight / (float) options.outWidth;
        linkHeightWidth();
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
            outState.putInt(OUT_WIDTH, outWidth);
            outState.putInt(OUT_HEIGHT, outHeight);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(CompressImgsService.COMPRESSION_DONE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private void linkHeightWidth(){
        heightTextWatcher = new HeightTextWatcher();
//        height.addTextChangedListener(heightTextWatcher);
        widthTextWatcher = new WidthTextWatcher();
        width.addTextChangedListener(widthTextWatcher);
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

    private class HeightTextWatcher implements TextWatcher{

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            //remove it...or we'll get stuck in a loop between the editTexts afterTextChanged callback, FOREVERRRRRR
            width.removeTextChangedListener(widthTextWatcher);

            String num = s.toString();
            if(!num.isEmpty()) {
                float d = Float.parseFloat(num);
                width.setText((int) (d / factor) + "");
            }else width.setText("");

            width.addTextChangedListener(widthTextWatcher);
        }
    }

    private class WidthTextWatcher implements TextWatcher{

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            height.removeTextChangedListener(heightTextWatcher);

            String num = s.toString();
            if(!num.isEmpty()) {
                float d = Float.parseFloat(num);
                height.setText((int)(d * factor) + "");
            }else height.setText("");

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    height.addTextChangedListener(heightTextWatcher);
                }
            }, 1);
        }
    }

    public void cancel(View view){
        finish();
    }

    public void compress(View view){
        progressBar.setVisibility(View.VISIBLE);
        tvDone.setText("");
        if(picPaths.length == 1){
            new compress(Integer.parseInt(width.getText().toString()),
                    Integer.parseInt(height.getText().toString()), seekBar.getProgress()).execute();
        }else if(picPaths.length > 1){
            int targetWidth = Integer.parseInt(width.getText().toString());
            int targetHeight = Integer.parseInt(height.getText().toString());
            Intent i = new Intent(CompressPicActivity.this, CompressImgsService.class);
            i.putExtra(CompressImgsService.EXTRA_PIC_PATHS, picPaths);
            i.putExtra(CompressImgsService.EXTRA_HEIGHT, targetHeight);
            i.putExtra(CompressImgsService.EXTRA_WIDTH, targetWidth);
            i.putExtra(CompressImgsService.EXTRA_QUALITY, seekBar.getProgress());
            i.putExtra(CompressImgsService.EXTRA_IN_SAMPLE_SIZE, Math.min(outWidth/targetWidth, outHeight/targetHeight));
            startService(i);
        }
    }

    private class compress extends AsyncTask<Void, Void, Void> {
        int targetWidth;
        int targetHeight;
        int quality;

        public compress(int targetHidth, int targetHeight, int quality){
            this.targetHeight = targetHeight;
            this.targetWidth = targetHidth;
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
