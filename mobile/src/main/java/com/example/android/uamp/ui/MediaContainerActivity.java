package com.example.android.uamp.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

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
    protected FloatingActionButton mFab;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mMediaId);
        }
        super.onSaveInstanceState(outState);
    }

    protected void initializeViews(){
        initializeToolbar(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        mTitleView = (TextView) findViewById(R.id.title);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mMediaId.startsWith(MediaIDHelper.MEDIA_ID_BY_ALBUM))
            getMenuInflater().inflate(R.menu.album, menu);
        return true;
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
            Intent intent = new Intent(this, MediaContainerActivity.class).putExtra(MediaContainerActivity.SAVED_MEDIA_ID, mediaId);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, sharedElement, "image");
            startActivity(intent, options.toBundle());
        }
    }

    @Override
    public void setMediaTitle(CharSequence title) {
        mTitleView.setText(title);
    }

    protected abstract void openFragment(String mediaId);
}
