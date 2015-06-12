package com.example.android.uamp.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

/**
 * Displays a fragment with the music tracks for the given media ID
 * Created by Jorge on 07/06/2015.
 */
public class MediaContainerActivity extends BaseActivity {

    private static final String TAG = LogHelper.makeLogTag(MediaContainerActivity.class);
    public static final String SAVED_MEDIA_ID = "media_id";
    public static final String BROWSE_FRAG_TAG = "browse_frag";

    private String mMediaId;
    private TextView mTitleView;

    private int mHeaderHeight;
    private int mActionBarHeight;

    // Header stuff
    private int mMinHeaderTranslation;
    private boolean mIsHeaderFillShown;
    private ImageView mHeaderImageView;
    private View mHeaderView;
    private View mHeaderFill;
    private View mTopShadow;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mMediaId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        // Color to fill the gap between status bar and title bar
        mIsHeaderFillShown = false;

        initializeToolbar(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        mMinHeaderTranslation = getStatusBarHeight();

        // Find views
        mHeaderView = findViewById(R.id.header);
        mHeaderFill = mHeaderView.findViewById(R.id.header_fill);
        mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.art);
        final View bar = findViewById(R.id.bar);
        mTitleView = (TextView) bar.findViewById(R.id.title);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mMediaId.startsWith(MediaIDHelper.MEDIA_ID_BY_ALBUM))
            getMenuInflater().inflate(R.menu.album, menu);
        return true;
    }

    @Override
    protected void onMediaControllerConnected() {
        getBrowseFragment().onConnected();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getSupportFragmentManager().findFragmentByTag(BROWSE_FRAG_TAG);
    }

    private String getMediaId() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            return extras.getString(SAVED_MEDIA_ID);
        }
        return null;
    }

    @Override
    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        if (savedInstanceState != null) {
            // If there is a saved media ID, use it
            mMediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
        } else {
            mMediaId = getMediaId();
        }
        navigateToBrowser(mMediaId, null);
    }

    @Override
    protected void navigateToBrowser(String mediaId, View sharedElement) {
        if (mediaId.equals(mMediaId))
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MediaBrowserFragment.newInstance(mediaId, mOnScrollListener, mHeaderHeight), BROWSE_FRAG_TAG)
                    .commit();
        else {
            Intent intent = new Intent(this, MediaContainerActivity.class).putExtra(MediaContainerActivity.SAVED_MEDIA_ID, mediaId);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, sharedElement, "image");
            startActivity(intent, options.toBundle());
        }
    }

    @Override
    public void setMediaTitle(CharSequence title) {
        mTitleView.setText(title);
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
            mHeaderImageView.setTranslationY(-translationY + translationY / 3);

            if (mHeaderFill != null) {
                if ((int) translationY <= mMinHeaderTranslation + mActionBarHeight / 2) {
                    if (!mIsHeaderFillShown) {
                        mHeaderFill.setVisibility(View.VISIBLE);
                        mHeaderFill.startAnimation(AnimationUtils.loadAnimation(MediaContainerActivity.this, R.anim.show_header_bar));
                        mIsHeaderFillShown = true;
                    }
                } else if (mIsHeaderFillShown) {
                    mHeaderFill.setVisibility(View.INVISIBLE);
                    mHeaderFill.startAnimation(AnimationUtils.loadAnimation(MediaContainerActivity.this, R.anim.hide_header_bar));
                    mIsHeaderFillShown = false;
                }
            }
        }
    };
}
