/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class to get a list of MusicTrack's based on MediaStore queries.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadata>> mMusicListByAlbum; // <albumId, music>
    private ConcurrentMap<String, List<MediaMetadata>> mMusicListByArtist; // <artistId, music>
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById; // <musicId, music>
    private ConcurrentMap<String, List<String>> mAlbumListByArtist; // <artistId, albumId>
    private ConcurrentMap<String, Album> mAlbumListById; // <albumId, album>

    private final Set<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        mMusicListByAlbum = new ConcurrentHashMap<>();
        mAlbumListByArtist = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mAlbumListById = new ConcurrentHashMap<>();
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    public Album getAlbum(String id) {
        return mAlbumListById.get(id);
    }

    /**
     * Get all music tracks
     */
    public Iterable<MediaMetadata> getMusics() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadata> musics = new ArrayList<>();
        for (MutableMediaMetadata m : mMusicListById.values())
            musics.add(m.metadata);
        return musics;
    }

    /**
     * Get an iterator over the list of albums
     *
     * @return genres
     */
    public Iterable<String> getAlbums() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.keySet();
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return genres
     */
    public Iterable<String> getArtists() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mAlbumListByArtist.keySet();
    }

    /**
     * Get music tracks of the given album
     */
    public List<MediaMetadata> getMusicsByAlbum(String album) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByAlbum.containsKey(album)) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.get(album);
    }

    /**
     * Get music tracks of the given artist
     */
    public List<MediaMetadata> getMusicsByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByArtist.containsKey(artist)) {
            return Collections.emptyList();
        }
        return mMusicListByArtist.get(artist);
    }

    /**
     * Get albums of the given artist
     */
    public List<String> getAlbumsByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED || !mAlbumListByArtist.containsKey(artist)) {
            return Collections.emptyList();
        }
        return mAlbumListByArtist.get(artist);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     */
    public Iterable<MediaMetadata> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     */
    public Iterable<MediaMetadata> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     */
    public Iterable<MediaMetadata> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadata> searchMusic(String metadataField, String query) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadata> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mMusicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                    .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }


    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadata getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusic(Context context, String musicId, MediaMetadata metadata) {
        MutableMediaMetadata track = mMusicListById.get(musicId);
        if (track == null) {
            return;
        }

        String oldGenre = track.metadata.getString(MediaMetadata.METADATA_KEY_GENRE);
        String newGenre = metadata.getString(MediaMetadata.METADATA_KEY_GENRE);

        track.metadata = metadata;

        /* todo precisa atualizar as listas?
        // if genre has changed, we need to rebuild the list by genre
        if (!oldGenre.equals(newGenre)) {
            buildListsByGenre(context.getContentResolver());
        }*/
    }

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Context context, final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            // Nothing to do, execute callback immediately
            callback.onMusicCatalogReady(true);
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia(context);
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void retrieveMedia(Context context) {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                ContentResolver contentResolver = context.getContentResolver();
                Cursor musicCursor = contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.IS_MUSIC + " != ?",
                        new String[]{"0"},
                        null);

                if (musicCursor.moveToFirst()) {
                    int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int sourceColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                    int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int trackNumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

                    ConcurrentHashMap<String, List<MediaMetadata>> newMusicListByArtist = new ConcurrentHashMap<>();
                    ConcurrentHashMap<String, List<MediaMetadata>> newMusicListByAlbum = new ConcurrentHashMap<>();
                    ConcurrentHashMap<String, List<String>> newAlbumListByArtist = new ConcurrentHashMap<>();

                    do {
                        String musicId = musicCursor.getString(idColumn);
                        String artist = musicCursor.getString(artistColumn);
                        String album = musicCursor.getString(albumColumn);
                        String albumId = musicCursor.getString(albumIdColumn);

                        MediaMetadata item = new MediaMetadata.Builder()
                                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, musicId)
                                .putString(CUSTOM_METADATA_TRACK_SOURCE, musicCursor.getString(sourceColumn))
                                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                                .putLong(MediaMetadata.METADATA_KEY_DURATION, musicCursor.getLong(durationColumn))
                                        //todo .putString(MediaMetadata.METADATA_KEY_GENRE, "genero teste")
                                        //todo .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cursor.getString(idColumn))
                                .putString(MediaMetadata.METADATA_KEY_TITLE, musicCursor.getString(titleColumn))
                                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, musicCursor.getInt(trackNumColumn))
                                        //todo .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, 2)
                                .build();
                        mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));

                        // Add this song to the respective album
                        List<MediaMetadata> songsListByArtist = newMusicListByArtist.get(artist);
                        if (songsListByArtist == null) {
                            songsListByArtist = new ArrayList<>();
                            newMusicListByArtist.put(artist, songsListByArtist);
                        }
                        songsListByArtist.add(item);

                        // Add this song to the respective album
                        List<MediaMetadata> songsListByAlbum = newMusicListByAlbum.get(albumId);
                        if (songsListByAlbum == null) {
                            songsListByAlbum = new ArrayList<>();
                            newMusicListByAlbum.put(albumId, songsListByAlbum);
                        }
                        songsListByAlbum.add(item);

                        if (!mAlbumListById.containsKey(albumId))
                            mAlbumListById.put(albumId, new Album(album));

                        // Add this song's album to the respective artist
                        List<String> albumsList = newAlbumListByArtist.get(artist);
                        if (albumsList == null) {
                            albumsList = new ArrayList<>();
                            newAlbumListByArtist.put(artist, albumsList);
                        }
                        albumsList.add(albumId);

                    } while (musicCursor.moveToNext());

                    // Update cache lists
                    mMusicListByArtist = newMusicListByArtist;
                    mMusicListByAlbum = newMusicListByAlbum;
                    mAlbumListByArtist = newAlbumListByArtist;
                }
                musicCursor.close();

                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }
}
