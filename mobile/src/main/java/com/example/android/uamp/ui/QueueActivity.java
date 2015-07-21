package com.example.android.uamp.ui;

import android.content.ComponentName;
import android.content.Intent;
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
import com.example.android.uamp.utils.MediaIDHelper;

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

            @Override
            public void onMenuItemClick(MenuItem item, int position) {
                MediaSession.QueueItem queueItem;
                switch (item.getItemId()) {
                    case R.id.go_album:
                        queueItem = mAdapter.getQueue().get(position);
                        startService(new Intent(QueueActivity.this, MusicService.class)
                                .setAction(MusicService.ACTION_CMD)
                                .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ALBUM)
                                .putExtra(MusicService.EXTRA_MEDIA_ID, queueItem.getDescription().getMediaId()));
                        break;
                    case R.id.go_artist:
                        queueItem = mAdapter.getQueue().get(position);
                        startService(new Intent(QueueActivity.this, MusicService.class)
                                .setAction(MusicService.ACTION_CMD)
                                .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ARTIST)
                                .putExtra(MusicService.EXTRA_MEDIA_ID, queueItem.getDescription().getMediaId()));
                        break;
                    case R.id.remove_queue:
                        startService(new Intent(QueueActivity.this, MusicService.class)
                                .setAction(MusicService.ACTION_CMD)
                                .putExtra(MusicService.CMD_NAME, MusicService.CMD_DEL_FROM_QUEUE)
                                .putExtra(MusicService.EXTRA_QUEUE_INDEX, position));
                        break;
                    case R.id.delete:
                        break;
                }
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
        mAdapter.setQueue(mediaController.getQueue());
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

    private void updateCurrentMediaId(String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item : mAdapter.getQueue()) {
            if (mediaId.equals(MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId())))
                break;
            index++;
        }
        mAdapter.setCurrentIndex(index);
    }
}
