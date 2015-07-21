package com.example.android.uamp.ui;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.android.uamp.R;
import com.example.android.uamp.model.MusicProvider;

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
    private View mHeaderBar;

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

        mHeaderBar = findViewById(R.id.bar);
        mHeaderBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mHeaderBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mMinHeaderTranslation += mHeaderBar.getHeight();
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

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTopShadow.getLayoutParams();
        params.height += getStatusBarHeight();
        mTopShadow.setLayoutParams(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.album, menu);
        return true;
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

    @Override
    public void setMediaDescription(MediaDescription description) {
        mTitleView.setText(description.getTitle());
        mSubtitleView.setText(description.getExtras().getString(MusicProvider.ALBUM_EXTRA_ARTIST));
        Uri iconUri = description.getIconUri();
        if (iconUri != null) {
            Bitmap art = BitmapFactory.decodeFile(iconUri.toString());
            mHeaderImageView.setImageBitmap(art);
            new Palette.Builder(art).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int darkVibrantColor = palette.getDarkVibrantColor(getResources().getColor(R.color.default_dark_vibrant_color));
                    mHeaderBar.setBackgroundColor(darkVibrantColor);
                    mHeaderFill.setBackgroundColor(darkVibrantColor);
                    ColorStateList stateList = new ColorStateList(new int[][]{new int[]{}}, new int[]{palette.getVibrantColor(getResources().getColor(R.color.default_vibrant_color))});
                    mFab.setBackgroundTintList(stateList);
                }
            });
        } else {
            //todo mHeaderImageView.setImageResource(R.drawable.placeholder);
        }
    }
}
