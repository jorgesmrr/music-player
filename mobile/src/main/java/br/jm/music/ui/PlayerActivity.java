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

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import br.jm.music.MusicApplication;
import br.jm.music.MusicService;
import br.jm.music.R;
import br.jm.music.utils.BitmapHelper;
import br.jm.music.utils.LogHelper;
import br.jm.music.utils.StringUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class PlayerActivity extends ActionBarCastActivity {
    private static final String TAG = LogHelper.makeLogTag(PlayerActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private View mSkipPrev;
    private View mSkipNext;
    private ImageView mShuffle;
    private ImageView mRepeat;
    private FloatingActionButton mPlayPause;
    private TextView mPosition;
    private TextView mDuration;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mArt;
    private View mBar;

    private boolean mShuffling;
    private int mRepeatMode;

    private int mVibrantColor;
    private int mDarkVibrantColor;
    private boolean mIsPortrait;

    private Handler mHandler = new Handler();
    private MediaBrowser mMediaBrowser;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackState mLastPlaybackState;

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            updateExtras(extras);
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
        setContentView(R.layout.activity_player);
        initializeToolbar(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        mRepeatMode = MusicService.REPEAT_NONE;
        mShuffling = false;
        mIsPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        mVibrantColor = getResources().getColor(R.color.default_vibrant_color);
        mDarkVibrantColor = getResources().getColor(R.color.default_dark_vibrant_color);

        mArt = (ImageView) findViewById(R.id.art);
        mPosition = (TextView) findViewById(R.id.position);
        mDuration = (TextView) findViewById(R.id.duration);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mTitle = (TextView) findViewById(R.id.title);
        mSubtitle = (TextView) findViewById(R.id.subtitle);
        mBar = findViewById(R.id.bar);
        final View controlBar = findViewById(R.id.controllers);
        mPlayPause = (FloatingActionButton) controlBar.findViewById(R.id.play_pause);
        mSkipNext = controlBar.findViewById(R.id.next);
        mSkipPrev = controlBar.findViewById(R.id.prev);
        mShuffle = (ImageView) controlBar.findViewById(R.id.shuffle);
        mRepeat = (ImageView) controlBar.findViewById(R.id.repeat);

        mTitle.setSelected(true);

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMediaController().getTransportControls().skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMediaController().getTransportControls().skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackState state = getMediaController().getPlaybackState();
                MediaController.TransportControls controls =
                        getMediaController().getTransportControls();
                switch (state.getState()) {
                    case PlaybackState.STATE_PLAYING:
                        controls.pause();
                        stopSeekbarUpdate();
                        break;
                    case PlaybackState.STATE_PAUSED:
                    case PlaybackState.STATE_STOPPED:
                        controls.play();
                        scheduleSeekbarUpdate();
                        break;
                    default:
                        LogHelper.d(TAG, "onClick with state ", state.getState());
                }
            }
        });

        mShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(PlayerActivity.this, MusicService.class)
                        .setAction(MusicService.ACTION_CMD)
                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_TOGGLE_SHUFFLE));
            }
        });

        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(PlayerActivity.this, MusicService.class)
                        .setAction(MusicService.ACTION_CMD)
                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_TOGGLE_REPEAT));
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPosition != null) {
                    mPosition.setText(StringUtils.formatMillis(seekBar.getProgress()));
                    mDuration.setText("-" + StringUtils.formatMillis(seekBar.getMax() - seekBar.getProgress()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        // Translates seekbar
        if (mIsPortrait) {
            controlBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                        mBar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    else
                        mBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    mSeekbar.setTranslationY(controlBar.getTop() - mSeekbar.getHeight() / 2);
                }
            });
        }

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null)
            updateFromParams(getIntent());

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class), mConnectionCallback, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null)
            switch (item.getItemId()) {
                case R.id.show_queue:
                    startActivity(new Intent(this, QueueActivity.class));
                    return true;
                case R.id.go_album:
                    MediaMetadata metadata = getMediaController().getMetadata();
                    if (metadata != null)
                        startService(new Intent(this, MusicService.class)
                                .setAction(MusicService.ACTION_CMD)
                                .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ALBUM)
                                .putExtra(MusicService.EXTRA_MEDIA_ID, metadata.getDescription().getMediaId()));
                    return true;
                case R.id.go_artist:
                    metadata = getMediaController().getMetadata();
                    if (metadata != null)
                        startService(new Intent(this, MusicService.class)
                                .setAction(MusicService.ACTION_CMD)
                                .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ARTIST)
                                .putExtra(MusicService.EXTRA_MEDIA_ID, metadata.getDescription().getMediaId()));
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
        PlaybackState state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadata metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
        updateProgress();
        updateExtras(mediaController.getExtras());
        if (state != null && (state.getState() == PlaybackState.STATE_PLAYING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            MediaDescription description = intent.getParcelableExtra(
                    MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
            if (description != null) {
                updateMediaDescription(description);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    private void fetchImageAsync(MediaDescription description) {
        Uri artUri = description.getIconUri();
        if (artUri != null) {
            String path = artUri.toString();
            final Bitmap art = BitmapHelper.readFromDisk(path, MusicApplication.getInstance().getArtSizeNormal());
            if (art != null) {
                new Palette.Builder(art).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        mVibrantColor = palette.getVibrantColor(mVibrantColor);
                        mDarkVibrantColor = palette.getDarkVibrantColor(mDarkVibrantColor);
                        PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(mVibrantColor, PorterDuff.Mode.SRC_ATOP);

                        mPlayPause.getBackground().setColorFilter(colorFilter);
                        mBar.setBackgroundColor(mDarkVibrantColor);

                        if (mShuffling)
                            mShuffle.setColorFilter(mVibrantColor);
                        if (mRepeatMode != MusicService.REPEAT_NONE)
                            mRepeat.setColorFilter(mVibrantColor);

                        mSeekbar.getProgressDrawable().invalidateSelf();
                        mSeekbar.getProgressDrawable().setColorFilter(colorFilter);
                        mSeekbar.getThumb().invalidateSelf();
                        mSeekbar.getThumb().setColorFilter(colorFilter);

                        mArt.setImageBitmap(art);

                        getToolbar().setBackgroundColor(mDarkVibrantColor);
                    }
                });
            } else
                mArt.setImageBitmap(BitmapHelper.getDefault(getResources(), MusicApplication.DEF_ART_SIZE_NORMAL));
        } else
            mArt.setImageBitmap(BitmapHelper.getDefault(getResources(), MusicApplication.DEF_ART_SIZE_NORMAL));
    }

    private void updateMediaDescription(MediaDescription description) {
        if (description == null) {
            return;
        }
        LogHelper.d(TAG, "updateMediaDescription called ");
        mTitle.setText(description.getTitle());
        mSubtitle.setText(description.getSubtitle());
        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadata metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.d(TAG, "updateDuration called ");
        if (mSeekbar != null && mPosition != null) {
            int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            mSeekbar.setMax(duration);
            mPosition.setText(StringUtils.formatMillis(mSeekbar.getProgress()));
            mDuration.setText("-" + StringUtils.formatMillis(mSeekbar.getMax() - mSeekbar.getProgress()));
        }
    }

    private void updatePlaybackState(PlaybackState state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;

        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                mPlayPause.setSelected(true);
                scheduleSeekbarUpdate();
                break;
            case PlaybackState.STATE_PAUSED:
                mPlayPause.setSelected(false);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_STOPPED:
                mPlayPause.setSelected(false);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE);
        mSkipPrev.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE);
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() != PlaybackState.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaController.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
    }

    private void updateExtras(Bundle extras) {
        boolean shuffling = extras.getBoolean(MusicService.EXTRA_SHUFFLING);
        int repeatMode = extras.getInt(MusicService.EXTRA_REPEAT_MODE, MusicService.REPEAT_NONE);

        if (shuffling != mShuffling) {
            if (shuffling)
                mShuffle.setColorFilter(mVibrantColor);
            else
                mShuffle.setColorFilter(getResources().getColor(R.color.grey_icon));
            mShuffling = shuffling;
        }

        if (repeatMode != mRepeatMode) {
            switch (repeatMode) {
                case MusicService.REPEAT_NONE:
                    mRepeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                    mRepeat.setColorFilter(getResources().getColor(R.color.grey_icon));
                    break;
                case MusicService.REPEAT_ONCE:
                    mRepeat.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                    mRepeat.setColorFilter(mVibrantColor);
                    break;
                case MusicService.REPEAT_ALL:
                    mRepeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                    mRepeat.setColorFilter(mVibrantColor);
                    break;
            }
            mRepeatMode = repeatMode;
        }
    }
}
