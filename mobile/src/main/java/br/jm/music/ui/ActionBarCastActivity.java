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

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;

import br.jm.music.R;
import br.jm.music.utils.LogHelper;
import br.jm.music.utils.PrefUtils;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 * <p/>
 * The requirements for a subclass is to call {@link #initializeToolbar(boolean)} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link android.support.v7.widget.Toolbar} with id 'toolbar',
 * a {@link android.support.v4.widget.DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(ActionBarCastActivity.class);
    private static final int DELAY_MILLIS = 1000;

    private VideoCastManager mCastManager;
    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;

    private boolean mToolbarInitialized;
    private int mStatusBarHeight;

    private VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onFailed(int resourceId, int statusCode) {
            LogHelper.d(TAG, "onFailed ", resourceId, " status ", statusCode);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            LogHelper.d(TAG, "onConnectionSuspended() was called with cause: ", cause);
        }

        @Override
        public void onConnectivityRecovered() {
        }

        @Override
        public void onCastDeviceDetected(final MediaRouter.RouteInfo info) {
            // FTU stands for First Time Use:
            if (!PrefUtils.isFtuShown(ActionBarCastActivity.this)) {
                // If user is seeing the cast button for the first time, we will
                // show an overlay that explains what that button means.
                PrefUtils.setFtuShown(ActionBarCastActivity.this, true);

                LogHelper.d(TAG, "Route is visible: ", info);
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (mMediaRouteMenuItem.isVisible()) {
                            LogHelper.d(TAG, "Cast Icon is visible: ", info.getName());
                            showFtu();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this);

        mCastManager = VideoCastManager.getInstance();
        mCastManager.reconnectSessionIfPossible();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                    "the end of your onCreate method");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected Toolbar getToolbar() {
        return mToolbar;
    }

    protected void initializeToolbar(boolean addPadding) {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                    "'toolbar'");
        }
        if (addPadding) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
                mToolbar.setPadding(0, mStatusBarHeight, 0, 0);
            }
        }
        mToolbar.inflateMenu(R.menu.remove_ads);
        setSupportActionBar(mToolbar);

        mToolbarInitialized = true;
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private void showFtu() {
        Menu menu = mToolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view != null && view instanceof MediaRouteButton) {
            new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget(view))
                    .setContentTitle(R.string.touch_to_cast)
                    .hideOnTouchOutside()
                    .build();
        }
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }
}
