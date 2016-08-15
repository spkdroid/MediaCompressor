package com.freddieptf.meh.imagecompressor;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.freddieptf.meh.imagecompressor.services.CameraActionHandlerService;
import com.freddieptf.meh.imagecompressor.services.CompressService;
import com.freddieptf.meh.imagecompressor.utils.CompressUtils;
import com.freddieptf.meh.imagecompressor.utils.ImageCache;
import com.freddieptf.meh.imagecompressor.utils.MediaUtils;
import com.freddieptf.meh.imagecompressor.views.EditResolutionView;
import com.freddieptf.meh.imagecompressor.views.TaskView;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by freddieptf on 20/07/16.
 */
public class VideoActivity extends AppCompatActivity implements TaskView.OnTaskCheck{

    boolean ffmpegEnabled = false;
    String[] videoDetails;
    String[] presets;
    String[] threads;
    Bitmap t;
    public static final  String VID_DETS = "vid_dets";
    private static final String TAG      = "CompressVidActivity";

    GetVidDetailsFromUri getVidDetails;
    TasksStatusAdapter tasksStatusAdapter;
    ArrayList<String> tasksStatusStringArray;

    Spinner spinnerThreads, spinnerPresets, spinnerContainers;
    EditResolutionView resolutionView;
    ImageView ivThumbnail;
    TextView tvVideoDuration, tvVideoResolution, tvVideoSize, tvVideoName, tvVideoQuality, tvTaskStatus;
    SeekBar sbVideoQuality;
    RadioGroup radioGroup;
    FloatingActionButton fab;
    TaskView taskViewScale, taskViewConvert;
    Toolbar toolbar;
    LinearLayout taskParent;
    CoordinatorLayout coordinatorLayout;
    View bottomSheet;
    BottomSheetBehavior bottomSheetBehavior;
    RecyclerView taskStatusRecycler;

