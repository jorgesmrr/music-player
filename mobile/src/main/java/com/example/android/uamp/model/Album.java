package com.example.android.uamp.model;

/**
 * Created by Jorge on 06/06/2015.
 */
public class Album {
    private final int id;
    private final String title;
    private final String artist;
    private final String artwork;

    public Album(int id, String title, String artist, String artwork) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.artwork = artwork;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getArtwork() {
        return artwork;
    }
}
