package com.freddieptf.meh.imagecompressor;

import android.content.Intent;
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
    TextView tvQuality;
    float factor;
    HeightTextWatcher heightTextWatcher;
    WidthTextWatcher widthTextWatcher;
    String picPath;
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

        if(savedInstanceState != null && savedInstanceState.containsKey(CompressService.PIC_PATH)){
            picPath = savedInstanceState.getString(CompressService.PIC_PATH);
            outWidth = savedInstanceState.getInt(OUT_WIDTH);
            outHeight = savedInstanceState.getInt(OUT_HEIGHT);
        }else {
            picPath = "";
            init(getIntent());
        }

    }

    private void init(Intent intent) {
        picPath = intent.getStringExtra(CompressService.PIC_PATH);
        Log.d(TAG, "init: " + picPath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, options); //different thread..maybe?
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
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra(CompressService.PIC_PATH));
        init(intent);
        setIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(!picPath.isEmpty()){
            outState.putString(CompressService.PIC_PATH, picPath);
            outState.putInt(OUT_WIDTH, outWidth);
            outState.putInt(OUT_HEIGHT, outHeight);
        }

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
        new compress(Integer.parseInt(width.getText().toString()),
                Integer.parseInt(height.getText().toString()), seekBar.getProgress()).execute();
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
            CompressUtils.compressPic(new File(picPath), options, quality, targetWidth, targetHeight);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(CompressPicActivity.this, "Compression/Resizing done", Toast.LENGTH_SHORT).show();
        }
    }
}
