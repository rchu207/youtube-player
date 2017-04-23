package tw.idv.rchu.youtubeplayer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WebPlayerFragment.OnPlayerStateChangeListener} interface
 * to handle interaction events.
 * Use the {@link WebPlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebPlayerFragment extends Fragment {
    private YouTubePlayerWrapper mWrapper;
    private OnPlayerStateChangeListener mListener;

    private Handler mUiHandler = new Handler();
    private boolean mIsResume = false;

    public WebPlayerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WebPlayerFragment.
     */
    public static WebPlayerFragment newInstance() {
        return new WebPlayerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web_player, container, false);

        YouTubeWebView webView = (YouTubeWebView) view.findViewById(R.id.youtubeView);
        if (webView != null) {
            // Developer key is not used in WebView.
            webView.initialize("", mWrapper);
        }
        mWrapper.setupUI(view);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPlayerStateChangeListener) {
            mListener = (OnPlayerStateChangeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update current position immediately.
        mIsResume = true;
        mUiHandler.postDelayed(mVideoTimeRefresher, 10);
    }

    @Override
    public void onPause() {
        mIsResume = false;
        mUiHandler.removeCallbacks(mVideoTimeRefresher);

        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void initialize(YouTubePlayerWrapper wrapper) {
        mWrapper = wrapper;
    }

    /**
     * ProgressRefresher
     */
    class ProgressRefresher implements Runnable {
        public void run() {
            if (!mIsResume) {
                return;
            } else if (mWrapper != null) {
                mWrapper.getCurrentPosition();
            }

            mUiHandler.removeCallbacks(mVideoTimeRefresher);
            // Because the position is only updated every 1 second when use YouTubePlayerView,
            // using 1000ms is enough.
            mUiHandler.postDelayed(mVideoTimeRefresher, 1000);
        }
    }

    ProgressRefresher mVideoTimeRefresher = new ProgressRefresher();

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnPlayerStateChangeListener {
        void onVideoEnded();
    }
}
