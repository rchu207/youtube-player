package tw.idv.rchu.youtubeplayer;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import tw.idv.rchu.youtubeplayer.dummy.DummyContent;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        WebDataFragment.OnLoadYouTubeListener,
        YouTubeDataFragment.OnListFragmentInteractionListener,
        WebPlayerFragment.OnPlayerStateChangeListener {

    private YouTubePlayerWrapper mWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Launch WebData fragment by default.
        navigationView.setCheckedItem(R.id.nav_web);
        addWebDataFragment();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().findFragmentByTag(WebDataFragment.TAG) == null) {
            addWebDataFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_web) {
            addWebDataFragment();
        } else if (id == R.id.nav_youtube) {
            addYouTubeDataFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCastYouTube(int playerType, String videoId, int time, String playlistId, int playlistIndex) {
        mWrapper = new YouTubePlayerWrapper(playerType);
        mWrapper.loadYouTube(videoId, time, playlistId, playlistIndex);

        Fragment fragment = mWrapper.createFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_layout, fragment);
        transaction.commit();
    }

    @Override
    public void onVideoEnded() {

    }

    private void addWebDataFragment() {
        Fragment fragment = WebDataFragment.newInstance(Uri.parse("https://m.youtube.com"));
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_layout, fragment, WebDataFragment.TAG);
        transaction.commit();
    }

    private void addYouTubeDataFragment() {
        Fragment fragment = YouTubeDataFragment.newInstance(1);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_layout, fragment, YouTubeDataFragment.TAG);
        transaction.commit();
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {
        onCastYouTube(YouTubePlayerWrapper.PLAYER_VIEW, item.id, 0, "", 0);
    }
}
