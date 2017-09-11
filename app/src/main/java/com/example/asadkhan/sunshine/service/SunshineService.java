package com.example.asadkhan.sunshine.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.asadkhan.sunshine.sync.SunshineSyncAdapter;

import okhttp3.OkHttpClient;

/**
 * Brought to you by asadkhan on 3/4/17.
 */

public class SunshineService extends IntentService {

    private String LOG_TAG = SunshineService.class.getSimpleName();

    private OkHttpClient client;

    // private ArrayAdapter<String> mForecastAdapter;
    // public static final MediaType JSON =
    //       MediaType.parse("application/json; charset=utf-8");
    // These two need to be declared outside the try/catch
    // so that they can be closed in the finally block.
    // HttpURLConnection urlConnection = null;
    // BufferedReader reader = null;

    private boolean DEBUG = true;


    public SunshineService() {
        super("SunshineService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if ( client == null ) {
            client = new OkHttpClient();
        }
    }


//
//    void getDataHttpURLConnection(URL url){
//        try {
//            // Create the request to OpenWeatherMap, and open the connection
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("GET");
//            urlConnection.connect();
//
//            // Read the input stream into a String
//            InputStream inputStream = urlConnection.getInputStream();
//            StringBuilder stringBuilder = new StringBuilder();
//            if (inputStream == null) {
//                // Nothing to do.
//                return;
//            }
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                // But it does make debugging a *lot* easier if you print out the completed
//                // buffer for debugging.
//                stringBuilder.append(line).append("\n");
//            }
//            if (stringBuilder.length() == 0) {
//                // Stream was empty.  No point in parsing.
//                return;
//            }
//            // Create the weather JSON data string
//            forecastJsonStr = stringBuilder.toString();
//
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "Error ", e);
//            // If the code didn't successfully get the weather data, there's no point in attemping
//            // to parse it.
//        } finally {
//
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (final IOException e) {
//                    Log.e(LOG_TAG, "Error closing stream", e);
//                }
//            }
//            // Log.e(LOG_TAG, "\nHere is Forecast data : \n" + forecastJsonStr);
//        }
//    }


    /**
     *  Critical method invoked when this service is called from outside
     *
     *  @param intent the intent holding the user's location data
     *
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.e(LOG_TAG, "Inside Sunshine Service");
        // getDataFromAPI(intent);
    }


    /**
     *   Inner class broadcast receiver
     */
    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SunshineSyncAdapter.syncImmediately(context);
//            Intent sendIntent = new Intent(context, SunshineService.class);
//            sendIntent.putExtra(
//                    SunshineService.LOCATION_QUERY_EXTRA,
//                    intent.getStringExtra(SunshineService.LOCATION_QUERY_EXTRA));
//            context.startService(sendIntent);

        }
    }

}