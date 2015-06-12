package com.example.android.uamp.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.android.uamp.R;

/**
 * Created by Jorge Augusto da Silva Moreira on 12/06/2015.
 */
public class AlbumActivity extends MediaContainerActivity {

    private int mMinHeaderTranslation;
    private boolean mIsHeaderFillShown;
    private ImageView mHeaderImageView;
    private View mHeaderView;
    private View mHeaderFill;
    private View mTopShadow;

    private int mHeaderHeight;
    private int mActionBarHeight;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        initializeViews();

        // Color to fill the gap between status bar and title bar
        mIsHeaderFillShown = false;

        mMinHeaderTranslation = getStatusBarHeight();

        // Find views
        mHeaderView = findViewById(R.id.header);
        mHeaderFill = mHeaderView.findViewById(R.id.header_fill);
        mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.art);

        mTopShadow = findViewById(R.id.shadow);
        mMinHeaderTranslation -= getResources().getDimensionPixelSize(R.dimen.margin_screen);
        if (mIsHeaderFillShown)
            mHeaderFill.setVisibility(View.VISIBLE);
        else
            mHeaderFill.setVisibility(View.INVISIBLE);

        TypedValue mTypedValue = new TypedValue();
        getTheme()
                .resolveAttribute(android.R.attr.actionBarSize, mTypedValue, true);
        mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                mTypedValue.data, getResources().getDisplayMetrics());
        mMinHeaderTranslation += mActionBarHeight;

        final View bar = findViewById(R.id.bar);
        bar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mMinHeaderTranslation += bar.getHeight();
            }
        });

        mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mHeaderView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mHeaderHeight = mHeaderView.getHeight();
                mMinHeaderTranslation -= mHeaderHeight;

                initializeFromParams(savedInstanceState, getIntent());
            }
        });
    }

    @Override
    protected void openFragment(String mediaId) {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, MediaBrowserFragment.newInstance(mediaId, mOnScrollListener, mHeaderHeight), BROWSE_FRAG_TAG)
                .commit();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

        private int getScrollY(RecyclerView recyclerView) throws IllegalStateException {
            View c = recyclerView.getChildAt(0);

            //Se a lista estiver vazia
            if (c == null)
                return 0;

            int firstVisiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

            int top = c.getTop();

            int headerHeight = 0;
            if (firstVisiblePosition >= 1)
                headerHeight = mHeaderHeight;

            //Quando o mHeader ainda nao saiu da tela, apenas move a altura do primeiro item
            // menos a distancia entre ele e o topo. Quando o mHeader some, move so a sua altura
            return firstVisiblePosition * c.getHeight() - top + headerHeight;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mMinHeaderTranslation > 0)
                return;

            float translationY = Math.max(-getScrollY(recyclerView), mMinHeaderTranslation);

            mHeaderView.setTranslationY(translationY);
            mTopShadow.setTranslationY(-translationY);
            mFab.setTranslationY(translationY);
            mHeaderImageView.setTranslationY(-translationY + translationY / 3);

            if (mHeaderFill != null) {
                if ((int) translationY <= mMinHeaderTranslation + mActionBarHeight / 2) {
                    if (!mIsHeaderFillShown) {
                        mHeaderFill.setVisibility(View.VISIBLE);
                        mHeaderFill.startAnimation(AnimationUtils.loadAnimation(AlbumActivity.this, R.anim.show_header_bar));
                        mIsHeaderFillShown = true;
                    }
                } else if (mIsHeaderFillShown) {
                    mHeaderFill.setVisibility(View.INVISIBLE);
                    mHeaderFill.startAnimation(AnimationUtils.loadAnimation(AlbumActivity.this, R.anim.hide_header_bar));
                    mIsHeaderFillShown = false;
                }
            }
        }
    };
}
