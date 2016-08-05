package com.freddieptf.meh.imagecompressor;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.freddieptf.meh.imagecompressor.databinding.LayoutVidConversionBinding;
import com.freddieptf.meh.imagecompressor.services.CompressService;

import java.io.File;

/**
 * Created by freddieptf on 01/08/16.
 */
public class FragmentVidConversion extends Fragment {

    String[] videoDetails;
    LayoutVidConversionBinding binding;
    private static final String TAG = "FragmentVidConversion";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_vid_conversion, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoDetails = getArguments().getStringArray(VideoActivity.VID_DETS);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(), R.array.formats, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinner.setAdapter(spinnerAdapter);
        binding.btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertVideo();
            }
        });
    }


    public void convertVideo() {
        File file = new File(videoDetails[0]);

        String fileName = file.getParent() +"/output_" + file.getName() +".mkv";

        String[] convertCmdx264 = new String[]{
                "-y", "-i", videoDetails[VideoActivity.KEY_PATH], "-c:v", "libx264", "-preset", "veryfast",
                "-profile:v", "baseline", "-level", "3.0",
                fileName };

        file = new File(fileName);
        if(file.exists()) file.delete();

//        String[] convertCmdvp9 = new String[]{
//                "-y", "-i", videoDetails[0], "-c:v", "libvpx-vp9", "-quality", "good", "-cpu-used", "2",
//                "-crf", "23", "-b:v", "1200k", "-strict", "-2", "-threads", Runtime.getRuntime().availableProcessors() - 1 + "",
//                file.getParent() + "/out_" + file.getName() + ".webm"
//        };

        Intent intent = new Intent(getContext(), CompressService.class);
        intent.setAction(CompressService.ACTION_COMPRESS_VID);
        intent.putExtra(CompressService.EXTRA_VID_CMD, convertCmdx264);
        getActivity().startService(intent);
    }
}
