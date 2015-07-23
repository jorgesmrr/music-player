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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaDescription;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import br.jm.music.MusicApplication;
import br.jm.music.R;
import br.jm.music.billing.IabHelper;
import br.jm.music.billing.IabResult;
import br.jm.music.billing.Inventory;
import br.jm.music.billing.Purchase;
import br.jm.music.utils.LogHelper;
import br.jm.music.utils.MediaIDHelper;
import br.jm.music.utils.StringUtils;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = LogHelper.makeLogTag(MainActivity.class);

    public static final String EXTRA_START_FULLSCREEN =
            "br.jm.music.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link PlayerActivity}, speeding up the screen rendering
     * while the {@link android.media.session.MediaController} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "br.jm.music.CURRENT_MEDIA_DESCRIPTION";

    private static final String SKU_REMOVE_ADS = "ads_removal";

    private static final int PURCHASE_REQ_CODE = 10587;

    private Bundle mVoiceSearchParams;

    private LibraryAdapter mAdapter;
    private ViewPager mViewPager;

    private boolean mPurchaseInprogress;

    private IabHelper mHelper;
    private IabHelper.OnIabPurchaseFinishedListener mOnIabPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.d(TAG, "Error purchasing: " + result);
                mPurchaseInprogress = false;
                return;
            }

            if (purchase.getSku().equals(SKU_REMOVE_ADS)) {
                hideAds();

                // Remove purchase item
                supportInvalidateOptionsMenu();
            }

            mPurchaseInprogress = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.container);

        initializeToolbar(false);
        initializeFromParams(savedInstanceState, getIntent());
        setTitle("");

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;

        // Art sizes
        MusicApplication appController = MusicApplication.getInstance();
        if (appController.getArtSizeNormal() == 0) {
            int width = Math.min(screenWidth, size.y);
            appController.setArtSizes((int) (width * 0.83), (int) (width * 0.38));
        }

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }

        handleAds();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (isDisplayingAds())
            getMenuInflater().inflate(R.menu.remove_ads, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.remove_ads) {
            startPurchaseFlow();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        startFullScreenActivityIfNeeded(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }

    protected void startPurchaseFlow() {
        if (!mPurchaseInprogress) {
            mPurchaseInprogress = true;
            mHelper.launchPurchaseFlow(this, SKU_REMOVE_ADS, PURCHASE_REQ_CODE, mOnIabPurchaseFinishedListener);
        }
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, PlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                            intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    @Override
    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null
                && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                    mVoiceSearchParams.getString(SearchManager.QUERY));
        }
        navigateToBrowser(null, null);
    }

    @Override
    protected void handleAds() {
        mPurchaseInprogress = false;
        String base64EncodedPublicKey = "miibiJanbGKQHKWht+e3AvLAp5fzBegjVyW8nRCu8Lm5KWD3UDC1tyyPJoYDfW+FpEYPUoyfHRLXo0wzNLTi8mpxj+sIPl+mHFpINeqd3MjqP6220J2GeW1ytolnY3Wu93XXjelLKuIv/4Y6fNDoqPUXjU0e1XX8BT+uo0qPp/fQUU1mcb7O7/WWpXHJuydg+mx66w6bi5vv4mvM7MH/CGnUie4Pw27Zihxz1Zeh07IbwUhpO9AYZogUxPj44kH+xDmlm5xoh7tjI5enjoVh4MyAGak8f260qq2ybFymch67+nevI+hn7ocXqMCJ/fmirDYUu3vmmEXK0AEPADtqidaqab";
        mHelper = new IabHelper(this, StringUtils.swapCases(base64EncodedPublicKey.substring(0, 14) + StringUtils.getReversedString() + base64EncodedPublicKey.substring(14)));
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.v(TAG, "Problem setting up In-app Billing: " + result);
                } else {
                    mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                            if (result.isFailure()) {
                                Log.v(TAG, "Problem fetching inventory. Connection?");
                                return;
                            }

                            if (!inventory.hasPurchase(SKU_REMOVE_ADS)) {
                                showAds();
                                supportInvalidateOptionsMenu();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void navigateToBrowser(String mediaId, View sharedElement) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);

        if (mAdapter == null) {
            mAdapter = new LibraryAdapter(getSupportFragmentManager(), this);
            mViewPager.setAdapter(mAdapter);
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);
        }

        if (mediaId != null)
            switch (mediaId) {
                case MediaIDHelper.MEDIA_ID_BY_ARTIST:
                    mViewPager.setCurrentItem(0);
                    break;
                case MediaIDHelper.MEDIA_ID_BY_ALBUM:
                    mViewPager.setCurrentItem(1);
                    break;
                case MediaIDHelper.MEDIA_ID_MUSICS_ALL:
                    mViewPager.setCurrentItem(2);
                    break;
                default:
                    super.navigateToBrowser(mediaId, sharedElement);
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

    @Override
    public void setMediaDescription(MediaDescription description) {
        // No need to show the media title
    }

    private static class LibraryAdapter extends FragmentPagerAdapter {
        private List<String> mTitles;
        private SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();

        public LibraryAdapter(FragmentManager fm, Context context) {
            super(fm);
            mTitles = new ArrayList<>();
            mTitles.add(context.getString(R.string.songs));
            mTitles.add(context.getString(R.string.albums));
            mTitles.add(context.getString(R.string.browse_artists));
            mRegisteredFragments = new SparseArray<>(4);
        }

        @Override
        public Fragment getItem(int position) {
            String mediaId;
            switch (position) {
                case 0:
                    mediaId = MediaIDHelper.MEDIA_ID_MUSICS_ALL;
                    break;
                case 1:
                    mediaId = MediaIDHelper.MEDIA_ID_BY_ALBUM;
                    break;
                case 2:
                    mediaId = MediaIDHelper.MEDIA_ID_BY_ARTIST;
                    break;
                default:
                    return null;
            }

            return MediaBrowserFragment.newInstance(mediaId);
        }

        @Override
        public int getCount() {
            return 3;
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
