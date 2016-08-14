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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

import java.util.concurrent.TimeUnit;

/**
 * Created by freddieptf on 20/07/16.
 */
public class VideoActivity extends AppCompatActivity implements TaskView.OnTaskCheck{

    boolean ffmpegEnabled = false;
    String[] videoDetails;
    String[] presets;
    String threads;
    Bitmap t;
    public static final  String VID_DETS = "vid_dets";
    private static final String TAG      = "CompressVidActivity";

    GetVidDetailsFromUri getVidDetails;

    Spinner spinnerThreads, spinnerPresets, spinnerContainers;
    EditResolutionView resolutionView;
    ImageView ivThumbnail;
    TextView tvVideoDuration, tvVideoResolution, tvVideoSize, tvVideoName, tvVideoQuality;
    SeekBar sbVideoQuality;
    RadioGroup radioGroup;
    FloatingActionButton fab;
    TaskView taskViewScale, taskViewConvert;
    Toolbar toolbar;
    LinearLayout taskParent;

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

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("ff"))
                ffmpegEnabled = savedInstanceState.getBoolean("ff");
            else initFfmpeg();

            if (savedInstanceState.containsKey(VID_DETS)) {
                videoDetails = savedInstanceState.getStringArray(VID_DETS);
                initVideoDetailView(videoDetails);
            }
            else {
                getVidDetails = new GetVidDetailsFromUri(Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
            }

            if(savedInstanceState.containsKey("presets")) presets = savedInstanceState.getStringArray("presets");
            resolutionView.restoreState(savedInstanceState);
        } else {
            initFfmpeg();
            getVidDetails = new GetVidDetailsFromUri(Uri.parse(getIntent().getStringExtra(CameraActionHandlerService.MEDIA_URI)));
        }

        if (getVidDetails != null){
            getVidDetails.execute();
        }

        taskViewScale.setOnTaskCheckedListener(this);
        taskViewConvert.setOnTaskCheckedListener(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    public void onFabClick(View view){
        if(((CheckBox) taskViewScale.findViewById(R.id.taskCheck)).isChecked()){
            CompressUtils.scaleVideo(this,
                    videoDetails[VideoActivity.KEY_PATH],
                    new int[]{resolutionView.getResWidth(), resolutionView.getResHeight()},
                    threads
            );
        }else if (((CheckBox) taskViewConvert.findViewById(R.id.taskCheck)).isChecked()){
            String container = spinnerContainers.getSelectedItem().toString();
            String crf = String.valueOf(sbVideoQuality.getProgress() + 18); //plus 18 cause we're faking the start (zero) as 18
            String encodingPreset = CompressUtils.getEncodingPreset(radioGroup.getCheckedRadioButtonId());
            CompressUtils.convertVideo(this, videoDetails[VideoActivity.KEY_PATH], container, crf, encodingPreset);
        }else {
            Snackbar.make(view, "You need to select atleast one Task", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCheck(boolean b, TaskView taskView) {
        switchTasks(b, taskView);
//        if(b) taskView.findViewById(R.id.taskBody).setVisibility(View.VISIBLE);
//        else taskView.findViewById(R.id.taskBody).setVisibility(View.GONE);
    }

    //placeholder until i enable task linking where one tasks output is another tasks input
    private void switchTasks(boolean b, TaskView taskView){
        if(taskView.getTag().equals("scale")){
            if(b) {
                taskViewScale.findViewById(R.id.taskBody).setVisibility(View.VISIBLE);
                taskViewConvert.findViewById(R.id.taskBody).setVisibility(View.GONE);
                ((CheckBox) taskViewConvert.findViewById(R.id.taskCheck)).setChecked(false);
            } else {
                taskViewScale.findViewById(R.id.taskBody).setVisibility(View.GONE);
            }
        }else {
            if(b) {
                taskViewConvert.findViewById(R.id.taskBody).setVisibility(View.VISIBLE);
                taskViewScale.findViewById(R.id.taskBody).setVisibility(View.GONE);
                ((CheckBox) taskViewScale.findViewById(R.id.taskCheck)).setChecked(false);
            }else {
                taskViewConvert.findViewById(R.id.taskBody).setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ff", ffmpegEnabled);
        if (videoDetails != null && videoDetails.length > 0) outState.putStringArray(VID_DETS, videoDetails);
        if(presets != null) outState.putStringArray("presets", presets);
        if(threads != null && !threads.isEmpty()) outState.putString("threads", threads);
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
    }

    private void animateToolBar(){
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        int toolBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());

        ValueAnimator heightAnimator = ValueAnimator.ofInt(toolBarHeight, getResources().getDimensionPixelSize(R.dimen.expanded_toolbar));
        heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
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

        ArrayAdapter<String> threadSpinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, android.R.id.text1,
                new String[]{Runtime.getRuntime().availableProcessors()-1 + " ", Runtime.getRuntime().availableProcessors() + " " });
        threadSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThreads.setAdapter(threadSpinnerAdapter);
        spinnerThreads.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position > 0){
                    threads = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
            Log.d(TAG, intent.getStringExtra(CompressService.PROGRESS_UPDATE));
        }
    };

}
