package com.example.android.uamp.ui;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.QueueHelper;

import java.util.List;

/**
 * Created by Jorge on 11/06/2015.
 */
public class QueueActivity extends ActionBarCastActivity {
    private static final String TAG = LogHelper.makeLogTag(QueueActivity.class);

    private MediaBrowser mMediaBrowser;
    private QueueAdapter mAdapter;

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            //todo caso precise updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null)
                updateCurrentMediaId(metadata.getDescription().getMediaId());
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_queue);

        mAdapter = new QueueAdapter(new QueueAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MediaSession.QueueItem queueItem) {
                MediaController.TransportControls controls =
                        getMediaController().getTransportControls();
                controls.skipToQueueItem(queueItem.getQueueId());
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        initializeToolbar(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class), mConnectionCallback, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectToSession(MediaSession.Token token) {
        MediaController mediaController = new MediaController(this, token);
        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }
        setMediaController(mediaController);
        mediaController.registerCallback(mCallback);
        updateQueue(mediaController.getQueue());
        MediaMetadata metadata = mediaController.getMetadata();
        if (metadata != null)
            updateCurrentMediaId(metadata.getDescription().getMediaId());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mCallback);
        }
    }

    private void updateQueue(List<MediaSession.QueueItem> queue) {
        mAdapter.setQueue(queue);
    }

    private void updateCurrentMediaId(String mediaId) {
        mAdapter.setCurrentIndex(QueueHelper.getMusicIndexOnQueue(mAdapter.getQueue(), mediaId));
    }
}
