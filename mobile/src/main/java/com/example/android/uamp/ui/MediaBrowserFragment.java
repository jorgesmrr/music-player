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

import android.app.Activity;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.List;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowser} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowser.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaBrowserFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

    private static final String ARG_MEDIA_ID = "media_id";

    private String mMediaId;
    private MediaBrowserListener mMediaFragmentListener;

    private BrowseAdapter mBrowserAdapter;
    private RecyclerView.OnScrollListener mOnScrollListener;
    private int mHeaderHeight;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata change to media ",
                    metadata.getDescription().getMediaId());
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
            mBrowserAdapter.notifyDataSetChanged();
        }
    };

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                    try {
                        LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
                                "  count=" + children.size());
                        mBrowserAdapter.clear();
                        for (MediaBrowser.MediaItem item : children) {
                            mBrowserAdapter.add(item);
                        }
                        mBrowserAdapter.notifyDataSetChanged();
                    } catch (Throwable t) {
                        LogHelper.e(TAG, "Error on childrenloaded", t);
                    }
                }

                @Override
                public void onError(String id) {
                    LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
                    Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                }
            };

    public static MediaBrowserFragment newInstance(String mediaId) {
        MediaBrowserFragment fragment = new MediaBrowserFragment();
        fragment.setMediaId(mediaId);
        fragment.mHeaderHeight = 0;
        return fragment;
    }

    public static MediaBrowserFragment newInstance(String mediaId, RecyclerView.OnScrollListener scrollListener, int headerHeight) {
        MediaBrowserFragment fragment = new MediaBrowserFragment();
        fragment.setMediaId(mediaId);
        fragment.mOnScrollListener = scrollListener;
        fragment.mHeaderHeight = headerHeight;
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaSelectedListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaBrowserListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.d(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mMediaId = getMediaId();

        mBrowserAdapter = new BrowseAdapter(
                getActivity(),
                mMediaId,
                mHeaderHeight,
                new BrowseAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(MediaBrowser.MediaItem mediaItem, View sharedElement) {
                        mMediaFragmentListener.onMediaItemSelected(mediaItem, sharedElement);
                    }

                    @Override
                    public void onMenuItemClick(MenuItem item, int position) {
                        MediaBrowser.MediaItem mediaItem = mBrowserAdapter.get(position);
                        switch (item.getItemId()) {
                            case R.id.add_queue:
                                getActivity().startService(new Intent(getActivity(), MusicService.class)
                                        .setAction(MusicService.ACTION_CMD)
                                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_ADD_TO_QUEUE)
                                        .putExtra(MusicService.EXTRA_MEDIA_ID, mediaItem.getMediaId()));
                                break;
                            case R.id.go_album:
                                getActivity().startService(new Intent(getActivity(), MusicService.class)
                                        .setAction(MusicService.ACTION_CMD)
                                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ALBUM)
                                        .putExtra(MusicService.EXTRA_MEDIA_ID, mediaItem.getMediaId()));
                                break;
                            case R.id.go_artist:
                                getActivity().startService(new Intent(getActivity(), MusicService.class)
                                        .setAction(MusicService.ACTION_CMD)
                                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_GET_ARTIST)
                                        .putExtra(MusicService.EXTRA_MEDIA_ID, mediaItem.getMediaId()));
                                break;
                            case R.id.play_next:
                                getActivity().startService(new Intent(getActivity(), MusicService.class)
                                        .setAction(MusicService.ACTION_CMD)
                                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_ADD_TO_QUEUE)
                                        .putExtra(MusicService.EXTRA_MEDIA_ID, mediaItem.getMediaId())
                                        .putExtra(MusicService.EXTRA_PLAY_NEXT, true));
                                break;
                            case R.id.shuffle_all:
                                Bundle extras = new Bundle();
                                extras.putBoolean(MusicService.EXTRA_SHUFFLE, true);
                                getActivity().getMediaController().getTransportControls().playFromMediaId(mediaItem.getMediaId(), extras);
                                break;
                            //todo
                            /*case R.id.delete:
                                getActivity().startService(new Intent(getActivity(), MusicService.class)
                                        .setAction(MusicService.ACTION_CMD)
                                        .putExtra(MusicService.CMD_NAME, MusicService.CMD_DEL_FROM_DEVICE)
                                        .putExtra(MusicService.EXTRA_MEDIA_ID, mediaItem.getMediaId()));
                                mBrowserAdapter.remove(position);
                               break;*/
                        }
                    }
                });

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        recyclerView.setLayoutManager(mBrowserAdapter.getSuitableLayoutManager(getActivity()));
        recyclerView.setAdapter(mBrowserAdapter);
        if (mOnScrollListener != null)
            recyclerView.addOnScrollListener(mOnScrollListener);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // fetch browsing information to fill the listview:
        MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        LogHelper.d(TAG, "fragment.onStart, mediaId=", mMediaId,
                "  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }

    public void setMediaId(String mediaId) {
        Bundle args = new Bundle(1);
        args.putString(MediaBrowserFragment.ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        if (isDetached()) {
            return;
        }

        if (mMediaId == null) {
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }
        updateTitle();

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mMediaFragmentListener.getMediaBrowser().unsubscribe(mMediaId);

        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().registerCallback(mMediaControllerCallback);
        }
    }

    private void updateTitle() {
        if (MediaIDHelper.MEDIA_ID_ROOT.equals(mMediaId)) {
            return;
        }

        final String parentId = MediaIDHelper.getParentMediaID(mMediaId);

        // MediaBrowser doesn't provide metadata for a given mediaID, only for its children. Since
        // the mediaId contains the item's hierarchy, we know the item's parent mediaId and we can
        // fetch and iterate over it and find the proper MediaItem, from which we get the title,
        // This is temporary - a better solution (a method to get a mediaItem by its mediaID)
        // is being worked out in the platform and should be available soon.
        LogHelper.d(TAG, "on updateTitle: mediaId=", mMediaId, " parentID=", parentId);
        if (parentId != null) {
            MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();
            LogHelper.d(TAG, "on updateTitle: mediaBrowser is ",
                    mediaBrowser == null ? "null" : ("not null, connected=" + mediaBrowser.isConnected()));
            if (mediaBrowser != null && mediaBrowser.isConnected()) {
                // Unsubscribing is required to guarantee that we will get the initial values.
                // Otherwise, if there is another callback subscribed to this mediaID, mediaBrowser
                // will only call this callback when the media content change.
                mediaBrowser.unsubscribe(parentId);
                mediaBrowser.subscribe(parentId, new MediaBrowser.SubscriptionCallback() {
                    @Override
                    public void onChildrenLoaded(String parentId,
                                                 List<MediaBrowser.MediaItem> children) {
                        LogHelper.d(TAG, "Got ", children.size(), " children for ", parentId,
                                ". Looking for ", mMediaId);
                        for (MediaBrowser.MediaItem item : children) {
                            LogHelper.d(TAG, "child ", item.getMediaId());
                            if (item.getMediaId().equals(mMediaId)) {
                                if (mMediaFragmentListener != null) {
                                    mMediaFragmentListener.setMediaDescription(
                                            item.getDescription());
                                }
                                return;
                            }
                        }
                        mMediaFragmentListener.getMediaBrowser().unsubscribe(parentId);
                    }

                    @Override
                    public void onError(String id) {
                        super.onError(id);
                        LogHelper.d(TAG, "subscribe error: id=", id);
                    }
                });
            }
        }
    }

    public interface MediaBrowserListener extends MediaBrowserProvider {
        void onMediaItemSelected(MediaBrowser.MediaItem item, View sharedElement);

        void setMediaDescription(MediaDescription description);
    }

}
