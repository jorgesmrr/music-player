package br.jm.music.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Loads and caches local images
 * Created by Jorge on 17/08/2014.
 */
public class FileBitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

    private final WeakReference<ImageView> imageViewReference;
    private String path = null;
    private final int height;

    private FileBitmapWorkerTask(ImageView imageView, int height) {
        this.height = height;
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... files) {
        path = files[0];
        return BitmapHelper.readFromDisk(path, height);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            final FileBitmapWorkerTask resourceBitmapWorkerTask =
                    getBitmapWorkerTask(imageView);
            if (this == resourceBitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<FileBitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             FileBitmapWorkerTask fileBitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<>(fileBitmapWorkerTask);
        }

        public FileBitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static void loadBitmap(Resources res, String bitmapPath, ImageView imageView, int height) {
            if (cancelPotentialWork(bitmapPath, imageView)) {
                final FileBitmapWorkerTask task = new FileBitmapWorkerTask(imageView, height);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(res, BitmapHelper.getDefault(res, height), task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(bitmapPath);
            }
    }

    private static boolean cancelPotentialWork(String path, ImageView imageView) {
        final FileBitmapWorkerTask resourceBitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (resourceBitmapWorkerTask != null) {
            final String bitmapPath = resourceBitmapWorkerTask.path;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapPath == null || !bitmapPath.equals(path)) {
                // Cancel previous task
                resourceBitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static FileBitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
}
