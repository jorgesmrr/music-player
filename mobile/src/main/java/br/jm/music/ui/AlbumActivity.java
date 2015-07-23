package br.jm.music.ui;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
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

import br.jm.music.MusicApplication;
import br.jm.music.R;
import br.jm.music.model.MusicProvider;
import br.jm.music.utils.BitmapHelper;

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
    private View mHeaderBar;

    private int mHeaderHeight;
    private int mActionBarHeight;
    private boolean mIsPortrait;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        initializeViews();

        mIsPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        // Color to fill the gap between status bar and title bar
        mIsHeaderFillShown = false;

        mMinHeaderTranslation = getStatusBarHeight();

        // Find views
        mHeaderView = findViewById(R.id.header);
        mHeaderBar = findViewById(R.id.bar);

        mTopShadow = findViewById(R.id.shadow);

        if (mIsPortrait) {
            mHeaderFill = mHeaderView.findViewById(R.id.header_fill);
            mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.art);

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

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTopShadow.getLayoutParams();
            params.height += getStatusBarHeight();
            mTopShadow.setLayoutParams(params);

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
        } else initializeFromParams(savedInstanceState, getIntent());
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
                .add(R.id.container, MediaBrowserFragment.newInstance(mediaId, mIsPortrait ? mOnScrollListener : null, mHeaderHeight), BROWSE_FRAG_TAG)
                .commit();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

        private int getScrollY(RecyclerView recyclerView) throws IllegalStateException {
            View c = recyclerView.getChildAt(0);

            //If list is empty
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
            final Bitmap art = BitmapFactory.decodeFile(iconUri.toString());
            if (art != null)
                new Palette.Builder(art).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int vibrantColor = palette.getVibrantColor(getResources().getColor(R.color.default_vibrant_color));
                        int darkVibrantColor = palette.getDarkVibrantColor(getResources().getColor(R.color.default_dark_vibrant_color));
                        mHeaderBar.setBackgroundColor(darkVibrantColor);
                        ColorStateList stateList = new ColorStateList(new int[][]{new int[]{}}, new int[]{vibrantColor});
                        mFab.setBackgroundTintList(stateList);
                        if (mIsPortrait) {
                            mHeaderImageView.setImageBitmap(art);
                            mHeaderFill.setBackgroundColor(darkVibrantColor);
                        } else {
                            getToolbar().setBackgroundColor(darkVibrantColor);
                        }
                    }
                });
            else if (mIsPortrait) mHeaderImageView.setImageBitmap(
                    BitmapHelper.getDefault(getResources(), MusicApplication.DEF_ART_SIZE_NORMAL));
        } else if (mIsPortrait) {
            mHeaderImageView.setImageBitmap(
                    BitmapHelper.getDefault(getResources(), MusicApplication.DEF_ART_SIZE_NORMAL));
        }
    }
}
