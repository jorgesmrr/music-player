package br.jm.music.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.media.session.MediaSession;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import br.jm.music.R;

import java.util.Collections;
import java.util.List;

/**
 * Created by Jorge on 11/06/2015.
 */
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueItemHolder> implements ItemTouchHelperAdapter {

    private final int mDraggingElevation;
    private List<MediaSession.QueueItem> mQueue;
    private final Listener mListener;
    private final OnStartDragListener mDragStartListener;
    private int mCurrentIndex;

    /**
     * Listener for manual initiation of a drag.
     */
    public interface OnStartDragListener {

        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public QueueAdapter(Context context, OnStartDragListener dragStartListener, Listener listener) {
        mDragStartListener = dragStartListener;
        mListener = listener;
        mDraggingElevation = context.getResources().getDimensionPixelSize(R.dimen.elevation_dragging);
    }

    @Override
    public QueueItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_queue, parent, false);
        return new QueueItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final QueueItemHolder holder, int position) {
        MediaSession.QueueItem queueItem = mQueue.get(position);

        holder.mTitleView.setText(queueItem.getDescription().getTitle());
        holder.mSubtitleView.setText(queueItem.getDescription().getSubtitle());

        if (position == mCurrentIndex) {
            holder.mTitleView.setTypeface(null, Typeface.BOLD);
            holder.mSubtitleView.setTypeface(null, Typeface.BOLD);
        } else {
            holder.mTitleView.setTypeface(null, Typeface.NORMAL);
            holder.mSubtitleView.setTypeface(null, Typeface.NORMAL);
        }

        // Start a drag whenever the handle view it touched
        holder.mHandleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mQueue != null ? mQueue.size() : 0;
    }

    public void setQueue(List<MediaSession.QueueItem> queue) {
        mQueue = queue;
        notifyDataSetChanged();
    }

    public List<MediaSession.QueueItem> getQueue() {
        return mQueue;
    }

    public void setCurrentIndex(int currentIndex) {
        int aux = mCurrentIndex;
        this.mCurrentIndex = currentIndex;
        notifyItemChanged(aux);
        notifyItemChanged(currentIndex);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mQueue, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        mQueue.remove(position);
        notifyItemRemoved(position);
        mListener.onItemDismiss(position);
    }

    protected class QueueItemHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {
        ImageView mImageView;
        TextView mTitleView;
        TextView mSubtitleView;
        View mOverflowView, mHandleView;

        public QueueItemHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.icon);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mSubtitleView = (TextView) itemView.findViewById(R.id.subtitle);
            mOverflowView = itemView.findViewById(R.id.overflow);
            mHandleView = itemView.findViewById(R.id.handle);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(mQueue.get(getAdapterPosition()));
                }
            });

            mOverflowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(mOverflowView.getContext(), mOverflowView);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mListener.onMenuItemClick(item, getAdapterPosition());
                            return true;
                        }
                    });
                    popupMenu.inflate(R.menu.overflow_song_queue);
                    popupMenu.show();
                }
            });
        }

        @Override
        public void onItemSelected() {
            itemView.setElevation(mDraggingElevation);
        }

        @Override
        public void onItemClear() {
            itemView.setElevation(0);
        }
    }

    public interface Listener {
        void onItemClick(MediaSession.QueueItem queueItem);

        void onMenuItemClick(MenuItem item, int position);

        void onItemDismiss(int position);
    }
}
