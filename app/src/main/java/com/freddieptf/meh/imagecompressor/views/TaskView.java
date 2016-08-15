package com.freddieptf.meh.imagecompressor.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.freddieptf.meh.imagecompressor.R;

/**
 * Created by freddieptf on 08/08/16.
 */
public class TaskView extends LinearLayout implements View.OnClickListener {

    LinearLayout taskView, taskBody;
    TextView taskTitle;
    RadioButton taskRadioButton;
    private static final String TAG = "TaskView";
    OnTaskCheck onTaskCheck;
    boolean checked = false;

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
        taskRadioButton = (RadioButton) findViewById(R.id.taskCheck);

        TypedArray typedArray = context.getResources().obtainAttributes(attrs, R.styleable.TaskView);
        CharSequence title = typedArray.getString(R.styleable.TaskView_taskTitle);
        typedArray.recycle();
        if(title != null && !TextUtils.isEmpty(title)) taskTitle.setText(title);

        taskRadioButton.setOnClickListener(this);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(taskBody == null) super.addView(child, index, params);
        else taskBody.addView(child, index, params);
    }

    public boolean isChecked(){
        return taskRadioButton.isChecked();
    }

    public void toggle(){
        checked = !checked;
        expand(checked);
        taskRadioButton.setChecked(checked);
    }

    public void setChecked(boolean checked) {
        Log.d(TAG, getTag() + " SetChecked: " + checked);
        this.checked = checked;
        taskRadioButton.setChecked(checked);
        expand(checked);
    }

    private void expand(boolean checked){
        taskBody.setVisibility(checked ? VISIBLE : GONE);
    }

    @Override
    public void onClick(View v) {
        if(onTaskCheck != null) onTaskCheck.onTaskCheck(String.valueOf(getTag()));
    }

    public void setOnTaskCheckedListener(OnTaskCheck onTaskCheck){
       this.onTaskCheck = onTaskCheck;
    }

    public interface OnTaskCheck{
        void onTaskCheck(String tag);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable p = super.onSaveInstanceState();
        SaveState saveState = new SaveState(p);
        saveState.childrenStates = new SparseArray<Parcelable>();
        for(int i = 0; i < getChildCount(); i++){
            getChildAt(i).saveHierarchyState(saveState.childrenStates);
        }
        return saveState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SaveState saveState = (SaveState) state;
        super.onRestoreInstanceState(state);
        for(int i = 0; i < getChildCount(); i++){
            getChildAt(i).restoreHierarchyState(saveState.childrenStates);
        }
        if(taskRadioButton.isChecked()) expand(true);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    static class SaveState extends BaseSavedState {
        SparseArray childrenStates;

        SaveState(Parcelable source) {
            super(source);
        }

        private SaveState(Parcel source, ClassLoader loader) {
            super(source);
            childrenStates = source.readSparseArray(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSparseArray(childrenStates);
        }

        public static final Creator<SaveState> CREATOR =
                ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SaveState>() {

            @Override
            public SaveState createFromParcel(Parcel source, ClassLoader loader) {
                return new SaveState(source, loader);
            }
            @Override
            public SaveState[] newArray(int size) {
                return new SaveState[size];
            }
        });
    }

}
