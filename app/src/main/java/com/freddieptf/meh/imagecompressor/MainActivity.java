package com.freddieptf.meh.imagecompressor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    final int IMAGE_REQUEST_CODE        = 13;
    final int STORAGE_PERMISION_REQUEST = 101;
    String picturePath                  = "";
    private static final String TAG     = "MainActivity";
    ImageView imagePreview;
    Button compressBtn, chooseButton;
    TextView fileSize, filePath;
    File pictureFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chooseButton = (Button) findViewById(R.id.btnChooser);
        compressBtn = (Button) findViewById(R.id.btnCompress);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        filePath = (TextView) findViewById(R.id.tv_filePath);
        fileSize = (TextView) findViewById(R.id.tv_fileSize);

        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("visible")){
                compressBtn.setVisibility(savedInstanceState.getBoolean("visible") ? View.VISIBLE : View.GONE);
            }

            if(savedInstanceState.containsKey("pic_path")){
                picturePath = savedInstanceState.getString("pic_path");

                imagePreview.setImageBitmap(CompressUtils.scaleImageForPreview(picturePath, 100));

                if(pictureFile == null) pictureFile = new File(picturePath);
                filePath.setText(picturePath);
                fileSize.setText(pictureFile.length() + "Kb");
            }
        }

        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkStoragePermission()) launchAndroidImagePicker();
            }
        });

        compressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCompressDialog();
            }
        });
    }

    private boolean checkStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISION_REQUEST);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == STORAGE_PERMISION_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            launchAndroidImagePicker();
        }
    }

    public void launchAndroidImagePicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            if(data != null){
                picturePath = getPicPathFromPicUri(MainActivity.this, data.getData());
                //WHAT IF WE DON'T GET A PICTUREPATH
                if(!picturePath.isEmpty()) {
                    compressBtn.setVisibility(View.VISIBLE);
                    imagePreview.setImageBitmap(CompressUtils.scaleImageForPreview(picturePath, 100));

                    pictureFile = new File(picturePath);
                    filePath.setText(picturePath);
                    fileSize.setText(pictureFile.length() + "Kb");
                }
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(!picturePath.isEmpty()) outState.putString("pic_path", picturePath);
        outState.putBoolean("visible", compressBtn.getVisibility() == View.VISIBLE);
    }

    public static String getPicPathFromPicUri(Context c, Uri picUri) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = c.getContentResolver().query(picUri, filePathColumn, null, null, null);
        if(cursor == null){
            Toast.makeText(c, "error loading pic", Toast.LENGTH_LONG).show();
            return "";
        }
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
    }

    public void showCompressDialog(){
        Intent intent = new Intent(this, CompressPicActivity.class);
        intent.putExtra(CompressService.PIC_PATH, picturePath);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
