/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.jm.music;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import br.jm.music.utils.LruBitmapCache;

/**
 * The {@link Application} for the uAmp application.
 */
public class MusicApplication extends Application {

    public static final String TAG = "AppController";

    // Keys (values MUST NOT change!)
    public static final String PREF_ART_SIZE_NORMAL = "art_normal";
    public static final String PREF_ART_SIZE_SMALL = "art_small";

    public static final int DEF_ART_SIZE_NORMAL = 600;
    public static final int DEF_ART_SIZE_SMALL = 270;
    public static final int DEF_ART_SIZE_ICON = 100;

    private int mArtSizeNormal;
    private int mArtSizeSmall;

    private LruBitmapCache lruBitmapCache;
    private Tracker mTracker;

    private static MusicApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mArtSizeNormal = sharedPreferences.getInt(PREF_ART_SIZE_NORMAL, 0);
        mArtSizeSmall = sharedPreferences.getInt(PREF_ART_SIZE_SMALL, 0);
    }

    public static synchronized MusicApplication getInstance() {
        return mInstance;
    }

    public static int getDefaultArtRes(int height) {
        if (height >= DEF_ART_SIZE_NORMAL)
            return R.drawable.default_art_normal;
        else if (height >= DEF_ART_SIZE_SMALL)
            return R.drawable.default_art_small;
        else return R.drawable.default_art_icon;
    }

    public static int getArtSizeIcon() {
        return DEF_ART_SIZE_ICON;
    }

    public int getArtSizeNormal() {
        return mArtSizeNormal == 0 ? DEF_ART_SIZE_NORMAL : mArtSizeNormal;
    }

    public int getArtSizeSmall() {
        return mArtSizeSmall == 0 ? DEF_ART_SIZE_SMALL : mArtSizeSmall;
    }

    public void setArtSizes(int normal, int small) {
        mArtSizeNormal = normal;
        mArtSizeSmall = small;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                .putInt(PREF_ART_SIZE_NORMAL, mArtSizeNormal)
                .putInt(PREF_ART_SIZE_SMALL, mArtSizeSmall)
                .apply();
    }

    public LruBitmapCache getLruBitmapCache() {
        if (lruBitmapCache == null) {
            lruBitmapCache = new LruBitmapCache();
        }
        return this.lruBitmapCache;
    }

    public synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.tracker);
        }
        return mTracker;
    }
}
