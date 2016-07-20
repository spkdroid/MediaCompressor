package com.freddieptf.meh.imagecompressor;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by freddieptf on 20/07/16.
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewVH>
        implements View.OnClickListener{

    private static final String TAG = "ImagePreviewAdapter";
    ImageClickListener onImageClick;
    String[] picPaths;
    ArrayList<Integer> selected;

    public ImagePreviewAdapter(){
        selected = new ArrayList<>();
    }

    public void swapData(String[] picPaths){
        this.picPaths = picPaths;
        notifyDataSetChanged();
    }

    public void setClickListener(ImageClickListener onImageCLick){
        this.onImageClick = onImageCLick;
    }

    public ArrayList<Integer> getSelected() {
        return selected;
    }

    @Override
    public ImagePreviewVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImagePreviewVH(
                LayoutInflater.from(parent.getContext())
                              .inflate(R.layout.list_item_image_preview, parent, false));
    }

    @Override
    public void onBindViewHolder(ImagePreviewVH holder, int position) {
        Log.d(TAG, "BIND: " + picPaths[position]);
        if(selected.contains(position)) holder.itemView.setSelected(true);
        holder.previewImage.setImageBitmap(CompressUtils.scaleImageForPreview(picPaths[position], 100));
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return picPaths == null ? 0 : picPaths.length;
    }

    @Override
    public void onClick(View v) {
        if(onImageClick != null){
            if(!v.isSelected()){
                v.setSelected(true);
                selected.add((Integer)v.getTag());
            }else {
                v.setSelected(false);
                selected.remove((Integer)v.getTag());
            }
            onImageClick.onImageClick(picPaths[(int) v.getTag()]);
            Log.d(TAG, "selected: " + selected.size());
        }
    }

    public void saveState(Bundle state){
        state.putIntegerArrayList("skey", selected);
    }

    public void restoreState(Bundle state){
        if(state.containsKey("skey")) {
            selected = state.getIntegerArrayList("skey");
            if (selected != null) {
                for (Integer i : selected) {
                    notifyItemChanged(i);
                }
            }
        }
    }

    class ImagePreviewVH extends RecyclerView.ViewHolder {
        ImageView previewImage;
        ImagePreviewVH(View itemView) {
            super(itemView);
            previewImage = (ImageView) itemView.findViewById(R.id.imagePreview);
        }
    }

    public interface ImageClickListener {
        void onImageClick(String picPath);
    }

}
