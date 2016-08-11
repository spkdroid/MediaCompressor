package com.freddieptf.meh.imagecompressor.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.freddieptf.meh.imagecompressor.services.CompressService;
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
                                   int desWidth, int desHeight) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(picture), null, options);
            bitmap = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false);

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "MediaCompress-uh" + File.separator + "Pictures");
            if (!file.exists()) file.mkdirs();
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

    public static Bitmap scaleImageForPreview(String picPath, int size) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(picPath, size);
        return BitmapFactory.decodeFile(picPath, options);
    }

    public static int calculateInSampleSize(String picPath, int REQUIRED_SIZE) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, o);
        // Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
            scale *= 2;
        }
        return scale;
    }

    public static void scaleVideo(Context context, String path, int[] resolution, String threads) {
        File vidFile = new File(path);
        String res = getEvenRes(resolution[0], resolution[1]);
        String filePath;
        String[] scaleCmd;

        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "MediaCompress-uh" + File.separator + "Videos");
        if (!outputFile.exists()) outputFile.mkdirs();

        filePath = outputFile.getAbsolutePath() + File.separator + vidFile.getName();
        scaleCmd = new String[]{
                "-i", path, //input
                "-filter:v", "scale=" + res, //scale filter
                "-threads", threads.isEmpty() ? (Runtime.getRuntime().availableProcessors() - 1) + "" : threads,
                "-c:a", "copy", //just copy the audio, no re-encode
                filePath //output file
        };

        vidFile = new File(filePath);
        if (vidFile.exists()) vidFile.delete();

        Intent intent = new Intent(context, CompressService.class);
        intent.setAction(CompressService.ACTION_COMPRESS_VID);
        intent.putExtra(CompressService.EXTRA_VID_CMD, scaleCmd);
        context.startService(intent);
    }

    //cause we might get errors when we pass an odd size..dammit...something about libx264 \(>.\(>.<)/.<)/
    //http://superuser.com/a/624564
    private static String getEvenRes(int w, int h) {
        if (w % 2 != 0) w++;
        if (h % 2 != 0) h++;
        return w + ":" + h;
    }


}
