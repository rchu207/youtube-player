package tw.idv.rchu.youtubeplayer;

import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YouTubeDataWrapper {
    static final String TAG = "DataWrapper";

    public static final int MAX_RESULT_COUNT = 20;

    private YouTube mYoutube;

    public void init(String appName) {
        if (mYoutube == null) {
            mYoutube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName(appName).build();
        }
    }

    /**
     * YouTube Data API v3 implementations.
     */

    public List<Video> query(String id) throws IOException {
        YouTube.Videos.List query = mYoutube.videos().list("id,snippet,status");
        query.setKey(Auth.BROSWER_KEY);
        query.setId(id);
        query.setFields("items(id,snippet(title,description,thumbnails/medium/url),status)");

        VideoListResponse queryResponse = query.execute();
        for (Video video : queryResponse.getItems()) {
            Log.i(TAG, "id:" + video.getId());
            Log.i(TAG, "snippet:" + video.getSnippet());
            Log.i(TAG, "status:" + video.getStatus());
        }

        return queryResponse.getItems();
    }

    public int queryListCount(String id) throws IOException {
        YouTube.Playlists.List query = mYoutube.playlists().list("id,contentDetails");
        query.setKey(Auth.BROSWER_KEY);
        query.setId(id);

        PlaylistListResponse response = query.execute();
        if (response.getItems().size() > 0) {
            Playlist playlist = response.getItems().get(0);
            Log.i(TAG, "id:" + playlist.getId());
            Log.i(TAG, "contentDetails:" + playlist.getContentDetails());

            return playlist.getContentDetails().getItemCount().intValue();
        } else {
            return 0;
        }
    }

    public int queryListItemPosition(String id, String videoId) throws IOException {
        YouTube.PlaylistItems.List query = mYoutube.playlistItems().list("id,snippet");
        query.setKey(Auth.BROSWER_KEY);
        query.setPlaylistId(id);
        query.setVideoId(videoId);
        query.setMaxResults(50L);  // only can search first 50 items in the playlist.

        PlaylistItemListResponse response = query.execute();
        if (response.getItems().size() > 0) {
            PlaylistItem playlistItem = response.getItems().get(0);
            Log.i(TAG, "id:" + playlistItem.getId());
            Log.i(TAG, "snipper:" + playlistItem.getSnippet());

            return playlistItem.getSnippet().getPosition().intValue();
        } else {
            return -1;
        }
    }

    public String queryListItems(String id, String token, ArrayList<Video> videoList) throws IOException {
        YouTube.PlaylistItems.List query = mYoutube.playlistItems().list("id,snippet,status");
        query.setKey(Auth.BROSWER_KEY);
        query.setPlaylistId(id);
        query.setMaxResults((long) MAX_RESULT_COUNT);
        query.setFields("nextPageToken,items(id,snippet(title,description,thumbnails/medium/url,resourceId/videoId),status)");
        query.setPageToken(token);

        PlaylistItemListResponse response = query.execute();
        for (PlaylistItem playlistItem : response.getItems()) {
            Log.i(TAG, "id:" + playlistItem.getId());
            Log.i(TAG, "snippet:" + playlistItem.getSnippet());
            Log.i(TAG, "status:" + playlistItem.getStatus());

            Video video = new Video();
            video.setId(playlistItem.getSnippet().getResourceId().getVideoId());
            video.setSnippet(new VideoSnippet());
            video.getSnippet().setTitle(playlistItem.getSnippet().getTitle());
            video.getSnippet().setDescription(playlistItem.getSnippet().getDescription());
            video.getSnippet().setThumbnails(playlistItem.getSnippet().getThumbnails());
            videoList.add(video);
        }

        return response.getNextPageToken();
    }
}
