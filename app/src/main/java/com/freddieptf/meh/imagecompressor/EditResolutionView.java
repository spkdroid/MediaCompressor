package com.freddieptf.meh.imagecompressor;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by freddieptf on 24/07/16.
 */
public class EditResolutionView extends LinearLayout {

    private EditText height, width;
    private TextWatcher widthTextWatcher, heightTextWatcher;
    private float factor;

    public EditResolutionView(Context context) {
        this(context, null);
    }

    public EditResolutionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditResolutionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.edit_resolution_view, this);
        height = (EditText) findViewById(R.id.et_height);
        width  = (EditText) findViewById(R.id.et_width);
    }



    public void setResolution(int h, int w){
        height.setText(h + "");
        width.setText(w + "");
        factor = (float) h / (float) w;
        linkHeightWidth();
    }

    public int getResHeight(){
        return Integer.parseInt(height.getText().toString());
    }

    public int getResWidth(){
        return Integer.parseInt(width.getText().toString());
    }

    private void linkHeightWidth(){
        heightTextWatcher = new HeightTextWatcher();
        height.addTextChangedListener(heightTextWatcher);
        widthTextWatcher  = new WidthTextWatcher();
        width.addTextChangedListener(widthTextWatcher);
    }

    private class HeightTextWatcher implements TextWatcher {

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




}
