package br.jm.music.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Used for image caching
 * Created by Jorge on 08/07/2014.
 */
public class LruBitmapCache extends LruCache<String, Bitmap> {

    public LruBitmapCache() {
        this(getDefaultLruCacheSize());
    }

    private LruBitmapCache(int sizeInKiloBytes) {
        super(sizeInKiloBytes);
    }

    private static int getDefaultLruCacheSize() {
        return ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight() / 1024;
    }

    public Bitmap getBitmap(String key, int height){
        return get(key + "|" + height);
    }

    public void putBitmap(String key, Bitmap bitmap, int height){
        put(key + "|" + height, bitmap);
    }
}
