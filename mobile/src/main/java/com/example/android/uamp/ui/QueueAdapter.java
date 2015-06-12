package com.example.android.uamp.ui;

import android.graphics.Typeface;
import android.media.session.MediaSession;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.R;

import java.util.List;

/**
 * Created by Jorge on 11/06/2015.
 */
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueItemHolder> {

    private static final int TYPE_CURRENT = 0;
    private static final int TYPE_NORMAL = 1;

    private List<MediaSession.QueueItem> mQueue;
    private final OnItemClickListener mOnItemClickListener;
    private int mCurrentindex;

    public QueueAdapter(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public QueueItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_two_lines_overflow, parent, false);
        QueueItemHolder holder = new QueueItemHolder(itemView);

        if (viewType == TYPE_CURRENT) {
            holder.mTitleView.setTypeface(null, Typeface.BOLD);
            holder.mSubtitleView.setTypeface(null, Typeface.BOLD);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(QueueItemHolder holder, int position) {
        MediaSession.QueueItem queueItem = mQueue.get(position);

        holder.mTitleView.setText(queueItem.getDescription().getTitle());
        holder.mSubtitleView.setText(queueItem.getDescription().getSubtitle());
    }

    @Override
    public int getItemCount() {
        return mQueue != null ? mQueue.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mCurrentindex)
            return TYPE_CURRENT;
        else
            return TYPE_NORMAL;
    }

    public void setQueue(List<MediaSession.QueueItem> queue) {
        mQueue = queue;
        notifyDataSetChanged();
    }

    public Iterable<MediaSession.QueueItem> getQueue() {
        return mQueue;
    }

    public void setCurrentIndex(int currentIndex) {
        this.mCurrentindex = currentIndex;
        notifyItemChanged(currentIndex);
    }

    protected class QueueItemHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        TextView mTitleView;
        TextView mSubtitleView;
        View mOverflowView;

        public QueueItemHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.icon);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mSubtitleView = (TextView) itemView.findViewById(R.id.subtitle);
            mOverflowView = itemView.findViewById(R.id.overflow);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(mQueue.get(getAdapterPosition()));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(MediaSession.QueueItem queueItem);
    }
}
