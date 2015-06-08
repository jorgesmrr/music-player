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
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;

/**
 * An adapter for showing the list of browsed MediaItems
 * Created by Jorge on 07/06/2015.
 */
public class BrowseAdapter extends RecyclerView.Adapter<BrowseAdapter.MediaItemViewHolder> {
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    public static final int MEDIA_GENRE = 0;
    public static final int MEDIA_ARTIST = 1;
    public static final int MEDIA_ALBUM = 2;
    public static final int MEDIA_SONG = 3;

    private static final String TAG = LogHelper.makeLogTag(BrowseAdapter.class);

    private ColorStateList mColorStatePlaying;
    private ColorStateList mColorStateNotPlaying;
    private Activity mActivity;
    private final ArrayList<MediaBrowser.MediaItem> mMediaItems;
    private final OnItemClickListener mListener;
    private final int mMediaType;

    public BrowseAdapter(Activity activity, String mediaId, OnItemClickListener listener) {
        initializeColorStateLists(activity);
        mMediaItems = new ArrayList<>();
        mActivity = activity;
        mListener = listener;
        if (mediaId.equals(MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE))
            mMediaType = MEDIA_GENRE;
        else if (mediaId.equals(MediaIDHelper.MEDIA_ID_ALBUMS_BY_ARTIST))
            mMediaType = MEDIA_ARTIST;
        else if (mediaId.equals(MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM) || mediaId.startsWith(MediaIDHelper.MEDIA_ID_ALBUMS_BY_ARTIST))
            mMediaType = MEDIA_ALBUM;
        else
            mMediaType = MEDIA_SONG;
    }

    @Override
    public MediaItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout;
        switch (mMediaType) {
            case MEDIA_ARTIST:
            case MEDIA_ALBUM:
                layout = R.layout.media_grid_item;
                break;
            case MEDIA_GENRE:
            default:
                layout = R.layout.media_list_item;
                break;
        }

        View itemView = LayoutInflater.from(mActivity)
                .inflate(layout, parent, false);
        MediaItemViewHolder holder = new MediaItemViewHolder(itemView);

        switch (viewType) {
            case STATE_NONE:
                switch(mMediaType){
                    case MEDIA_GENRE:
                        holder.mImageView.setImageResource(R.drawable.ic_by_genre);
                        break;
                    case MEDIA_ARTIST:
                    case MEDIA_ALBUM:
                        holder.mImageView.setImageResource(R.drawable.placeholder);
                }
                holder.mImageView.setVisibility(View.VISIBLE);
                break;
            case STATE_PLAYABLE:
                holder.mImageView.setImageDrawable(
                        mActivity.getDrawable(R.drawable.ic_play_arrow_black_36dp));
                holder.mImageView.setImageTintList(mColorStateNotPlaying);
                holder.mImageView.setVisibility(View.VISIBLE);
                break;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        mActivity.getDrawable(R.drawable.ic_equalizer_white_36dp);
                holder.mImageView.setImageDrawable(animation);
                holder.mImageView.setImageTintList(mColorStatePlaying);
                holder.mImageView.setVisibility(View.VISIBLE);
                animation.start();
                break;
            case STATE_PAUSED:
                holder.mImageView.setImageDrawable(
                        mActivity.getDrawable(R.drawable.ic_equalizer1_white_36dp));
                holder.mImageView.setImageTintList(mColorStateNotPlaying);
                holder.mImageView.setVisibility(View.VISIBLE);
                break;
            default:
                holder.mImageView.setVisibility(View.GONE);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(MediaItemViewHolder holder, int position) {
        MediaBrowser.MediaItem item = mMediaItems.get(position);
        holder.mTitleView.setText(item.getDescription().getTitle());
        holder.mDescriptionView.setText(item.getDescription().getSubtitle());
    }

    @Override
    public int getItemCount() {
        return mMediaItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        MediaBrowser.MediaItem item = mMediaItems.get(position);
        int state = STATE_NONE;
        if (item.isPlayable()) {
            state = STATE_PLAYABLE;
            MediaController controller = mActivity.getMediaController();
            if (controller != null && controller.getMetadata() != null) {
                String currentPlaying = controller.getMetadata().getDescription().getMediaId();
                String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                        item.getDescription().getMediaId());
                if (currentPlaying != null && currentPlaying.equals(musicId)) {
                    if (controller.getPlaybackState().getState() ==
                            PlaybackState.STATE_PLAYING) {
                        state = STATE_PLAYING;
                    } else if (controller.getPlaybackState().getState() !=
                            PlaybackState.STATE_ERROR) {
                        state = STATE_PAUSED;
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

    public RecyclerView.LayoutManager getSuitableLayoutManager(Activity activity) {
        switch (mMediaType) {
            case MEDIA_ARTIST:
            case MEDIA_ALBUM:
                return new GridLayoutManager(activity, activity.getResources().getInteger(R.integer.columns));
            case MEDIA_GENRE:
            default:
                return new LinearLayoutManager(activity);
        }
    }

    public void add(MediaBrowser.MediaItem mediaItem) {
        mMediaItems.add(mediaItem);
        notifyItemInserted(mMediaItems.size() - 1);
    }

    public void clear() {
        mMediaItems.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(MediaBrowser.MediaItem mediaItem, View sharedElement);
    }

    public class MediaItemViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;
        TextView mTitleView;
        TextView mDescriptionView;

        public MediaItemViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.play_eq);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mDescriptionView = (TextView) itemView.findViewById(R.id.description);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(mMediaItems.get(getAdapterPosition()), mImageView);
                }
            });
        }
    }
}
