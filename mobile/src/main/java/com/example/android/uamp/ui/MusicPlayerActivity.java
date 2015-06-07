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
package com.example.android.uamp.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String SAVED_MEDIA_ID = "com.example.android.uamp.MEDIA_ID";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.example.android.uamp.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenPlayerActivity}, speeding up the screen rendering
     * while the {@link android.media.session.MediaController} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION";

    private Bundle mVoiceSearchParams;

    private LibraryAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);
        mViewPager = (ViewPager) findViewById(R.id.container);

        initializeToolbar();
        initializeFromParams(savedInstanceState, getIntent());

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenPlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                            intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null
                && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                    mVoiceSearchParams.getString(SearchManager.QUERY));
        } else {
            if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
            }
        }
        navigateToBrowser(mediaId);
    }

    private void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);

        if (mAdapter == null) {
            mAdapter = new LibraryAdapter(getSupportFragmentManager(), this);
            mViewPager.setAdapter(mAdapter);
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);
        }

        if (mediaId != null)
            switch (mediaId) {
                case MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE:
                    mViewPager.setCurrentItem(0);
                    break;
                case MediaIDHelper.MEDIA_ID_ALBUMS_BY_ARTIST:
                    mViewPager.setCurrentItem(1);
                    break;
                case MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM:
                    mViewPager.setCurrentItem(2);
                    break;
                case MediaIDHelper.MEDIA_ID_MUSICS_ALL:
                    mViewPager.setCurrentItem(3);
                    break;
                default:
                    //todo open activity sending mediaId
                /*MediaBrowserFragment fragment = getBrowseFragment();

                if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
                    fragment = new MediaBrowserFragment();
                    fragment.setMediaId(mediaId);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(
                            R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                            R.animator.slide_in_from_left, R.animator.slide_out_to_right);
                    transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
                    // If this is not the top level media (root), we add it to the fragment back stack,
                    // so that actionbar toggle and Back will work appropriately:
                    if (mediaId != null) {
                        transaction.addToBackStack(null);
                    }
                    transaction.commit();
                }*/
            }
    }

    @Override
    protected void onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            getMediaController().getTransportControls().playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }

        for (int i = 0; i < 4; i++) {
            Fragment fragment = mAdapter.getRegisteredFragment(i);
            if (fragment != null)
                ((MediaBrowserFragment) fragment).onConnected();
        }
    }

    private static class LibraryAdapter extends FragmentPagerAdapter {
        private List<String> mTitles;
        private SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();

        public LibraryAdapter(FragmentManager fm, Context context) {
            super(fm);
            mTitles = new ArrayList<>();
            mTitles.add(context.getString(R.string.browse_genres));
            mTitles.add(context.getString(R.string.browse_artists));
            mTitles.add(context.getString(R.string.browse_albums));
            mTitles.add(context.getString(R.string.browse_songs));
            mRegisteredFragments = new SparseArray<>(4);
        }

        @Override
        public Fragment getItem(int position) {
            String mediaId;
            switch (position) {
                case 0:
                    mediaId = MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
                    break;
                case 1:
                    mediaId = MediaIDHelper.MEDIA_ID_ALBUMS_BY_ARTIST;
                    break;
                case 2:
                    mediaId = MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM;
                    break;
                case 3:
                    mediaId = MediaIDHelper.MEDIA_ID_MUSICS_ALL;
                    break;
                default:
                    return null;
            }

            MediaBrowserFragment fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            return fragment;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mRegisteredFragments.delete(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return mRegisteredFragments.get(position);
        }
    }
}
