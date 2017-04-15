package tw.idv.rchu.youtubeplayer;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerWrapper implements
        YouTubePlayer.OnInitializedListener,
        YouTubePlayer.PlayerStateChangeListener,
        YouTubePlayer.PlaybackEventListener,
        YouTubePlayer.PlaylistEventListener {
    static final String TAG = "PlayerWrapper";

    public static final int PLAYER_VIEW = 0;
    public static final int WEB_VIEW = 1;

    private final int mMode;
    private YouTubePlayer mPlayer;

    private String mVideoId = "";
    private String mPlaylistId = "";
    private int mStartTime = 0;
    private int mStartIndex = 0;

    private TextView mVideoName;
    private View mVideoTimeLayout;
    private TextView mCurrentTime;
    private SeekBar mProgress;
    private TextView mEndTime;

    public YouTubePlayerWrapper(int mode) {
        mMode = mode;

        mVideoName = null;
        mProgress = null;
    }

    public Fragment createFragment() {
        if (mMode == YouTubePlayerWrapper.PLAYER_VIEW) {
            YouTubePlayerSupportFragment fragment = YouTubePlayerSupportFragment.newInstance();
            fragment.initialize(Auth.ANDROID_KEY, this);
            return fragment;
        } else {
            WebPlayerFragment fragment = WebPlayerFragment.newInstance();
            fragment.initialize(this);
            return fragment;
        }
    }

    public void setupUI(View view) {
        // Initialize UI components.
        mVideoName = (TextView) view.findViewById(R.id.textVideoName);
        mVideoTimeLayout = view.findViewById(R.id.videoTimeLayout);
        mCurrentTime = (TextView) view.findViewById(R.id.textCurrentTime);
        mCurrentTime.setText(Utils.stringForTime(0));
        mProgress = (SeekBar) view.findViewById(R.id.seekbarTime);
        mProgress.setMax(1000);
        mEndTime = (TextView) view.findViewById(R.id.textDuration);
        mEndTime.setText(Utils.stringForTime(0));
    }


    public void pauseVideo() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void playVideo() {
        if (mPlayer != null) {
            mPlayer.play();
        }
    }

    public void previousVideo() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.hasPrevious()) {
            mPlayer.previous();
            // onPrevious() will be called later.
        } else if (!mPlayer.isPlaying()) {
            playVideo();
            // onPlaying() will be called later.
        }
    }

    public void seekVideo(boolean isAbsolute, int time) {
        if (mPlayer != null) {
            if (isAbsolute) {
                mPlayer.seekToMillis(time);
            } else{
                mPlayer.seekRelativeMillis(time);
            }
        }
    }

    public void nextVideo() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.hasNext()) {
            mPlayer.next();
            // onNext() will be called later.
        } else if (!mPlayer.isPlaying()) {
            playVideo();
            // onPlaying() will be called later.
        }
    }

    public void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.setPlayerStateChangeListener(null);
            mPlayer.setPlaybackEventListener(null);
            mPlayer.setPlaylistEventListener(null);
            mPlayer.release();
        }
        mPlayer = null;
    }

    public void loadYouTube(String videoId, int time, String playlistId, int playlistIndex) {
        Log.d(TAG, "Load video " + videoId + " at " + time);
        Log.d(TAG, "Load playlist " + playlistId + " at " + playlistIndex);

        if (mPlayer != null) {
            if (!playlistId.isEmpty()) {
                mPlayer.loadPlaylist(playlistId, playlistIndex, time);
            } else {
                mPlayer.loadVideo(videoId, time);
            }

            // If player is not initialed, video will be loaded later.
        }

        mVideoId = videoId;
        mPlaylistId = playlistId;
        mStartTime = time;
        mStartIndex = playlistIndex;
    }

    public int getDuration() {
        if (mPlayer != null) {
            return mPlayer.getDurationMillis();
        }
        return 0;
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        mPlayer = player;
        if (!wasRestored) {
            player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
            player.setPlayerStateChangeListener(this);
            player.setPlaybackEventListener(this);
            player.setPlaylistEventListener(this);
        }

        if (!mPlaylistId.isEmpty()) {
            mPlayer.loadPlaylist(mPlaylistId, mStartIndex, mStartTime);
        } else if (!mVideoId.isEmpty()) {
            mPlayer.loadVideo(mVideoId, mStartTime);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult result) {
        Log.e(TAG, "Initialize YouTube player failed:" + result);
    }

    @Override
    public void onPlaying() {
        if (mVideoName != null) {
            mVideoName.setVisibility(View.INVISIBLE);
            mVideoTimeLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPaused() {
        if (mVideoName != null) {
            mVideoName.setVisibility(View.VISIBLE);
            mVideoTimeLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStopped() {
        if (mVideoName != null) {
            mVideoName.setVisibility(View.VISIBLE);
            mVideoTimeLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBuffering(boolean b) {

    }

    @Override
    public void onSeekTo(int i) {

    }

    @Override
    public void onLoading() {

    }

    @Override
    public void onLoaded(String s) {

    }

    @Override
    public void onAdStarted() {

    }

    @Override
    public void onVideoStarted() {
        Log.d(TAG, "onVideoStarted");

        if (mProgress != null) {
            mCurrentTime.setText(Utils.stringForTime(0));
            mProgress.setProgress(0);
            mEndTime.setText(Utils.stringForTime(getDuration()));
        }
    }

    @Override
    public void onVideoEnded() {
        Log.d(TAG, "onVideoEnded");

        if (mVideoName != null) {
            mVideoName.setText("");
        }
        if (mProgress != null) {
            mCurrentTime.setText(Utils.stringForTime(getDuration()));
            mProgress.setProgress(1000);
            mEndTime.setText(Utils.stringForTime(getDuration()));
        }
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        Log.e(TAG, "onError:" + errorReason);
    }

    @Override
    public void onPrevious() {

    }

    @Override
    public void onNext() {

    }

    @Override
    public void onPlaylistEnded() {

    }
}
