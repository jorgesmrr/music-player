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

package br.jm.music.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import br.jm.music.utils.LogHelper;

/**
 * Utility class to get a list of MusicTrack's based on MediaStore queries.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    public static final String ALBUM_EXTRA_ARTIST = "artist";

    // Categorized caches for music track data:
    private ConcurrentMap<Integer, List<MediaMetadata>> mMusicListByAlbum; // <albumId, music>
    private ConcurrentMap<String, List<MediaMetadata>> mMusicListByArtist; // <artistId, music>
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById; // <musicId, music>
    private ConcurrentMap<String, List<Integer>> mAlbumListByArtist; // <artistId, albumId>
    private ConcurrentMap<Integer, Album> mAlbumListById; // <albumId, album>

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
    }

    public Album getAlbum(int id) {
        return mAlbumListById.get(id);
    }

    /**
     * Get all music tracks
     */
    public List<MediaMetadata> getMusics() {
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
    public Iterable<Integer> getAlbums() {
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
    public List<MediaMetadata> getMusicsByAlbum(int album) {
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
    public List<Integer> getAlbumsByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED || !mAlbumListByArtist.containsKey(artist)) {
            return Collections.emptyList();
        }
        return mAlbumListByArtist.get(artist);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     */
    public List<MediaMetadata> searchMusicBySongTitle(String query) {
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

    List<MediaMetadata> searchMusic(String metadataField, String query) {
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

    public int getAlbumIdFromMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).albumId : -1;
    }

    public synchronized void updateMusic(Context context, String musicId, MediaMetadata metadata) {
        MutableMediaMetadata track = mMusicListById.get(musicId);
        if (track == null) {
            return;
        }

        track.metadata = metadata;
    }

    public synchronized void delete(String musicId, ContentResolver contentResolver) {
        //todo remover dos álbuns, artistas e playlists
        contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media._ID + " =" + musicId + "", null);
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final ContentResolver contentResolver, final Callback callback) {
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
                retrieveMedia(contentResolver);
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

    private synchronized void buildAlbumListById(ContentResolver contentResolver) {
        mAlbumListById.clear();

        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int artColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            do {
                int id = cursor.getInt(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String artwork = cursor.getString(artColumn);
                mAlbumListById.put(id, new Album(id, title, artist, artwork));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private synchronized void retrieveMedia(ContentResolver contentResolver) {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                mMusicListById.clear();

                // Retrieve albums
                buildAlbumListById(contentResolver);

                Cursor cursor = contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.IS_MUSIC + " != ?",
                        new String[]{"0"},
                        null);

                if (cursor.moveToFirst()) {
                    int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int sourceColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                    int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

                    ConcurrentHashMap<String, List<MediaMetadata>> newMusicListByArtist = new ConcurrentHashMap<>();
                    ConcurrentHashMap<Integer, List<MediaMetadata>> newMusicListByAlbum = new ConcurrentHashMap<>();
                    ConcurrentHashMap<String, List<Integer>> newAlbumListByArtist = new ConcurrentHashMap<>();

                    do {
                        String musicId = cursor.getString(idColumn);
                        String artist = cursor.getString(artistColumn);
                        String album = cursor.getString(albumColumn);
                        int albumId = cursor.getInt(albumIdColumn);

                        MediaMetadata.Builder itemBuilder = new MediaMetadata.Builder()
                                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, musicId)
                                .putString(CUSTOM_METADATA_TRACK_SOURCE, cursor.getString(sourceColumn))
                                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                                .putLong(MediaMetadata.METADATA_KEY_DURATION, cursor.getLong(durationColumn))
                                .putString(MediaMetadata.METADATA_KEY_TITLE, cursor.getString(titleColumn))
                                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, cursor.getInt(trackNumColumn));

                        Album albumA = mAlbumListById.get(albumId);
                        if (albumA != null)
                            itemBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, albumA.getArtwork());

                        MediaMetadata item = itemBuilder.build();
                        mMusicListById.put(musicId, new MutableMediaMetadata(musicId, albumId, item));

                        // Add this song to the respective artist
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

                        // Add this song's album to the respective artist
                        List<Integer> albumsList = newAlbumListByArtist.get(artist);
                        if (albumsList == null) {
                            albumsList = new ArrayList<>();
                            newAlbumListByArtist.put(artist, albumsList);
                        }
                        if (!albumsList.contains(albumId))
                            albumsList.add(albumId);

                    } while (cursor.moveToNext());

                    // Update cache lists
                    mMusicListByArtist = newMusicListByArtist;
                    mMusicListByAlbum = newMusicListByAlbum;
                    mAlbumListByArtist = newAlbumListByArtist;
                }
                cursor.close();

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
