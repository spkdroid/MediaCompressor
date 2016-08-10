package com.freddieptf.meh.imagecompressor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by freddieptf on 18/07/16.
 */
public class CompressUtils {
    private static final String TAG = "CompressUtils";

    public static void compressPic(File picture, BitmapFactory.Options options, int quality,
                                   int desWidth, int desHeight){
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(picture), null, options);
            bitmap = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false);

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "MediaCompress-uh" + File.separator + "Pictures");
            if(!file.exists()) file.mkdirs();
            FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath()
                    + File.separator + picture.getName());
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.close();
            System.gc();
            bitmap.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compressVid(Context context, String[] commands,
                                   FFmpegExecuteResponseHandler fFmpegExecuteResponseHandler)
            throws FFmpegCommandAlreadyRunningException {

            FFmpeg.getInstance(context).execute(commands, fFmpegExecuteResponseHandler);

    }

    public static Bitmap scaleImageForPreview(String picPath, int size){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(picPath, size);
        return BitmapFactory.decodeFile(picPath, options);
    }

    public static int calculateInSampleSize(String picPath, int REQUIRED_SIZE){
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, o);
        // Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while(o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
            scale *= 2;
        }
        return scale;
    }

}