    public static final int KEY_PATH       = 0;
    public static final int KEY_TITLE      = 1;
    public static final int KEY_DURATION   = 2;
    public static final int KEY_ROTATION   = 3;
    public static final int KEY_RESOLUTION = 4;
    public static final int KEY_MIMETYPE   = 5;
    public static final int KEY_SIZE       = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        toolbar           = (Toolbar) findViewById(R.id.toolbar);
        taskParent        = (LinearLayout) findViewById(R.id.parent);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.content);

        bottomSheet       = coordinatorLayout.findViewById(R.id.bottomSheet);
        taskStatusRecycler= (RecyclerView) findViewById(R.id.recyclerTaskStatus);
        tvTaskStatus      = (TextView) findViewById(R.id.tv_taskStatus);

        tvVideoName       = (TextView)  findViewById(R.id.tv_videoName);
        tvVideoDuration   = (TextView)  findViewById(R.id.tv_videoDuration);
        tvVideoResolution = (TextView)  findViewById(R.id.tv_videoResolution);
        tvVideoSize       = (TextView)  findViewById(R.id.tv_videoSize);
        ivThumbnail       = (ImageView) findViewById(R.id.iv_thumbnail);
        fab               = (FloatingActionButton) findViewById(R.id.fab);

        taskViewScale     = (TaskView) findViewById(R.id.taskViewScale);
        spinnerThreads    = (Spinner)  findViewById(R.id.spinnerThreads);
        spinnerPresets    = (Spinner)  findViewById(R.id.spinnerPresets);
        resolutionView    = (EditResolutionView) findViewById(R.id.et_videoResolution);

        taskViewConvert   = (TaskView) findViewById(R.id.taskViewConvert);
        spinnerContainers = (Spinner)  findViewById(R.id.spinner);
        tvVideoQuality    = (TextView) findViewById(R.id.tv_quality);
        sbVideoQuality    = (SeekBar)  findViewById(R.id.sk_vidQuality);
        radioGroup        = (RadioGroup) findViewById(R.id.rgButtons);

        restoreState(savedInstanceState);
        if (getVidDetails != null) getVidDetails.execute();

        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = getResources().getDisplayMetrics().heightPixels
                - getResources().getDimensionPixelSize(R.dimen.expanded_toolbar)
                - getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        bottomSheet.setLayoutParams(params);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        taskViewScale.setOnTaskCheckedListener(this);
        taskViewConvert.setOnTaskCheckedListener(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("ff"))
                ffmpegEnabled = savedInstanceState.getBoolean("ff");
            else initFfmpeg();

            if(savedInstanceState.containsKey("presets")) presets = savedInstanceState.getStringArray("presets");
            if(savedInstanceState.containsKey("threads")) threads = savedInstanceState.getStringArray("threads");

            if (savedInstanceState.containsKey(VID_DETS)) {
                videoDetails = savedInstanceState.getStringArray(VID_DETS);
                initVideoDetailView(videoDetails);
            }
            else {
                getVidDetails = new GetVidDetailsFromUri(Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
            }

            if(tasksStatusAdapter != null) tasksStatusAdapter.restoreState(savedInstanceState);
            resolutionView.restoreState(savedInstanceState);
        } else {
            initFfmpeg();
            getVidDetails = new GetVidDetailsFromUri(Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
        }
    }

    public void onFabClick(View view){
        if(taskViewScale.isChecked()){
            CompressUtils.scaleVideo(this,
                    videoDetails[VideoActivity.KEY_PATH],
                    new int[]{resolutionView.getResWidth(), resolutionView.getResHeight()},
                    spinnerThreads.getSelectedItem().toString()
            );
            hideFab();
        }else if (taskViewConvert.isChecked()){
            String container = spinnerContainers.getSelectedItem().toString();
            String crf = String.valueOf(sbVideoQuality.getProgress() + 18); //plus 18 cause we're faking the start (zero) as 18
            String encodingPreset = CompressUtils.getEncodingPreset(radioGroup.getCheckedRadioButtonId());
            CompressUtils.convertVideo(this, videoDetails[VideoActivity.KEY_PATH], container, crf, encodingPreset);
            hideFab();
        }else {
            Snackbar.make(view, "You need to select atleast one Task", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCheck(String tag) {
//        Log.d(TAG, "onTaskCheck: " + taskView.getTag() + "/" + b);
//        if(b) taskView.findViewById(R.id.taskBody).setVisibility(View.VISIBLE);
//        else taskView.findViewById(R.id.taskBody).setVisibility(View.GONE);
        switchTasks(tag);
    }

    public void switchTasks(String tag){
        Log.d(TAG, taskViewScale.isChecked() + "/" + taskViewConvert.isChecked());
        if(tag.equals("scale")) {
            taskViewScale.toggle();
            if(taskViewConvert.isChecked()) taskViewConvert.setChecked(false);
        }else if(tag.equals("convert")){
            taskViewConvert.toggle();
            if(taskViewScale.isChecked()) taskViewScale.setChecked(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ff", ffmpegEnabled);
        if(videoDetails != null && videoDetails.length > 0) outState.putStringArray(VID_DETS, videoDetails);
        if(presets != null) outState.putStringArray("presets", presets);
        if(threads != null) outState.putStringArray("threads", threads);
        if(tasksStatusAdapter != null) tasksStatusAdapter.saveState(outState);
        resolutionView.saveState(outState);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getVidDetails != null && getVidDetails.getStatus() == AsyncTask.Status.RUNNING) {
            getVidDetails.cancel(true);
        }
    }

    private void initFfmpeg() {
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
//                    runCommand();
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

    private class GetVidDetailsFromUri extends AsyncTask<Void, Void, String[]> {
        Uri vidUri;

        public GetVidDetailsFromUri(Uri vidUri) {
            this.vidUri = vidUri;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            taskParent.setAlpha(0f);
            for(int i = 0; i < taskParent.getChildCount(); i++){
                View v = taskParent.getChildAt(i);
                v.setTranslationY(i == 0 ? 70 : 100*i);
            }
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            return MediaUtils.getVideoDetailsFromUri(VideoActivity.this, vidUri);
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            videoDetails = strings;
            initVideoDetailView(videoDetails);
            initTaskScale();
            initTaskConvert();
        }
    }

    private void initVideoDetailView(final String[] videoDetails) {
        Bitmap bitmap = ImageCache.getInstance().getBitmapFromCache(videoDetails[KEY_PATH]);
        ivThumbnail.setAlpha(0f);
        if (bitmap == null) {
            Thread loadThumbnail = new Thread(new Runnable() {
                @Override
                public void run() {
                    t = ThumbnailUtils.createVideoThumbnail(videoDetails[KEY_PATH],
                            MediaStore.Video.Thumbnails.MINI_KIND);
                    t = Bitmap.createBitmap(t, 0, 0, t.getWidth(), t.getHeight()/2);
                    t = Bitmap.createScaledBitmap(t, t.getWidth()*2, t.getHeight()*2, true);
                    ImageCache.getInstance().addBitmapToCache(videoDetails[KEY_PATH], t);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivThumbnail.setImageBitmap(t);
                            ivThumbnail.animate().alpha(1f).setDuration(600).start();
                        }
                    });
                }
            });
            loadThumbnail.start();
        } else {
            ivThumbnail.setImageBitmap(bitmap);
            ivThumbnail.animate().alpha(1f).setDuration(600).start();
        }

        animateToolBar();
        tvVideoName.setText(videoDetails[KEY_TITLE]);
        tvVideoDuration.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(videoDetails[KEY_DURATION]))) + "seconds");
        tvVideoResolution.setText(videoDetails[KEY_RESOLUTION]);
        tvVideoSize.setText(Long.parseLong(videoDetails[KEY_SIZE]) / 1024000 + "mb");
        initTaskScale();
        initTaskConvert();
    }

    private void animateToolBar(){
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        int toolBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());

        ValueAnimator heightAnimator = ValueAnimator.ofInt(toolBarHeight, getResources().getDimensionPixelSize(R.dimen.expanded_toolbar));
        heightAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();
                ViewGroup.LayoutParams thumbnailViewParams = ivThumbnail.getLayoutParams();

                toolbarParams.height = (int) valueAnimator.getAnimatedValue();
                thumbnailViewParams.height = (int) valueAnimator.getAnimatedValue();

                toolbar.setLayoutParams(toolbarParams);
                ivThumbnail.setLayoutParams(thumbnailViewParams);
            }
        });

        heightAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                taskParent.animate().alpha(1f).start();
                for(int i = 0; i < taskParent.getChildCount(); i++){
                    View v = taskParent.getChildAt(i);
                    v.animate().translationY(0)
                            .setStartDelay(100 * i)
                            .setInterpolator(new DecelerateInterpolator(1.5f)).start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        heightAnimator.start();
    }

    private void hideFab(){
       fab.setVisibility(View.GONE);
    }

    private void initTaskScale(){
        String[] res = videoDetails[VideoActivity.KEY_RESOLUTION].split("x");
        int w = Integer.parseInt(res[0]);
        int h = Integer.parseInt(res[1]);
        if(presets == null) presets = MediaUtils.generateResolutionPresets(h);
        if(presets == null){
            presets = new String[]{"presets"};
            spinnerPresets.setEnabled(false);
        }
        resolutionView.setResolution(w, h);

        if(threads == null) threads = new String[]{Runtime.getRuntime().availableProcessors()-1 + " ", Runtime.getRuntime().availableProcessors() + " " };
        ArrayAdapter<String> threadSpinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, android.R.id.text1,
                threads);
        threadSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThreads.setAdapter(threadSpinnerAdapter);

        ArrayAdapter<String> presetSpinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, android.R.id.text1, presets);
        presetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(presetSpinnerAdapter);
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position > 0 && view != null && resolutionView != null){
                    if(Integer.parseInt(videoDetails[VideoActivity.KEY_ROTATION]) == 0) {
                        resolutionView.setHeight(((TextView) view.findViewById(android.R.id.text1)).getText().toString());
                    }else {
                        resolutionView.setWidth(((TextView) view.findViewById(android.R.id.text1)).getText().toString());
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initTaskConvert(){
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.formats, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContainers.setAdapter(spinnerAdapter);

        tvVideoQuality.setText(String.valueOf(sbVideoQuality.getProgress() + 18));
        sbVideoQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvVideoQuality.setText(String.valueOf(i + 18));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getBooleanExtra(CompressService.TASK_SUCCESS, false)) {
                tvTaskStatus.setText("Tasks Completed!");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }

            if(tasksStatusStringArray == null) {
                tvTaskStatus.setText("Processing Video...");
                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                tasksStatusStringArray = new ArrayList<>(24);
                tasksStatusStringArray.add(intent.getStringExtra(CompressService.PROGRESS_UPDATE));
            }else{
                if(tasksStatusStringArray.size() == 24) tasksStatusStringArray.remove(0);
                tasksStatusStringArray.add(intent.getStringExtra(CompressService.PROGRESS_UPDATE));
            }

            if(tasksStatusAdapter == null){
                tasksStatusAdapter = new TasksStatusAdapter();
                tasksStatusAdapter.swapData(tasksStatusStringArray);
                taskStatusRecycler.setAdapter(tasksStatusAdapter);
            }else tasksStatusAdapter.swapData(tasksStatusStringArray);
            taskStatusRecycler.scrollToPosition(tasksStatusStringArray.size() - 1);
        }
    };

    private class TasksStatusAdapter extends RecyclerView.Adapter<TaskStatusViewHolder>  {

        ArrayList<String> stringList;
        public void swapData(ArrayList<String> stringList){
            this.stringList = stringList;
            notifyDataSetChanged();
        }

        public void saveState(Bundle outState){
            outState.putStringArrayList("sk", stringList);
        }

        public void restoreState(Bundle savedInstanceState){
            if(savedInstanceState.containsKey("sk")) stringList = savedInstanceState.getStringArrayList("sk");
            notifyDataSetChanged();
        }

        @Override
        public TaskStatusViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TaskStatusViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(TaskStatusViewHolder holder, int position) {
            holder.status.setText(stringList.get(position));
        }

        @Override
        public int getItemCount() {
            return stringList == null ? 0 : stringList.size();
        }
    };

    private class TaskStatusViewHolder extends RecyclerView.ViewHolder {
        TextView status;
        public TaskStatusViewHolder(View itemView){
            super(itemView);
            status = (TextView) itemView.findViewById(android.R.id.text1);
            status.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.gray));
        }
    }

}
