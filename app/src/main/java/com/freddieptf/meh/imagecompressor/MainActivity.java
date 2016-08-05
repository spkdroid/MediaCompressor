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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.freddieptf.meh.imagecompressor.services.CameraActionHandlerService;
import com.freddieptf.meh.imagecompressor.adapters.ImagePreviewAdapter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final int IMAGE_REQUEST_CODE        = 13;
    final int VIDEO_REQUEST_CODE        = 14;
    final int STORAGE_PERMISION_REQUEST = 101;
    private static final String TAG     = "MainActivity";
    Button btnCompress, btnChoosePic, btnChooseVid;
    TextView fileSize, filePath;
    String[] picturePaths;
    RecyclerView recyclerView;
    ImagePreviewAdapter previewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnChoosePic = (Button) findViewById(R.id.btnChooser);
        btnCompress  = (Button) findViewById(R.id.btnCompress);
        btnChooseVid = (Button) findViewById(R.id.btnVidActivity);
        filePath     = (TextView) findViewById(R.id.tv_filePath);
        fileSize     = (TextView) findViewById(R.id.tv_fileSize);
        recyclerView = (RecyclerView) findViewById(R.id.previewRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        previewAdapter = new ImagePreviewAdapter();
        previewAdapter.setClickListener(new ImagePreviewAdapter.ImageClickListener() {
            @Override
            public void onImageClick(String picPath, int ts) {
                btnCompress.setEnabled(ts > 0);
            }
        });
        recyclerView.setAdapter(previewAdapter);

        if(savedInstanceState != null){
            previewAdapter.restoreState(savedInstanceState);
            if(savedInstanceState.containsKey("visible")){
                btnCompress.setVisibility(savedInstanceState.getBoolean("visible") ? View.VISIBLE : View.GONE);
            }

            if(savedInstanceState.containsKey("pic_path")){
                picturePaths = savedInstanceState.getStringArray("pic_path");
                previewAdapter.swapData(picturePaths);
                btnCompress.setEnabled(previewAdapter.getSelected().size() > 0);
            }
        }

        btnChoosePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkStoragePermission()) launchAndroidImagePicker();
                previewAdapter.clearSelectedItems();
            }
        });

        btnChooseVid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAndroidVideoPicker();
            }
        });

        btnCompress.setOnClickListener(new View.OnClickListener() {
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent = Intent.createChooser(intent, "Pick Images from ");
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    public void launchAndroidVideoPicker(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent = Intent.createChooser(intent, "Pick Videos from ");
        startActivityForResult(intent, VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri[] uris;
                if (data.getClipData() == null) {
                    uris = new Uri[]{data.getData()};
                    picturePaths = getPicPathsFromPicUris(MainActivity.this, uris);
                } else {
                    uris = new Uri[data.getClipData().getItemCount()];
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        uris[i] = data.getClipData().getItemAt(i).getUri();
                    }
                    picturePaths = getPicPathsFromPicUris(MainActivity.this, uris);
                }
                previewAdapter.swapData(picturePaths);
                btnCompress.setVisibility(View.VISIBLE);
                btnCompress.setEnabled(previewAdapter.getSelected().size() > 0);
            }
        }else if(requestCode == VIDEO_REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null) {
                Intent i = new Intent(MainActivity.this, VideoActivity.class);
                i.putExtra(CameraActionHandlerService.MEDIA_URI, getNormalisedVideoUriFromUri(data.getData()));
                startActivity(i);
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(picturePaths != null && picturePaths.length > 0) outState.putStringArray("pic_path", picturePaths);
        previewAdapter.saveState(outState);
        outState.putBoolean("visible", btnCompress.getVisibility() == View.VISIBLE);

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

    public static String getNormalisedVideoUriFromUri(Uri uri){
        Uri mediaUri = Uri.parse("content://media/external/video/media");
        String ss;
        if(uri.getAuthority().equals("com.android.providers.media.documents")){
            ss = mediaUri.buildUpon().appendPath(uri.getLastPathSegment().split(":")[1]).build().toString();
        }else ss = uri.toString();
        Log.d(TAG, "Video uri: " + uri.toString());
        return ss;
    }

    public void showCompressDialog(){
        Intent intent = new Intent(this, CompressPicActivity.class);
        intent.putExtra(CameraActionHandlerService.PIC_PATH, filterSelectedPaths(picturePaths));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private String[] filterSelectedPaths(String[] picPaths){
        ArrayList<Integer> s = previewAdapter.getSelected();
        String[] paths = new String[s.size()];
        for (int i = 0; i < s.size(); i++){
            paths[i] = picPaths[s.get(i)];
        }
        return paths;
    }
}
