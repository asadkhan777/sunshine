package com.example.asadkhan.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainForecastActivity extends AppCompatActivity implements MainForecastFragment.Callback {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private final String LOG_TAG = MainForecastActivity.class.getSimpleName();

    private String mLocation;

//    private final String FORECASTFRAGMENT_TAG = "FFTAG";
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation( this );

        setContentView(R.layout.activity_main);

        //Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        // setSupportActionBar(toolbar);

        if(findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container,
                                new ForecastDetailFragment(),
                                DETAILFRAGMENT_TAG)
                        .commit();
            }
            // else
            //    Saved data is present, and will be displayed in the dynamic fragment
        } else {
            mTwoPane = false;
            ActionBar actionBar = getSupportActionBar();
            if ( actionBar != null){
                actionBar.setElevation(0f);
            }
        }

        MainForecastFragment mainForecastFragment =
                (MainForecastFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_forecast);

        if (null != mainForecastFragment){
                mainForecastFragment.setAdapterTwoPaneSetting(mTwoPane);
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        Log.e(LOG_TAG, "Step 1 : OnCreate method here!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_map){
            // Do something
            // Toast.makeText(this, "Clicked on [View Location]", Toast.LENGTH_SHORT).show();
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String location_id = sharedPref.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        final String QUERY_PARAM = "q";
        final String ZOOM_PARAM = "z";

        Uri geoloc = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter(QUERY_PARAM, location_id)
                .appendQueryParameter(ZOOM_PARAM, "20")
                .build();
        Log.e(LOG_TAG, "\nGeoLoc URI : \n" + geoloc);

        Intent intent_map = new Intent(Intent.ACTION_VIEW);
        intent_map.setData(geoloc);
        if (intent_map.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(intent_map);
            } catch (Exception e) {
                String error = "Couldn't call > " + location_id ;
                Log.e(LOG_TAG, e.getMessage() + error);
            }
        } else {
            String error = "No map app appears to be available > " + location_id ;
            Log.e(LOG_TAG, error);
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    // Experimenting with Activity lifecycle

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
        Log.e(LOG_TAG, "Step 2 : OnStart method here!");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e(LOG_TAG, "Step 3/W : OnPause method here!");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.e(LOG_TAG, "Step 4/X : OnResume method here!");
        String loco = Utility.getPreferredLocation(this);
        if ( loco != null && !loco.equals(mLocation) ) {

            // Check Main fragment
            MainForecastFragment mff =
                    (MainForecastFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.fragment_forecast);
            if (null != mff){
                mff.onLocationChanged();
            }

            // Check Detail fragment
            ForecastDetailFragment fdf =
                    (ForecastDetailFragment) getSupportFragmentManager()
                                    .findFragmentByTag(DETAILFRAGMENT_TAG);
            if (fdf != null){
                fdf.onLocationChanged(loco);
            }

            mLocation = loco;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
        Log.e(LOG_TAG, "Step 5/Y : OnStop method here!");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e(LOG_TAG, "Step 6/Z : OnDestroy method here!");
    }


    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(ForecastDetailFragment.DETAIL_URI, contentUri);

            ForecastDetailFragment forcastdetalfragmint = new ForecastDetailFragment();
            forcastdetalfragmint.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, forcastdetalfragmint, DETAILFRAGMENT_TAG)
                    .commit();

        } else {
            Intent intent = new Intent(this, ForecastDetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }

    }
}
