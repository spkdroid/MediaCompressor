package com.freddieptf.meh.imagecompressor.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freddieptf.meh.imagecompressor.R;

/**
 * Created by freddieptf on 08/08/16.
 */
public class TaskView extends LinearLayout {

    LinearLayout taskView, taskBody;
    TextView taskTitle;
    CheckBox checkBox;
    private static final String TAG = "TaskView";

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.task_layout, this, true);
        taskView = (LinearLayout) findViewById(R.id.taskView);
        taskBody = (LinearLayout) findViewById(R.id.taskBody);
        taskTitle = (TextView) findViewById(R.id.taskTitle);
        checkBox = (CheckBox) findViewById(R.id.taskCheck);

        TypedArray typedArray = context.getResources().obtainAttributes(attrs, R.styleable.TaskView);
        CharSequence title = typedArray.getString(R.styleable.TaskView_taskTitle);
        typedArray.recycle();
        if(title != null && !TextUtils.isEmpty(title)) taskTitle.setText(title);

    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(taskBody == null) super.addView(child, index, params);
        else taskBody.addView(child, index, params);
    }

    public void setOnTaskCheckedListener(final OnTaskCheck onTaskCheck){
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onTaskCheck.onTaskCheck(b, TaskView.this);
            }
        });
    }

    public interface OnTaskCheck{
        void onTaskCheck(boolean b, TaskView taskView);
    }

}
