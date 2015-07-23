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
package br.jm.music.ui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import br.jm.music.MusicService;
import br.jm.music.R;
import br.jm.music.utils.LogHelper;
import br.jm.music.utils.MediaIDHelper;
import br.jm.music.utils.ResourceHelper;

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
public abstract class BaseActivity extends ActionBarCastActivity implements MediaBrowserFragment.MediaBrowserListener {

    private static final String TAG = LogHelper.makeLogTag(BaseActivity.class);

    protected static final String EXTRA_DISPLAY_ADS = "DISPLAY_ADS";
    public static final String ACTION_OPEN_MEDIA_ID = "ACTION_OPEN_MEDIA_ID";

    private MediaBrowser mMediaBrowser;
    private PlaybackControlsFragment mControlsFragment;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mIsDisplayingAds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogHelper.d(TAG, "Activity onCreate");

        mIsDisplayingAds = false;

        // Since our app icon has the same color as colorPrimary, our entry in the Recent Apps
        // list gets weird. We need to change either the icon or the color of the TaskDescription.
        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
                getTitle().toString(),
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_headset_white_24dp),
                ResourceHelper.getThemeColor(this, R.attr.colorPrimary, android.R.color.darker_gray));
        setTaskDescription(taskDesc);

        // Connect a media browser just to get the media session token. There are other ways
        // this can be done, for example by sharing the session token directly.
        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class), mConnectionCallback, null);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent startIntent) {
                if (startIntent != null) {
                    String action = startIntent.getAction();
                    String mediaId = startIntent.getStringExtra(MusicService.EXTRA_MEDIA_ID);
                    if (ACTION_OPEN_MEDIA_ID.equals(action))
                        navigateToBrowser(mediaId, null);
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver), new IntentFilter(ACTION_OPEN_MEDIA_ID));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.d(TAG, "Activity onStart");

        mControlsFragment = (PlaybackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        if (mControlsFragment == null) {
            throw new IllegalStateException("Mising fragment with id 'controls'. Cannot continue.");
        }

        hidePlaybackControls();

        mMediaBrowser.connect();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver), new IntentFilter(ACTION_OPEN_MEDIA_ID));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "Activity onStop");
        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mMediaControllerCallback);
        }
        mMediaBrowser.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    protected void showAds() {
        mIsDisplayingAds = true;
        ViewGroup adContainer = (ViewGroup) findViewById(R.id.ad_container);
        if (adContainer != null) {
            AdView adView = (AdView) LayoutInflater.from(this).inflate(R.layout.include_ad, adContainer, false);
            adContainer.addView(adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    protected void hideAds() {
        mIsDisplayingAds = false;
        ((ViewGroup) findViewById(R.id.ad_container)).removeAllViews();
    }

    @Override
    public MediaBrowser getMediaBrowser() {
        return mMediaBrowser;
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item, View sharedElement) {
        LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            //todo sharedElement.setTransitionName("image");
            navigateToBrowser(item.getMediaId(), sharedElement);
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    protected void navigateToBrowser(String mediaId, View sharedElement) {
        if (mediaId.startsWith(MediaIDHelper.MEDIA_ID_BY_ALBUM)) {
            Intent intent = new Intent(this, AlbumActivity.class)
                    .putExtra(MediaContainerActivity.SAVED_MEDIA_ID, mediaId)
                    .putExtra(EXTRA_DISPLAY_ADS, mIsDisplayingAds);
            //todo ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, sharedElement, "image");
            startActivity(intent/*todo, options.toBundle()*/);
        } else {
            Intent intent = new Intent(this, ArtistActivity.class)
                    .putExtra(MediaContainerActivity.SAVED_MEDIA_ID, mediaId)
                    .putExtra(EXTRA_DISPLAY_ADS, mIsDisplayingAds);
            startActivity(intent);
        }
    }

    protected abstract void onMediaControllerConnected();

    protected void showPlaybackControls() {
        LogHelper.d(TAG, "showPlaybackControls");
        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                .show(mControlsFragment)
                .commit();
    }

    protected void hidePlaybackControls() {
        LogHelper.d(TAG, "hidePlaybackControls");
        getFragmentManager().beginTransaction()
                .hide(mControlsFragment)
                .commit();
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible.
     */
    protected boolean shouldShowControls() {
        MediaController mediaController = getMediaController();
        if (mediaController == null ||
                mediaController.getMetadata() == null ||
                mediaController.getPlaybackState() == null) {
            return false;
        }
        Log.v("t", mediaController.getPlaybackState().getState() + "");
        switch (mediaController.getPlaybackState().getState()) {
            case PlaybackState.STATE_ERROR:
            case PlaybackState.STATE_NONE:
                return false;
            default:
                return true;
        }
    }

    private void connectToSession(MediaSession.Token token) {
        MediaController mediaController = new MediaController(this, token);
        setMediaController(mediaController);
        mediaController.registerCallback(mMediaControllerCallback);

        if (shouldShowControls()) {
            showPlaybackControls();
        } else {
            LogHelper.d(TAG, "connectionCallback.onConnected: " +
                    "hiding controls because metadata is null");
            hidePlaybackControls();
        }

        if (mControlsFragment != null) {
            mControlsFragment.onConnected();
        }

        onMediaControllerConnected();
    }

    // Callback that ensures that we are showing the controls
    private final MediaController.Callback mMediaControllerCallback =
            new MediaController.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        LogHelper.d(TAG, "mediaControllerCallback.onPlaybackStateChanged: " +
                                        "hiding controls because state is ",
                                state == null ? "null" : state.getState());
                        hidePlaybackControls();
                    }
                }

                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        LogHelper.d(TAG, "mediaControllerCallback.onMetadataChanged: " +
                                "hiding controls because metadata is null");
                        hidePlaybackControls();
                    }
                }
            };

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected");

                    MediaSession.Token token = mMediaBrowser.getSessionToken();
                    if (token == null) {
                        throw new IllegalArgumentException("No Session token");
                    }
                    connectToSession(token);
                }
            };

    protected abstract void initializeFromParams(Bundle savedInstanceState, Intent intent);

    protected abstract void handleAds();

    public boolean isDisplayingAds() {
        return mIsDisplayingAds;
    }
}
