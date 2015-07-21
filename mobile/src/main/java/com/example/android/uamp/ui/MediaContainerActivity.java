package com.example.android.uamp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Displays a fragment with the music tracks for the given media ID
 * Created by Jorge on 07/06/2015.
 */
public abstract class MediaContainerActivity extends BaseActivity {

    private static final String TAG = LogHelper.makeLogTag(MediaContainerActivity.class);
    public static final String SAVED_MEDIA_ID = "media_id";
    public static final String BROWSE_FRAG_TAG = "browse_frag";

    private String mMediaId;
    protected TextView mTitleView;
    protected TextView mSubtitleView;
    protected FloatingActionButton mFab;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mMediaId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_queue:
                startService(new Intent(this, MusicService.class)
                        .setAction(MusicService.ACTION_CMD)
                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_ADD_TO_QUEUE)
                        .putExtra(MusicService.EXTRA_MEDIA_ID, mMediaId));
                return true;
            case R.id.shuffle_all:
                Bundle extras = new Bundle();
                extras.putBoolean(MusicService.EXTRA_SHUFFLE, true);
                getMediaController().getTransportControls().playFromMediaId(mMediaId, extras);
                return true;
        }
        return false;
    }

    protected void initializeViews() {
        initializeToolbar(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        mTitleView = (TextView) findViewById(R.id.title);
        mSubtitleView = (TextView) findViewById(R.id.subtitle);
        mFab = (FloatingActionButton) findViewById(R.id.fab);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMediaController().getTransportControls().playFromMediaId(mMediaId, null);
            }
        });
    }

    @Override
    protected void onMediaControllerConnected() {
        getBrowseFragment().onConnected();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getSupportFragmentManager().findFragmentByTag(BROWSE_FRAG_TAG);
    }

    private String getMediaId() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            return extras.getString(SAVED_MEDIA_ID);
        }
        return null;
    }

    @Override
    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        if (savedInstanceState != null) {
            // If there is a saved media ID, use it
            mMediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
        } else {
            mMediaId = getMediaId();
        }
        navigateToBrowser(mMediaId, null);
    }

    @Override
    protected void navigateToBrowser(String mediaId, View sharedElement) {
        if (mediaId.equals(mMediaId))
            openFragment(mediaId);
        else {
            super.navigateToBrowser(mediaId, sharedElement);
        }
    }

    protected abstract void openFragment(String mediaId);
}
