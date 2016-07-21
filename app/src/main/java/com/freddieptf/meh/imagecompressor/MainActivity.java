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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final int IMAGE_REQUEST_CODE        = 13;
    final int STORAGE_PERMISION_REQUEST = 101;
    private static final String TAG     = "MainActivity";
    Button compressBtn, chooseButton;
    TextView fileSize, filePath;
    String[] picturePaths;
    RecyclerView recyclerView;
    ImagePreviewAdapter previewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chooseButton = (Button) findViewById(R.id.btnChooser);
        compressBtn  = (Button) findViewById(R.id.btnCompress);
        filePath     = (TextView) findViewById(R.id.tv_filePath);
        fileSize     = (TextView) findViewById(R.id.tv_fileSize);
        recyclerView = (RecyclerView) findViewById(R.id.previewRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        previewAdapter = new ImagePreviewAdapter();
        previewAdapter.setClickListener(new ImagePreviewAdapter.ImageClickListener() {
            @Override
            public void onImageClick(String picPath, int ts) {
                compressBtn.setEnabled(ts > 0);
            }
        });
        recyclerView.setAdapter(previewAdapter);

        if(savedInstanceState != null){
            previewAdapter.restoreState(savedInstanceState);
            if(savedInstanceState.containsKey("visible")){
                compressBtn.setVisibility(savedInstanceState.getBoolean("visible") ? View.VISIBLE : View.GONE);
            }

            if(savedInstanceState.containsKey("pic_path")){
                picturePaths = savedInstanceState.getStringArray("pic_path");
                previewAdapter.swapData(picturePaths);
                compressBtn.setEnabled(previewAdapter.getSelected().size() > 0);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent = Intent.createChooser(intent, "Pick Images from ");
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    public void launchAndroidVideoPicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
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
                compressBtn.setVisibility(View.VISIBLE);
                compressBtn.setEnabled(previewAdapter.getSelected().size() > 0);
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(picturePaths != null && picturePaths.length > 0) outState.putStringArray("pic_path", picturePaths);
        previewAdapter.saveState(outState);
        outState.putBoolean("visible", compressBtn.getVisibility() == View.VISIBLE);

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
