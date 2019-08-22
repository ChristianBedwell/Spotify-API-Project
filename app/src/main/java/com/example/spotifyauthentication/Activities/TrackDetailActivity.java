package com.example.spotifyauthentication.Activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.example.spotifyauthentication.R;
import com.spotify.protocol.types.Track;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class TrackDetailActivity extends AppCompatActivity {

    // declare constants
    public static final String CLIENT_ID = "clientid";
    public static SpotifyAppRemote mSpotifyAppRemote;
    private static final String TAG = TrackDetailActivity.class.getSimpleName();

    private String trackUri, shareLink, trackShareName, trackShareArtist;
    private TextView trackName, trackYear, trackArtist, trackPopularityNumber, trackItemNumber;
    private ImageView trackImage;
    private Button playButton, shareButton;
    private RatingBar trackPopularity;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_detail);

        // add custom toolbar to the activity
        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(toolbar.getNavigationIcon()).setColorFilter(getResources().
                getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);

        // initialize the views
        trackName = (TextView) findViewById(R.id.track_detail_name);
        trackYear = (TextView) findViewById(R.id.track_detail_year);
        trackArtist = (TextView) findViewById(R.id.track_detail_artist);
        trackPopularityNumber = (TextView) findViewById(R.id.track_detail_popularity_number);
        trackItemNumber = (TextView) findViewById(R.id.track_detail_item_number);
        trackImage = (ImageView) findViewById(R.id.track_detail_image);
        playButton = (Button) findViewById(R.id.play_button);
        shareButton = (Button) findViewById(R.id.share_button);
        trackPopularity = (RatingBar) findViewById(R.id.track_detail_popularity);

        // get intent extras from adapter
        trackUri = getIntent().getStringExtra("track_uri");
        shareLink = getIntent().getStringExtra("track_share_link");
        trackShareName = getIntent().getStringExtra("track_share_name");
        trackShareArtist = getIntent().getStringExtra("track_artist");
        trackName.setText(getIntent().getStringExtra("track_name"));
        trackYear.setText(getIntent().getStringExtra("track_year"));
        trackArtist.setText(getIntent().getStringExtra("track_artist"));
        trackPopularityNumber.setText(getIntent().getStringExtra("track_popularity_number"));
        trackItemNumber.setText(getIntent().getStringExtra("track_item_number"));
        trackPopularity.setRating((getIntent().getFloatExtra("track_popularity", 0.0f)));
        Picasso.get().load(getIntent().getStringExtra("track_image_resource")).into(trackImage);

        // if Spotify app is installed on Android device
        if(SpotifyAppRemote.isSpotifyInstalled(getApplicationContext())) {
            if(mSpotifyAppRemote != null) {
                mSpotifyAppRemote.getUserApi().getCapabilities().setResultCallback(capabilities -> {
                    // current user is able to play on demand
                    if (capabilities.canPlayOnDemand) {
                        playButton.setVisibility(View.VISIBLE);
                    }
                    // current user is not able to play on demand
                    else {
                        playButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }
        // if Spotify app is not installed on Android device
        else {
            playButton.setVisibility(View.INVISIBLE);
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open track with Spotify app remote
                openTrack();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send track via SMS or email
                shareTrack();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.detail_activity_in, R.anim.detail_activity_out);
        // if Spotify app is installed on Android device
        if(SpotifyAppRemote.isSpotifyInstalled(getApplicationContext())) {
            if(mSpotifyAppRemote != null) {
                mSpotifyAppRemote.getUserApi().getCapabilities().setResultCallback(capabilities -> {
                    // current user is able to play on demand
                    if (capabilities.canPlayOnDemand) {
                        mSpotifyAppRemote.getPlayerApi().pause();
                        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_most_popular, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home){
            onBackPressed();
            return true;
        }
        else if(id == R.id.action_settings) {
            // open settings activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openTrack() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);

        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(getRedirectUri().toString())
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {

            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d(TAG, "Connected! Yay!");

                // App Remote is connected and interactable
                connected();
            }

            public void onFailure(Throwable throwable) {
                Log.e(TAG, throwable.getMessage(), throwable);
            }
        });
    }

    // app remote is connected
    private void connected() {
        // Play a playlist
        mSpotifyAppRemote.getPlayerApi().play(trackUri);

        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
            final Track track = playerState.track;
            if (track != null) {
                Log.d(TAG, track.name + " by " + track.artist.name);
            }
        });
    }

    // get redirect uri using redirect scheme and host
    private Uri getRedirectUri() {
        return new Uri.Builder()
                .scheme(getString(R.string.com_spotify_sdk_redirect_scheme))
                .authority(getString(R.string.com_spotify_sdk_redirect_host))
                .build();
    }

    // share the track uri with a contact
    public void shareTrack() {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, trackShareName
                + " by " + trackShareArtist);
        share.putExtra(Intent.EXTRA_TEXT, shareLink);

        startActivity(Intent.createChooser(share, "Share link!"));
    }
}
