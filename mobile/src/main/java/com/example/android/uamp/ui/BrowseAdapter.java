package com.example.android.uamp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_BY_ALBUM;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_BY_ARTIST;

/**
 * An adapter for showing the list of browsed MediaItems
 * Created by Jorge on 07/06/2015.
 */
public class BrowseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_PLAYABLE = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_PLAYING = 3;
    private static final int TYPE_PLACEHOLDER = 4;
    private static final int TYPE_HEADER = 5;

    public static final int MEDIA_ARTIST = 1;
    public static final int MEDIA_ALBUM = 2;
    public static final int MEDIA_SONG = 3;
    public static final int MEDIA_ALBUMS_SONGS = 4;

    private static final String TAG = LogHelper.makeLogTag(BrowseAdapter.class);

    private ColorStateList mColorStatePlaying;
    private ColorStateList mColorStateNotPlaying;
    private Activity mActivity;
    private ArrayList<MediaBrowser.MediaItem> mMediaItems;
    private final Set<Integer> mHeadersPositions;
    private OnItemClickListener mListener;
    private int mMediaType;
    private int mPlaceholderHeight;

    public BrowseAdapter(Activity activity, String mediaId, int placeholderHeight, OnItemClickListener listener) {
        initializeColorStateLists(activity);
        mMediaItems = new ArrayList<>();
        mHeadersPositions = new TreeSet<>();
        mActivity = activity;
        mListener = listener;
        if (mediaId.equals(MEDIA_ID_BY_ARTIST))
            mMediaType = MEDIA_ARTIST;
        else if (mediaId.startsWith(MEDIA_ID_BY_ARTIST))
            mMediaType = MEDIA_ALBUMS_SONGS;
        else if (mediaId.equals(MEDIA_ID_BY_ALBUM))
            mMediaType = MEDIA_ALBUM;
        else
            mMediaType = MEDIA_SONG;
        mPlaceholderHeight = placeholderHeight;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_PLACEHOLDER)
            return new PlaceHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_placeholder, parent, false));
        else if (viewType == TYPE_HEADER) {
            return new HeaderHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_header, parent, false));
        } else {
            int layout;
            if (mMediaType == MEDIA_ALBUM)
                layout = R.layout.grid_item_album;
            else if (mMediaType == MEDIA_ALBUMS_SONGS && viewType == TYPE_NONE)
                layout = R.layout.list_item_album;
            else
                layout = R.layout.list_item_two_lines_overflow;

            View itemView = LayoutInflater.from(mActivity)
                    .inflate(layout, parent, false);
            MediaItemViewHolder holder = new MediaItemViewHolder(itemView);

            switch (viewType) {
                case TYPE_NONE:
                    switch (mMediaType) {
                        case MEDIA_ARTIST:
                            holder.mImageView.setImageResource(R.drawable.ic_by_genre);
                            holder.mOverflowView.setVisibility(View.GONE);
                            break;
                        case MEDIA_ALBUM:
                        case MEDIA_ALBUMS_SONGS:
                            holder.mImageView.setImageResource(R.drawable.placeholder);
                            break;
                    }
                    break;
                case TYPE_PLAYABLE:
                    holder.mImageView.setImageDrawable(
                            mActivity.getDrawable(R.drawable.ic_play_arrow_black_36dp));
                    holder.mImageView.setImageTintList(mColorStateNotPlaying);
                    if (mMediaType == MEDIA_ALBUMS_SONGS)
                        holder.mSubtitleView.setVisibility(View.GONE);
                    break;
                case TYPE_PLAYING:
                    AnimationDrawable animation = (AnimationDrawable)
                            mActivity.getDrawable(R.drawable.ic_equalizer_white_36dp);
                    holder.mImageView.setImageDrawable(animation);
                    holder.mImageView.setImageTintList(mColorStatePlaying);
                    animation.start();
                    break;
                case TYPE_PAUSED:
                    holder.mImageView.setImageDrawable(
                            mActivity.getDrawable(R.drawable.ic_equalizer1_white_36dp));
                    holder.mImageView.setImageTintList(mColorStateNotPlaying);
                    break;
            }

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MediaItemViewHolder) {
            MediaItemViewHolder mediaItemViewHolder = (MediaItemViewHolder) holder;

            MediaBrowser.MediaItem item = mMediaItems.get(getMediaItemIndex(position));
            mediaItemViewHolder.mTitleView.setText(item.getDescription().getTitle());
            mediaItemViewHolder.mSubtitleView.setText(item.getDescription().getSubtitle());
        } else if (holder instanceof HeaderHolder) {
            String text;
            if (mMediaItems.get(getMediaItemIndex(position + 1)).isPlayable())
                text = holder.itemView.getContext().getString(R.string.songs);
            else
                text = holder.itemView.getContext().getString(R.string.albums);

            ((TextView) ((HeaderHolder) holder).itemView.findViewById(R.id.text)).setText(text);
        }
    }

    @Override
    public int getItemCount() {
        int mediaItemsCount = mMediaItems.size();
        if (mediaItemsCount == 0)
            return 0;

        int count = mediaItemsCount;
        if (mPlaceholderHeight > 0)
            count++;
        if (mMediaType == MEDIA_ALBUMS_SONGS && !mMediaItems.isEmpty()) {
            count++;
            if (mMediaItems.get(0).isPlayable()
                    != mMediaItems.get(mediaItemsCount - 1).isPlayable())
                count++;
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (mPlaceholderHeight > 0 && position == 0)
            return TYPE_PLACEHOLDER;

        if (mHeadersPositions.contains(position))
            return TYPE_HEADER;

        MediaBrowser.MediaItem item = mMediaItems.get(getMediaItemIndex(position));
        int state = TYPE_NONE;
        if (item.isPlayable()) {
            state = TYPE_PLAYABLE;
            MediaController controller = mActivity.getMediaController();
            if (controller != null && controller.getMetadata() != null) {
                String currentPlaying = controller.getMetadata().getDescription().getMediaId();
                String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                        item.getDescription().getMediaId());
                if (currentPlaying != null && currentPlaying.equals(musicId)) {
                    if (controller.getPlaybackState().getState() ==
                            PlaybackState.STATE_PLAYING) {
                        state = TYPE_PLAYING;
                    } else if (controller.getPlaybackState().getState() !=
                            PlaybackState.STATE_ERROR) {
                        state = TYPE_PAUSED;
                    }
                }
            }
        }
        return state;
    }

    private void initializeColorStateLists(Context ctx) {
        mColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_not_playing));
        mColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_playing));
    }

    private int getMediaItemIndex(int position) {
        int index = position;
        if (mPlaceholderHeight > 0)
            index--;
        if (mMediaType == MEDIA_ALBUMS_SONGS) {
            for (Integer headerPosition : mHeadersPositions)
                if (headerPosition >= position)
                    break;
                else index--;
        }
        return index;
    }

    public RecyclerView.LayoutManager getSuitableLayoutManager(Activity activity) {
        switch (mMediaType) {
            case MEDIA_ALBUM:
                return new GridLayoutManager(activity, activity.getResources().getInteger(R.integer.columns));
            default:
                return new LinearLayoutManager(activity);
        }
    }

    public void add(MediaBrowser.MediaItem mediaItem) {
        int currentSize = mMediaItems.size();
        if (mMediaType == MEDIA_ALBUMS_SONGS) {
            if (mMediaItems.isEmpty() || mMediaItems.get(currentSize - 1).isPlayable()
                    != mediaItem.isPlayable()) {
                if (mPlaceholderHeight > 0)
                    mHeadersPositions.add(currentSize + mHeadersPositions.size() + 1);
                else
                    mHeadersPositions.add(currentSize + mHeadersPositions.size());
            }
            mMediaItems.add(mediaItem);
        } else {
            mMediaItems.add(mediaItem);
        }
    }

    public void clear() {
        mMediaItems.clear();
        mHeadersPositions.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(MediaBrowser.MediaItem mediaItem, View sharedElement);
    }

    protected class PlaceHolder extends RecyclerView.ViewHolder {

        public PlaceHolder(final View itemView) {
            super(itemView);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
            params.height = mPlaceholderHeight;
            itemView.setLayoutParams(params);
        }
    }

    protected class HeaderHolder extends RecyclerView.ViewHolder {

        public HeaderHolder(final View itemView) {
            super(itemView);
        }
    }

    public class MediaItemViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;
        TextView mTitleView;
        TextView mSubtitleView;
        View mOverflowView;

        public MediaItemViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.icon);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mSubtitleView = (TextView) itemView.findViewById(R.id.subtitle);
            mOverflowView = itemView.findViewById(R.id.overflow);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(mMediaItems.get(getMediaItemIndex(getAdapterPosition())), mImageView);
                }
            });

            if (mOverflowView != null)
                mOverflowView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(mActivity, mOverflowView);
                        switch (mMediaType) {
                            case MEDIA_ALBUM:
                                popupMenu.inflate(R.menu.overflow_album);
                                break;
                            case MEDIA_SONG:
                                popupMenu.inflate(R.menu.overflow_song);
                                break;
                            case MEDIA_ALBUMS_SONGS:
                                if (mMediaItems.get(getMediaItemIndex(getAdapterPosition())).isPlayable())
                                    popupMenu.inflate(R.menu.overflow_song);
                                else
                                    popupMenu.inflate(R.menu.overflow_album);
                        }
                        popupMenu.show();
                    }
                });
        }
    }
}
