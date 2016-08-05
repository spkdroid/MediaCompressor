package com.freddieptf.meh.imagecompressor.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by freddieptf on 21/07/16.
 */
public class ImageCache {

    private static LruCache<String, Bitmap> lruCache;
    private static ImageCache imageCache;

    private ImageCache(){
        int maxMem = (int) Runtime.getRuntime().maxMemory()/1024;
        int cacheSize = maxMem/4;
        lruCache = new LruCache<>(cacheSize);
    }

    public static ImageCache getInstance() {
        if(imageCache == null){
            imageCache = new ImageCache();
        }
        return imageCache;
    }

    public void addBitmapToCache(String key, Bitmap bitmap){
        if(key != null && bitmap != null) lruCache.put(key, bitmap);
    }

    public Bitmap getBitmapFromCache(String key){
        return lruCache.get(key);
    }
}



