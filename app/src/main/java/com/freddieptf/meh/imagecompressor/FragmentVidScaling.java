package com.freddieptf.meh.imagecompressor;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.freddieptf.meh.imagecompressor.databinding.LayoutVidScalingBinding;
import com.freddieptf.meh.imagecompressor.services.CompressService;

import java.io.File;

/**
 * Created by freddieptf on 01/08/16.
 */
public class FragmentVidScaling extends Fragment {

    String[] videoDetails;
    private static final String TAG = "FragmentVidScaling";
    LayoutVidScalingBinding binding;
    String[] presets = null;
    String threads = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_vid_scaling, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoDetails = getArguments().getStringArray(VideoActivity.VID_DETS);
        if(savedInstanceState == null){
            String[] res = videoDetails[VideoActivity.KEY_RESOLUTION].split("x");
            int w = Integer.parseInt(res[0]);
            int h = Integer.parseInt(res[1]);
            binding.etVideoResolution.setResolution(w, h);
            generatePresets(h);
        }else {
            binding.etVideoResolution.restoreState(savedInstanceState);
            presets = savedInstanceState.getStringArray("presets");
            threads = savedInstanceState.getString("threads");
        }

        if(presets == null){
            presets = new String[]{"presets"};
            binding.spinnerPresets.setEnabled(false);
        }

        ArrayAdapter<String> presetSpinnerAdapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item, android.R.id.text1, presets);
        presetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPresets.setAdapter(presetSpinnerAdapter);
        binding.spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position > 0 && view != null && binding.etVideoResolution != null){
                    if(Integer.parseInt(videoDetails[VideoActivity.KEY_ROTATION]) == 0) {
                        binding.etVideoResolution.setHeight(((TextView) view.findViewById(android.R.id.text1)).getText().toString());
                    }else {
                        binding.etVideoResolution.setWidth(((TextView) view.findViewById(android.R.id.text1)).getText().toString());
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.btnScaleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleVideo();
            }
        });


        ArrayAdapter<String> threadSpinnerAdapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item, android.R.id.text1,
                new String[]{Runtime.getRuntime().availableProcessors()-1 + " ", Runtime.getRuntime().availableProcessors() + " " });
        threadSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerThreads.setAdapter(threadSpinnerAdapter);
        binding.spinnerThreads.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        ((VideoActivity) getActivity()).setProgressListener(new VideoActivity.ProgressListener() {
            @Override
            public void update(String progress) {
                if(binding.tvStatus != null) binding.tvStatus.setText(progress);
            }
        });

    }

    private void generatePresets(int h) {
        int[] sizes = {1080, 720, 640, 480, 360, 144};
        for(int i = 0; i < sizes.length; i++){
            if(sizes[i] < h) {
                presets = new String[(sizes.length - i) + 1];
                presets[0] = "presets";
                for(int y = 1; i < sizes.length; y++, i++){
                    presets[y] = sizes[i] + "";
                }
                break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.etVideoResolution.saveState(outState);
        if(presets != null) outState.putStringArray("presets", presets);
        if(threads != null && !threads.isEmpty()) outState.putString("threads", threads);
    }

    public void scaleVideo(){
        File file = new File(videoDetails[VideoActivity.KEY_PATH]);
        String res = getEvenRes(binding.etVideoResolution.getResWidth(), binding.etVideoResolution.getResHeight());
        String filePath;
        String[] scaleCmd;
        if(binding.checkbox.isChecked()){
        }else{
        }

        filePath = file.getParent() + "/scaled_" + file.getName();
        scaleCmd = new String[]{
                "-i", videoDetails[VideoActivity.KEY_PATH], //input
                "-filter:v", "scale=" + res, //scale filter
                "-threads", threads.isEmpty() ? (Runtime.getRuntime().availableProcessors() - 1) + "" : threads,
                "-c:a", "copy", //just copy the audio, no re-encode
                filePath //output file
        };

        file = new File(filePath);
        if(file.exists()) file.delete();

        Intent intent = new Intent(getActivity(), CompressService.class);
        intent.setAction(CompressService.ACTION_COMPRESS_VID);
        intent.putExtra(CompressService.EXTRA_VID_CMD, scaleCmd);
        getActivity().startService(intent);
    }

    //cause we might get errors when we pass an odd size..dammit...something about libx264 \(>.\(>.<)/.<)/
    //http://superuser.com/a/624564
    public String getEvenRes(int w, int h) {
        if(w % 2 != 0) w++;
        if(h % 2 != 0) h++;
        return w + ":" + h;
    }

}
