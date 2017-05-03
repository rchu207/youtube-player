package tw.idv.rchu.youtubeplayer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tw.idv.rchu.youtubeplayer.dummy.DummyContent;
import tw.idv.rchu.youtubeplayer.dummy.DummyContent.DummyItem;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class YouTubeDataFragment extends Fragment {
    static final String TAG = "YouTubeDataFrag";

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private YouTubeDataWrapper mYouTubeData;
    private HandlerThread mYouTubeDataThread;
    private Handler mYouTubeDataHandler;

    private Handler mUiHandler = new Handler();
    private RecyclerView mRecyclerView;

    private static final int YOUTUBE_SEARCH = 0;

    private class MyYouTubeDataCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case YOUTUBE_SEARCH:
                    try {
                        List<SearchResult> results = mYouTubeData.search((String) msg.obj);
                        final List<DummyItem> ITEMS = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            SearchResult searchResult = results.get(i);
                            ITEMS.add(new DummyItem(
                                    searchResult.getId().getVideoId(),
                                    searchResult.getSnippet().getTitle(),
                                    searchResult.getSnippet().toString()));
                        }

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mRecyclerView != null) {
                                    mRecyclerView.setAdapter(
                                            new MyYouTubeDataRecyclerViewAdapter(ITEMS, mListener));
                                }
                            }
                        });
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


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public YouTubeDataFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static YouTubeDataFragment newInstance(int columnCount) {
        YouTubeDataFragment fragment = new YouTubeDataFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        mYouTubeData = new YouTubeDataWrapper();
        mYouTubeData.init(getString(R.string.app_name));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_youtubedata_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(new MyYouTubeDataRecyclerViewAdapter(DummyContent.ITEMS, mListener));

            mRecyclerView = recyclerView;
        } else {
            mRecyclerView = null;
        }

        mYouTubeDataThread = new HandlerThread("YouTubeData");
        mYouTubeDataThread.start();
        mYouTubeDataHandler = new Handler(mYouTubeDataThread.getLooper(), new MyYouTubeDataCallback());

        // Search keyword "music".
        Message msg = mYouTubeDataHandler.obtainMessage(YOUTUBE_SEARCH);
        msg.obj = "music";
        mYouTubeDataHandler.sendMessage(msg);

        return view;
    }

    @Override
    public void onDestroyView() {
        if (mYouTubeDataThread != null) {
            mYouTubeDataHandler.removeCallbacksAndMessages(null);
            mYouTubeDataHandler = null;
            mYouTubeDataThread.quit();
            mYouTubeDataThread = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(DummyItem item);
    }
}
