package com.freddieptf.meh.imagecompressor;

import android.app.NotificationManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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
    BitmapFactory.Options options;
    HeightTextWatcher heightTextWatcher;
    WidthTextWatcher widthTextWatcher;
    String picPath;
    private static final String TAG = "DialogActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity);

        height = (EditText) findViewById(R.id.et_height);
        width = (EditText) findViewById(R.id.et_width);
        seekBar = (SeekBar) findViewById(R.id.seekbar_quality);
        tvQuality = (TextView) findViewById(R.id.tv_quality);

        picPath = getIntent().getStringExtra(CompressService.PIC_PATH);

        options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, options);

        height.setText(options.outHeight + "");
        width.setText(options.outWidth + "");

        factor = (float) options.outHeight / (float) options.outWidth;
        linkHeightWidth();
        initSeekBar();

    }

    private void linkHeightWidth(){
        heightTextWatcher = new HeightTextWatcher();
        height.addTextChangedListener(heightTextWatcher);
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

            height.addTextChangedListener(heightTextWatcher);
        }
    }

    public void cancel(View view){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(CompressService.NOTIFICATION_ID);
        finish();
    }

    public void compress(View view){
        Log.d(TAG, "Compress");
        new compress(Integer.parseInt(width.getText().toString()),
                Integer.parseInt(height.getText().toString()), seekBar.getProgress()).execute();
    }

    private class compress extends AsyncTask<Void, Void, Void>
    {
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
            options.inSampleSize = Math.min(options.outWidth/targetWidth, options.outHeight/targetHeight);
            options.inJustDecodeBounds = false;
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
