package com.freddieptf.meh.imagecompressor.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * Created by freddieptf on 12/08/16.
 */
public class MediaUtils {

    private MediaUtils(){}

    public static String getNormalisedVideoUri(Uri uri){
        Uri mediaUri = Uri.parse("content://media/external/video/media");
        String ss;
        if(uri.getAuthority().equals("com.android.providers.media.documents")){
            ss = mediaUri.buildUpon().appendPath(uri.getLastPathSegment().split(":")[1]).build().toString();
        }else ss = uri.toString();
        return ss;
    }

    public static String[] getVideoDetailsFromUri(Context c, Uri vidUri) {
        String[] columns = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        Cursor cursor = c.getContentResolver().query(vidUri, columns, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(c, "error loading video from path", Toast.LENGTH_LONG).show();
            return null;
        }
        cursor.moveToFirst();
        String[] dets = new String[7];
        dets[0] = cursor.getString(cursor.getColumnIndex(columns[0])); //path 0

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(dets[0]);

        dets[1] = cursor.getString(cursor.getColumnIndex(columns[2]));
        dets[2] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        dets[3] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (Integer.parseInt(dets[3]) == 0) { //when the phone is in landscape mode.."normal video"
            dets[4] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    + "x" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        } else if (Integer.parseInt(dets[3]) >= 90) { //when the phone is in portrait mode.."vertical video"..i think..
            dets[4] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    + "x" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        }
        dets[5] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        dets[6] = cursor.getString(cursor.getColumnIndex(columns[1]));

        if (!cursor.isClosed()) cursor.close();
        retriever.release();

        return dets;
    }

    public static String[] getPicPathsFromPicUris(Context c, Uri[] picUris) {
        Uri mediaUri = Uri.parse("content://media/external/images/media");
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        String[] picPaths = new String[picUris.length];
        int i = 0;
        for(Uri picUri : picUris) {
            if(picUri.getAuthority().equals("com.android.providers.media.documents")){
                picUri = mediaUri.buildUpon().appendPath(picUri.getLastPathSegment().split(":")[1]).build();
            }
            Cursor cursor = c.getContentResolver().query(picUri, filePathColumn, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(c, "error loading pic", Toast.LENGTH_LONG).show();
                break;
            }
            cursor.moveToFirst();
            picPaths[i] = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            if(!cursor.isClosed()) cursor.close();
            i++;
        }
        return picPaths;
    }


    public static String[] generateResolutionPresets(int h) {
        int[] sizes = {1080, 720, 640, 576, 480, 360, 144};
        String[] presets = null;
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] < h) {
                presets = new String[(sizes.length - i) + 1];
                presets[0] = "presets";
                for (int y = 1; i < sizes.length; y++, i++) {
                    presets[y] = sizes[i] + "";
                }
                break;
            }
        }
        return presets;
    }

}
