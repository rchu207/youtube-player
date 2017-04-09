package tw.idv.rchu.youtubeplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.youtube.player.YouTubePlayer;

public class YouTubeWebView extends WebView {
    private YouTubeWebPlayer mPlayer;
    private YouTubePlayer.OnInitializedListener mOnInitializedListener;

    public YouTubeWebView(Context context) {
        super(context);
    }

    public YouTubeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YouTubeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initialize(String developerKey, YouTubePlayer.OnInitializedListener onInitializedListener) {
        mOnInitializedListener = onInitializedListener;
        mPlayer = new YouTubeWebPlayer(this);

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals("ytplayer")) {
                    if (uri.getHost().equals("onReady")) {
                        if (mOnInitializedListener != null) {
                            mOnInitializedListener.onInitializationSuccess(null, mPlayer, false);
                        }
                    } else if (uri.getHost().equals("onPlayTime")) {
                        String time = uri.getQueryParameter("time");
                        mPlayer.setCurrentTime(Float.parseFloat(time));
                    }

                    return true;
                }

                return false;
            }
        });

        setInitialScale(1);

        getSettings().setPluginState(WebSettings.PluginState.ON);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAppCacheEnabled(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setMediaPlaybackRequiresUserGesture(false);

        addJavascriptInterface(mPlayer, "Android");

        loadUrl("file:///android_asset/YTPlayerView-iframe-player.html");
    }
}


