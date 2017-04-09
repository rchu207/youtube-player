package tw.idv.rchu.youtubeplayer;

//import com.google.android.gms.common.Scopes;
//import com.google.api.services.youtube.YouTubeScopes;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

public class Auth {
    // FIXME: Register an API key here: https://console.developers.google.com
    public static final String ANDROID_KEY = "";
    public static final String BROSWER_KEY = "";

    //public static final String[] SCOPES = {Scopes.PROFILE, YouTubeScopes.YOUTUBE};

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new GsonFactory();
}