package com.example.asadkhan.sunshine.service;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.asadkhan.sunshine.BuildConfig;
import com.example.asadkhan.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Brought to you by asadkhan on 3/4/17.
 */

public class SunshineService extends IntentService {

    private ArrayAdapter<String> mForecastAdapter;
    public static final String LOCATION_QUERY_EXTRA = "lqe";
    private String LOG_TAG = SunshineService.class.getSimpleName();
    public SunshineService() {
        super("SunshineService");
    }

    // These two need to be declared outside the try/catch
    // so that they can be closed in the finally block.
    HttpURLConnection urlConnection = null;
    BufferedReader reader = null;

    // Will contain the raw JSON response as a string.
    String forecastJsonStr = null;

    String format = "json";
    String units = "metric";
    int numDays = 14;

    private boolean DEBUG = true;

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long location_id;
        Uri insertedUri;
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        Cursor loco_cursor = this.getContentResolver()
                                .query(WeatherContract.LocationEntry.CONTENT_URI,
                                    new String[]{WeatherContract.LocationEntry._ID},
                                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                                    new String[]{locationSetting},
                                    null);

        if( loco_cursor != null && loco_cursor.moveToFirst() ){
            int locationIDIndex = loco_cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            location_id = loco_cursor.getLong(locationIDIndex);
            loco_cursor.close();
        } else {
            if (loco_cursor != null ) {
                loco_cursor.close();
            }
            ContentValues location_vals = new ContentValues();

            location_vals.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            location_vals.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            location_vals.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            location_vals.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            insertedUri = this.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    location_vals
            );
            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            location_id = ContentUris.parseId(insertedUri);
        }
        return location_id;
    }

    private void getWeatherDataFromJson(String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                Log.e(LOG_TAG, "Right Now, for day ["+i+"] humidity is " + humidity);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            int inserted = 0 ;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cv_array = new ContentValues[cVVector.size()];
                cVVector.toArray(cv_array);
                inserted = this.getContentResolver()
                        .bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cv_array);
            }

//            // Sort order:  Ascending, by date.
//            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//            Uri weatherForLocationUri =
//                    WeatherContract.WeatherEntry.
//                            buildWeatherLocationWithStartDate(
//                                locationSetting,
//                                System.currentTimeMillis());
//
//            // Students: Uncomment the next lines to display what what you stored in the bulkInsert
//
//            Cursor cur = this.getContentResolver().query(weatherForLocationUri,
//                    null, null, null, sortOrder);
//            if (cur != null) {
//                cVVector = new Vector<>(cur.getCount());
//                if ( cur.moveToFirst() ) {
//                    do {
//                        ContentValues cv = new ContentValues();
//                        DatabaseUtils.cursorRowToContentValues(cur, cv);
//                        cVVector.add(cv);
//                    } while (cur.moveToNext());
//                }
//            }
            Log.d(LOG_TAG, "Sunshine Service Complete. " + inserted + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(LOG_TAG, "Inside Sunshine Service");

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if ((intent != null) && intent.hasExtra(LOCATION_QUERY_EXTRA)) {

            String locationQuery = intent.getStringExtra(LOCATION_QUERY_EXTRA);

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, locationQuery)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    stringBuilder.append(line + "\n");
                }

                if (stringBuilder.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return;
                }
                forecastJsonStr = stringBuilder.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.

            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
                Log.e(LOG_TAG, "\nHere is Forecast data : \n" + forecastJsonStr);
            }
            // Now to store the data for use
            try {
                this.getWeatherDataFromJson( forecastJsonStr, locationQuery );
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error : " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
    }

}

