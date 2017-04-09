package tw.idv.rchu.youtubeplayer;

import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.google.android.youtube.player.YouTubePlayer;

import java.util.List;

public class YouTubeWebPlayer implements YouTubePlayer {
    static final String TAG = "WebPlayer";

    static final int PLAYER_STATE_UNSTARTED = -1;
    static final int PLAYER_STATE_ENDED = 0;
    static final int PLAYER_STATE_PLAYING = 1;
    static final int PLAYER_STATE_PAUSED = 2;
    //static final int PLAYER_STATE_BUFFERING = 3;
    //static final int PLAYER_STATE_CUED = 5;

    private WebView mWebView;
    private Handler mUiHandler;
    private int mCurrentTime;
    private int mDuration;
    private int mPlayerState;

    private boolean mIsVideoLoaded;
    private boolean mIsPlaylistLoaded;
    private int mPlaylistIndex;
    private int mPlaylistCount;

    private PlaylistEventListener mPlaylistEventListener;
    private PlayerStateChangeListener mPlayerStateChangeListener;
    private PlaybackEventListener mPlaybackEventListener;

    public YouTubeWebPlayer(WebView webView) {
        mWebView = webView;
        mUiHandler = new Handler();
    }

    public void setCurrentTime(float seconds) {
        mCurrentTime = (int)(seconds * 1000);
    }

    public void setDuration(int ms) {
        mDuration = ms;
    }

