package com.example.android.uamp.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;

import com.example.android.uamp.R;

import java.lang.ref.WeakReference;

/**
 * Created by Jorge on 16/06/2015.
 */
public class FileBitmapWorkerTask extends AsyncTask<String, Void, FileBitmapWorkerTask.TaskResult> {

    private final WeakReference<ImageView> imageViewReference;
    private final WeakReference<View> parentReference;
    private String path = null;
    private final int height;

    private FileBitmapWorkerTask(ImageView imageView, View parent, int height) {
        this.height = height;
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
        if (parent != null)
            parentReference = new WeakReference<>(parent);
        else parentReference = null;
    }

    // Decode image in background.
    @Override
    protected TaskResult doInBackground(String... files) {
        path = files[0];
        Bitmap bitmap;
        bitmap = BitmapHelper.readFromDisk(path, height);
        if (bitmap != null) {
            int darkVibrantColor = -1;
            if (parentReference != null)
                darkVibrantColor = new Palette.Builder(bitmap).generate().getDarkVibrantColor(-1);
            return new TaskResult(bitmap, darkVibrantColor);
        } else return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(TaskResult result) {
        if (isCancelled() && result != null)
            result.bitmap = null;

        if (result != null) {
            final ImageView imageView = imageViewReference.get();
            final FileBitmapWorkerTask resourceBitmapWorkerTask =
                    getPopularItemWorkerTask(imageView);
            if (this == resourceBitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(result.bitmap);
                if (parentReference != null && result.darkVibrantColor != -1)
                    parentReference.get().setBackgroundColor(result.darkVibrantColor);
            }
        }
    }

    public static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<FileBitmapWorkerTask> workerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             FileBitmapWorkerTask fileBitmapWorkerTask) {
            super(res, bitmap);
            workerTaskReference =
                    new WeakReference<>(fileBitmapWorkerTask);
        }

        public FileBitmapWorkerTask getBitmapWorkerTask() {
            return workerTaskReference.get();
        }
    }

    public static void loadBitmap(Resources res, String bitmapPath, ImageView imageView, View parent, int height) {
        if (cancelPotentialWork(bitmapPath, imageView)) {
            final FileBitmapWorkerTask task = new FileBitmapWorkerTask(imageView, parent, height);
            final AsyncDrawable asyncDrawable = //todo placeholder
                    new AsyncDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_launcher), task);
            imageView.setImageDrawable(asyncDrawable);
            task.executeOnExecutor(THREAD_POOL_EXECUTOR, bitmapPath);
        }
    }

    private static boolean cancelPotentialWork(String data, ImageView imageView) {
        final FileBitmapWorkerTask task = getPopularItemWorkerTask(imageView);

        if (task != null) {
            final String bitmapData = task.path;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || !bitmapData.equals(data)) {
                // Cancel previous task
                task.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static FileBitmapWorkerTask getPopularItemWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    protected static class TaskResult {
        Bitmap bitmap;
        int darkVibrantColor;

        private TaskResult(Bitmap bitmap, int darkVibrantColor) {
            this.bitmap = bitmap;
            this.darkVibrantColor = darkVibrantColor;
        }
    }

}
