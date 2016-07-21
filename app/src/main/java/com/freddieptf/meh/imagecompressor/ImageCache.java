package com.freddieptf.meh.imagecompressor;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by freddieptf on 21/07/16.
 */
public class ImageCache {

    private static LruCache<String, Bitmap> lruCache;
    private static ImageCache imageCache;

    private ImageCache(){}

    public static ImageCache getInstance() {
        if(imageCache == null){
            imageCache = new ImageCache();
            int maxMem = (int) Runtime.getRuntime().maxMemory()/1024;
            int cacheSize = maxMem/4;
            lruCache = new LruCache<>(cacheSize);
        }
        return imageCache;
    }

    public void addBitmapToCache(String key, Bitmap bitmap){
        lruCache.put(key, bitmap);
    }

    public Bitmap getBitmapFromCache(String key){
        return lruCache.get(key);
    }
}