    @JavascriptInterface
    public void notifyStateChanged(String data) {
        Log.i(TAG, "notifyStateChanged:" + data);
        int state = Integer.parseInt(data);

        // Retrieve playlist information.
        if (!mIsPlaylistLoaded && state == PLAYER_STATE_PLAYING) {
            // First video is loaded, retrieve playlist information.
            mIsPlaylistLoaded = true;

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.evaluateJavascript("player.getPlaylist();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (s.equals("null")) {
                                mPlaylistCount = 0;
                            } else {
                                mPlaylistCount = s.split(",").length;
                            }
                        }
                    });
                }
            });
        }

        // Retrieve video information.
        if (state == PLAYER_STATE_UNSTARTED) {
            mIsVideoLoaded = false;
        } else if (!mIsVideoLoaded && state == PLAYER_STATE_PLAYING) {
            // Video is loaded, get duration and index of playlist.
            mIsVideoLoaded = true;

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.evaluateJavascript("player.getDuration();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            int index = s.indexOf('.');
                            setDuration(Integer.parseInt(s.substring(0, index))
                                    + Integer.parseInt(s.substring(index+1)));
                        }
                    });

                    mWebView.evaluateJavascript("player.getPlaylistIndex();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            int index = Integer.parseInt(s);
                            if (mPlaylistIndex != -1 && index != -1) {
                                if (mPlaylistEventListener != null) {
                                    if (index > mPlaylistIndex) {
                                        mPlaylistEventListener.onNext();
                                    } else if (index < mPlaylistIndex) {
                                        mPlaylistEventListener.onPrevious();
                                    }
                                }
                            }
                            mPlaylistIndex = index;
                        }
                    });
                }
            });
        }

        mPlayerState = state;

        if (mPlaybackEventListener != null) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayerState == PLAYER_STATE_PLAYING) {
                        mPlaybackEventListener.onPlaying();
                    } else if (mPlayerState == PLAYER_STATE_PAUSED) {
                        mPlaybackEventListener.onPaused();
                    }
                }
            });

            // TODO: notify onStopped().
        }

        if (mPlaylistEventListener != null) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayerState == PLAYER_STATE_ENDED) {
                        mPlaylistEventListener.onPlaylistEnded();
                    }
                }
            });
        }
    }

    @JavascriptInterface
    public void notifyError(String data) {
        Log.e(TAG, "notifyError:" + data);
        if (mPlayerStateChangeListener != null) {
            final int errorCode = Integer.parseInt(data);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (errorCode == 5) {
                        mPlayerStateChangeListener.onError(ErrorReason.UNAUTHORIZED_OVERLAY);
                    } else if (errorCode == 100 || errorCode == 101 || errorCode == 150) {
                        mPlayerStateChangeListener.onError(ErrorReason.NOT_PLAYABLE);
                    } else {
                        mPlayerStateChangeListener.onError(ErrorReason.INTERNAL_ERROR);
                    }
                }
            });
        }
    }

    @Override
    public void release() {

    }

    @Override
    public void cueVideo(String s) {

    }

    @Override
    public void cueVideo(String s, int i) {

    }

    @Override
    public void loadVideo(String videoId) {
        loadVideo(videoId, 0);
    }

    @Override
    public void loadVideo(String videoId, int timeMillis) {
        mCurrentTime = timeMillis;
        mDuration = 0;

        mPlayerState = PLAYER_STATE_UNSTARTED;
        mIsVideoLoaded = false;
        mIsPlaylistLoaded = true;
        mPlaylistIndex = -1;
        mPlaylistCount = 0;
        mWebView.evaluateJavascript("player.loadVideoById({videoId:'" + videoId + "', startSeconds: " + timeMillis + "});", null);
    }

    @Override
    public void cuePlaylist(String s) {

    }

    @Override
    public void cuePlaylist(String s, int i, int i1) {

    }

    @Override
    public void loadPlaylist(String playlistId) {
        loadPlaylist(playlistId, 0, 0);
    }

    @Override
    public void loadPlaylist(String playlistId, int startIndex, int timeMillis) {
        mCurrentTime = timeMillis;
        mDuration = 0;

        mPlayerState = PLAYER_STATE_UNSTARTED;
        mIsVideoLoaded = false;
        mIsPlaylistLoaded = false;
        mPlaylistIndex = startIndex;
        mPlaylistCount = 0;
        mWebView.evaluateJavascript("player.loadPlaylist({list:'" + playlistId + "', index:" + startIndex + ", startSeconds:" + timeMillis + "});", null);
    }

    @Override
    public void cueVideos(List<String> list) {

    }

    @Override
    public void cueVideos(List<String> list, int i, int i1) {

    }

    @Override
    public void loadVideos(List<String> list) {

    }

    @Override
    public void loadVideos(List<String> list, int i, int i1) {

    }

    @Override
    public void play() {
        mWebView.evaluateJavascript("player.playVideo();", null);
    }

    @Override
    public void pause() {
        mWebView.evaluateJavascript("player.pauseVideo();", null);
    }

    @Override
    public boolean isPlaying() {
        return (mPlayerState == PLAYER_STATE_PLAYING || mPlayerState == PLAYER_STATE_PAUSED) ;
    }

    @Override
    public boolean hasNext() {
        return (mPlaylistIndex + 1 < mPlaylistCount);
    }

    @Override
    public boolean hasPrevious() {
        return (mPlaylistIndex - 1 >= 0);
    }

    @Override
    public void next() {
        mCurrentTime = 0;
        mDuration = 0;
        mPlayerState = PLAYER_STATE_UNSTARTED;
        mIsVideoLoaded = false;
        mWebView.evaluateJavascript("player.nextVideo();", null);
    }

    @Override
    public void previous() {
        mCurrentTime = 0;
        mDuration = 0;
        mPlayerState = PLAYER_STATE_UNSTARTED;
        mIsVideoLoaded = false;
        mWebView.evaluateJavascript("player.previousVideo();", null);
    }

    @Override
    public int getCurrentTimeMillis() {
        return mCurrentTime;
    }

    @Override
    public int getDurationMillis() {
        return mDuration;
    }

    @Override
    public void seekToMillis(int milliSeconds) {
        int seconds = milliSeconds / 1000;
        mWebView.evaluateJavascript("player.seekTo(" + seconds + ", true);", null);
    }

    @Override
    public void seekRelativeMillis(int milliSeconds) {
        int seconds = (mCurrentTime + milliSeconds) / 1000;
        mWebView.evaluateJavascript("player.seekTo(" + seconds + ", true);", null);
    }

    @Override
    public void setFullscreen(boolean b) {

    }

    @Override
    public void setOnFullscreenListener(OnFullscreenListener onFullscreenListener) {

    }

    @Override
    public void setFullscreenControlFlags(int i) {

    }

    @Override
    public int getFullscreenControlFlags() {
        return 0;
    }

    @Override
    public void addFullscreenControlFlag(int i) {

    }

    @Override
    public void setPlayerStyle(PlayerStyle playerStyle) {

    }

    @Override
    public void setShowFullscreenButton(boolean b) {

    }

    @Override
    public void setManageAudioFocus(boolean b) {

    }

    @Override
    public void setPlaylistEventListener(PlaylistEventListener playlistEventListener) {
        mPlaylistEventListener = playlistEventListener;
    }

    @Override
    public void setPlayerStateChangeListener(PlayerStateChangeListener playerStateChangeListener) {
        mPlayerStateChangeListener = playerStateChangeListener;
    }

    @Override
    public void setPlaybackEventListener(PlaybackEventListener playbackEventListener) {
        mPlaybackEventListener = playbackEventListener;
    }
}
