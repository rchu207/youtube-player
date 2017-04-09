package tw.idv.rchu.youtubeplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebDataFragment extends Fragment implements
        View.OnClickListener {
    static final String TAG = "WebDataFrag";

    static final String ARG_URI = "arg_uri";

    private OnLoadYouTubeListener mListener;
    private WebView mWebView;
    private String mVideoId;
    private String mPlaylistId;
    private String mPlaylistIndex;

    private AlertDialog mYouTubeDialog;
    private EditText mEditTitle;
    private EditText mEditDescription;
    private ImageView mImageThumbnail;

    private Handler mUiHandler = new Handler();

    private YouTubeDataWrapper mYouTubeData;
    private HandlerThread mYouTubeDataThread;
    private Handler mYouTubeDataHandler;
    private ArrayList<Video> mYouTubeVideos;

    @Override
    public void onClick(View view) {
        int mode;
        if (view.getId() == R.id.buttonWebPlayer) {
            mode = YouTubePlayerWrapper.WEB_VIEW;
        } else {
            mode = YouTubePlayerWrapper.PLAYER_VIEW;
        }
        mYouTubeDialog.dismiss();

        if (mListener != null) {
            if (mPlaylistId != null && mPlaylistIndex != null) {
                mListener.onCastYouTube(mode, mVideoId, 0, mPlaylistId, Integer.parseInt(mPlaylistIndex));
            } else {
                mListener.onCastYouTube(mode, mVideoId, 0, "", 0);
            }
        }
    }

    private static final int YOUTUBE_QUERY_VIDEO = 0;
    private static final int YOUTUBE_QUERY_PLAYLIST = 1;
    private static final int YOUTUBE_QUERY_PLAYLIST_ITEMS = 2;

    private class MyYouTubeDataCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case YOUTUBE_QUERY_VIDEO:
                    try {
                        // Query video information.
                        final String videoId = (String) msg.obj;
                        final List<Video> videoList = mYouTubeData.query(videoId);

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mYouTubeVideos.addAll(videoList);

                                // Check video ID exists in the result or not.
                                for (int i = 0; i < mYouTubeVideos.size(); i++) {
                                    if (mYouTubeVideos.get(i).getId().equals(mVideoId)) {
                                        mPlaylistIndex = String.valueOf(i);
                                        getActivity().supportInvalidateOptionsMenu();
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    break;
                case YOUTUBE_QUERY_PLAYLIST:
                    try {
                        String[] params = (String[]) msg.obj;
                        mYouTubeData.queryListCount(params[0]);
                        mYouTubeData.queryListItemPosition(params[0], params[1]);
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    break;
                case YOUTUBE_QUERY_PLAYLIST_ITEMS:
                    try {
                        // Query playlist information.
                        final ArrayList<Video> videoList = new ArrayList<>(YouTubeDataWrapper.MAX_RESULT_COUNT);
                        String[] params = (String[]) msg.obj;
                        String token = mYouTubeData.queryListItems(params[0], params[1], videoList);

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mYouTubeVideos.addAll(videoList);

                                if (mVideoId == null) {
                                    mVideoId = mYouTubeVideos.get(0).getId();
                                    mPlaylistIndex = "0";
                                    getActivity().supportInvalidateOptionsMenu();
                                } else {
                                    // Check video ID exists in the result or not.
                                    for (int i = 0; i < mYouTubeVideos.size(); i++) {
                                        if (mYouTubeVideos.get(i).getId().equals(mVideoId)) {
                                            mPlaylistIndex = String.valueOf(i);
                                            getActivity().supportInvalidateOptionsMenu();
                                            break;
                                        }
                                    }
                                }
                            }
                        });

                        if (token != null) {
                            Message msgNext = new Message();
                            msgNext.what = YOUTUBE_QUERY_PLAYLIST_ITEMS;
                            String[] params2 = new String[2];
                            params2[0] = mPlaylistId;
                            params2[1] = token;
                            msgNext.obj = params2;
                            mYouTubeDataHandler.sendMessage(msgNext);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    private Runnable mUrlChangedRunnable = new Runnable() {
        String url = "";

        @Override
        public void run() {
            String currentUrl = mWebView.getUrl();
            if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals(url)) {
                url = currentUrl;
                parseYouTube(url);
            }

            if (mPausedCount > 0) {
                pauseYouTube();
                mPausedCount--;
            }

            mUiHandler.postDelayed(mUrlChangedRunnable, 500);
        }
    };

    private int mPausedCount = 0;

    public static WebDataFragment newInstance(Uri weburl) {
        WebDataFragment fragment = new WebDataFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, weburl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoadYouTubeListener) {
            mListener = (OnLoadYouTubeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLoadYouTubeListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mYouTubeData = new YouTubeDataWrapper();
        mYouTubeData.init(getString(R.string.app_name));
        mYouTubeVideos = new ArrayList<>(200);

        mVideoId = null;
        mPlaylistId = null;
        mPlaylistIndex = null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        // Inflate the XML layout.
        View view = inflater.inflate(R.layout.fragment_web_data, container, false);

        // Get UI widgets.
        mWebView = (WebView) view.findViewById(R.id.fileDetailView);

        // Setup file detail view.
        mWebView.setKeepScreenOn(true);

        // Enable JavaScript.
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.getSettings().setAppCacheEnabled(true);
        // TODO: setup webview cache folder.
//        File cacheFolder = ImageCache.getDiskCacheDir(getActivity(), "webyt");
//        if (cacheFolder.exists() || cacheFolder.mkdirs()) {
//            mWebView.getSettings().setAppCachePath(cacheFolder.getAbsolutePath());
//        }
        mWebView.getSettings().setGeolocationEnabled(false);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        // Display web content inside our WebView widget.
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient());

        // Go to previous or default URL.
        Uri defaultUrl = getArguments().getParcelable(ARG_URI);
        if (defaultUrl != null) {
            mWebView.loadUrl(defaultUrl.toString());
        }

        mYouTubeDialog = createCastingDialog(getActivity());

        mYouTubeDataThread = new HandlerThread("YouTubeData");
        mYouTubeDataThread.start();
        mYouTubeDataHandler = new Handler(mYouTubeDataThread.getLooper(), new MyYouTubeDataCallback());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mWebView.onResume();

        mUiHandler.post(mUrlChangedRunnable);
    }

    @Override
    public void onPause() {
        mUiHandler.removeCallbacks(mUrlChangedRunnable);

        mWebView.onPause();

        super.onPause();
    }

    @Override
    public void onStop() {
        if (mYouTubeDialog != null) {
            mYouTubeDialog.dismiss();
        }

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mYouTubeDataThread != null) {
            mYouTubeDataHandler.removeCallbacksAndMessages(null);
            mYouTubeDataHandler = null;
            mYouTubeDataThread.quit();
            mYouTubeDataThread = null;
        }

        mYouTubeDialog = null;

        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.web_data, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            if (itemId == R.id.menuCast) {
                if (mPlaylistIndex != null) {
                    item.setEnabled(true);
                    item.getIcon().setAlpha(Utils.ALPHA_ENABLE);
                } else {
                    item.setEnabled(false);
                    item.getIcon().setAlpha(Utils.ALPHA_DISABLE);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menuCast) {
            castYouTube();
            return true;
        } else if (itemId == R.id.menuRefresh) {
            // TODO: close drawer.
            mWebView.reload();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void parseYouTube(String url) {
        Uri uri = Uri.parse(url.replace("youtube.com/#/", "youtube.com/"));
        String path = uri.getPath();

        Log.d(TAG, "parse: " + url);

        if (path != null && (path.equals("/watch") || path.equals("/playlist"))) {
            String id = null;
            String list = uri.getQueryParameter("list");
            String index = null;

            if (path.equals("/watch")) {
                id = uri.getQueryParameter("v");

                index = uri.getQueryParameter("index");
                for (int i = 0; i < mYouTubeVideos.size(); i++) {
                    Video video = mYouTubeVideos.get(i);
                    if (video.getId().equals(id)) {
                        // We already get its information, can cast it to projector now.
                        index = String.valueOf(i);
                        break;
                    }
                }
            }

            // User watches the same playlist again.
            boolean isSamePlaylist = (mPlaylistId != null && list != null && mPlaylistId.equals(list));

            mVideoId = id;
            mPlaylistId = list;
            mPlaylistIndex = index;
            mPausedCount = 10; // Try to pause YouTube playback in 5 seconds.
            Log.i(TAG, "Watch video:" + mVideoId + " in list:" + mPlaylistId + "-" + mPlaylistIndex);

            if (mPlaylistIndex != null) {
                getActivity().supportInvalidateOptionsMenu();
                return;
            }
            if (isSamePlaylist) {
                return;
            }

            // Query video's information.
            mYouTubeVideos.clear();
            if (mYouTubeDataHandler != null) {
                mYouTubeDataHandler.removeCallbacksAndMessages(null);

                Message msg = new Message();
                if (mPlaylistId != null) {
                    // Query playlist's information.
                    msg.what = YOUTUBE_QUERY_PLAYLIST_ITEMS;
                    String[] params = new String[2];
                    params[0] = mPlaylistId;
                    params[1] = "";
                    msg.obj = params;
                    mYouTubeDataHandler.sendMessage(msg);
                } else {
                    // Query single video's information.
                    msg.what = YOUTUBE_QUERY_VIDEO;
                    msg.obj = mVideoId;
                    mYouTubeDataHandler.sendMessage(msg);
                }
            }
        } else {
            mVideoId = null;
            mPlaylistId = null;
            mPlaylistIndex = null;
            mPausedCount = 0;

            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void pauseYouTube() {
        String script = "var videos = document.querySelectorAll(\"video\"); for (var i = videos.length - 1; i >= 0; i--) { videos[i].pause(); };";

        if (Utils.hasKitkat()) {
            mWebView.evaluateJavascript(script, null);
        } else {
            mWebView.loadUrl("javascript:" + script);
        }
    }

    private void castYouTube() {
        if (mVideoId == null) {
            Log.w(TAG, "YouTube video ID is not found!");
            return;
        }

        if (mYouTubeDialog != null) {
            mYouTubeDialog.dismiss();
        }

        // Reset YouTube control dialog.
        if (mEditTitle != null) {
            mEditTitle.setText(R.string.video_title);
        } else {
            mYouTubeDialog.setTitle(getString(R.string.video_title));
        }
        updateYouTubeTitle();
        mYouTubeDialog.show();
    }

    private AlertDialog createCastingDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder
                .setCancelable(true)
                .setPositiveButton(R.string.close, null)
                .create();

        View view = dialog.getLayoutInflater().inflate(R.layout.dialog_youtube_control, null);

        mEditTitle = (EditText) view.findViewById(R.id.editTextTitle);
        mEditDescription = (EditText) view.findViewById(R.id.editTextDescription);
        mImageThumbnail = (ImageView) view.findViewById(R.id.imageView);

        Button button;
        button = (Button) view.findViewById(R.id.buttonWebPlayer);
        button.setOnClickListener(this);
        button = (Button) view.findViewById(R.id.buttonYoutubePlayer);
        button.setOnClickListener(this);

        dialog.setView(view);
        return dialog;
    }

    private void updateYouTubeTitle() {
        if (mVideoId == null) {
            return;
        }

        // Show video information.
        for (int i = 0; i < mYouTubeVideos.size(); i++) {
            Video video = mYouTubeVideos.get(i);
            if (video.getId().equals(mVideoId)) {
                String title = video.getSnippet().getTitle();
                if (mPlaylistId != null) {
                    title = (i + 1) + "/" + mYouTubeVideos.size() + " " + title;
                }

                if (mEditTitle != null) {
                    mEditTitle.setText(title);
                } else if (mYouTubeDialog != null) {
                    mYouTubeDialog.setTitle(title);
                }

                if (mEditDescription != null) {
                    mEditDescription.setText(video.getSnippet().getDescription());
                }

                if (mImageThumbnail != null) {
                    Glide.with(getActivity())
                            .load(video.getSnippet().getThumbnails().getMedium().getUrl())
                            .into(mImageThumbnail);
                }

                break;
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    interface OnLoadYouTubeListener {
        void onCastYouTube(int playerType, String videoId, int time, String playlistId, int playlistIndex);
    }
}
