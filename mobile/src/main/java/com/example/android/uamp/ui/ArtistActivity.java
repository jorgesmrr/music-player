package com.example.android.uamp.ui;

import android.os.Bundle;

import com.example.android.uamp.R;

/**
 * Created by Jorge Augusto da Silva Moreira on 12/06/2015.
 */
public class ArtistActivity extends MediaContainerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        initializeViews();
        initializeFromParams(savedInstanceState, getIntent());
    }

    protected void openFragment(String mediaId){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, MediaBrowserFragment.newInstance(mediaId), BROWSE_FRAG_TAG)
                .commit();
    }
}
