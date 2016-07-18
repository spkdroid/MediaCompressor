package com.freddieptf.meh.imagecompressor;

import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

/**
 * Created by freddieptf on 18/07/16.
 */
public class CompressPicActivity extends AppCompatActivity {

    EditText height, width;
    SeekBar seekBar;
    TextView tvQuality;
    Bitmap bitmap;
    float factor;
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
        bitmap = BitmapFactory.decodeFile(picPath);

        if(bitmap != null){
            height.setText(bitmap.getHeight() + "");
            width.setText(bitmap.getWidth() + "");

            factor = (float) bitmap.getHeight()/(float) bitmap.getWidth();
            linkHeightWidth();
            initSeekBar();
        }

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
                tvQuality.setText("Quality:" + progress + "%");
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = Integer.parseInt(height.getText().toString());
        options.outWidth = Integer.parseInt(width.getText().toString());
        CompressUtils.compressPic(new File(picPath), options, seekBar.getProgress());
    }
}
