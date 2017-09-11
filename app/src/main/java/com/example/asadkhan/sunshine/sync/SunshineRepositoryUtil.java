package com.example.asadkhan.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.example.asadkhan.sunshine.BuildConfig;
import com.example.asadkhan.sunshine.data.WeatherContract;
import com.example.asadkhan.sunshine.data.WeatherDisplayDO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Kreated by asadkhan on 06 | September |  2017 | at 8:38 PM.
 */

class SunshineRepositoryUtil {

    private String LOG_TAG = this.getClass().getSimpleName();


    // public static final String LOCATION_QUERY_EXTRA = "lqe";
    private static final String FORMAT = "json";
    private static final String UNITS = "metric";
    private static final int NUM_DAYS = 14;

    private Boolean hit = false;
    private ContentResolver myContentResolver;


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
    // Weather information.
    // Each day's forecast info is an element of the "list" array.
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


    SunshineRepositoryUtil() {
        // Use Builder setters to set myContent resolver
    }

    Boolean getDataFromAPI(String locationQuery){
        if ( myContentResolver == null ) {
            Log.e(LOG_TAG, "Content resolver cant be null!!!");
            return hit;
        }
        if (checkDbValuesExist(locationQuery, NUM_DAYS)){
            Log.e(LOG_TAG, "Values are present in the DB, no need to hit API");
            return hit;
        }

        // Prepare the URI which will be invoked for data
        URL url = createWeatherAPIURL(locationQuery);

        // // Get data from API server using classic HttpURLConnection
        // getDataHttpURLConnection(url);

        // Get data from API server using OkHttpClient
        String jsonResult = getDataOKHttpClient(url);

        // Now to store the data for use
        insertJSONIntoDB(locationQuery, jsonResult);
        Log.e(LOG_TAG, "Successful operation!");
        return hit;
    }

    private String getDataOKHttpClient(URL url) {
        String forecastJsonStr = null;
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = null;
        OkHttpClient okHttpInstance = new OkHttpClient();
        try {
            response = okHttpInstance.newCall(request).execute();

            if (response != null) {
                if (response.isSuccessful()) {
                    if ( response.body() == null ) { Log.e(LOG_TAG, "Response body is null"); }
                    else { forecastJsonStr = response.body().string(); }
                    response.close();
                    // response = null;
                    return forecastJsonStr;
                } else {
                    Log.e(LOG_TAG, "Some Error occurred ... ");
                    Log.e(LOG_TAG, "Code : " + Integer.toString(response.code()));
                    Log.e(LOG_TAG, "Details : " + response.body());
                }
            } else { Log.e(LOG_TAG, "Response is null .... Pls chekk"); }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error here! Something wrong in JSON response for : \n" + url.toString());
            e.printStackTrace();
        } finally {
            if ( response != null ) {
                response.close();
            }
        }
        return null;
    }

    private void insertJSONIntoDB(String locationQuery, String resultData){
        try {
            Vector<ContentValues> cvData = this.
                    getWeatherDataFromJson( resultData, locationQuery );
            if (cvData != null) {
                int result = insertContentValuesIntoDB(cvData);
                if (result > 0) { verifyDbValues(cvData, locationQuery); }
            }
            else { Log.e(LOG_TAG, "Weather data is null, please check!"); }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Some Error : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long location_id;
        Uri insertedUri;
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        Cursor loco_cursor = myContentResolver.query(WeatherContract.LocationEntry.CONTENT_URI,
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

            insertedUri = myContentResolver.insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    location_vals
            );
            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            location_id = ContentUris.parseId(insertedUri);
        }
        return location_id;
    }

    private URL createWeatherAPIURL(String locationQueryParam){

        // Construct the URL for the OpenWeatherMap query
        // Possible parameters are avaiable at OWM's forecast API page, at
        // http://openweathermap.org/API#forecast

        URL weatherUrl;

        final String FORECAST_BASE_URL =
                "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "UNITS";
        final String DAYS_PARAM = "cnt";
        final String APPID_PARAM = "APPID";

        Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, locationQueryParam)
                .appendQueryParameter(FORMAT_PARAM, FORMAT)
                .appendQueryParameter(UNITS_PARAM, UNITS)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(NUM_DAYS))
                .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                .build();

        try{
            weatherUrl = new URL(builtUri.toString());
        } catch (Exception e){
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
        Log.w(LOG_TAG, "Built URI " + builtUri.toString());
        return weatherUrl;
    }

    private boolean checkDbValuesExist(String locationSetting, int expectCount) {

        boolean exists;
        int rowCount = 0;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri =
                WeatherContract.WeatherEntry.
                        buildWeatherLocationWithStartDate(
                                locationSetting,
                                System.currentTimeMillis());

        Cursor cur = myContentResolver.query(weatherForLocationUri,
                null,
                null,
                null,
                sortOrder);
        if (cur != null) {
            rowCount = cur.getCount();
            cur.close();
        }
        exists = ( rowCount >= expectCount );
        // Log.w(LOG_TAG, "Exists : " + exists);
        // Log.w(LOG_TAG, "RowCount : " + rowCount);
        // Log.w(LOG_TAG, "ExpCount : " + expectCount);
        return exists;
    }

    private Vector<ContentValues> getWeatherDataFromJson(
                                    String forecastJsonStr,
                                    String locationSetting) {
        Vector<ContentValues> cVVector;
        try {
            if (forecastJsonStr == null) {
                Log.e(LOG_TAG, "JSON string result is null, please check!");
                return null;
            }

            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            JSONObject cityJson = forecastJson.getJSONObject(this.OWM_CITY);
            long locationId = saveLocationData(cityJson, locationSetting);
            
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            cVVector = new Vector<>(weatherArray.length());
            // Insert the new weather information into the database
            for(int i = 0; i < weatherArray.length(); i++) {
                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                // That element also contains a weather code.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                // // Description is in a child array called "weather", which is 1 element long.
                
                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

                // Cheating to convert this to UTC time, which is what we want anyhow
                // dateTime = dayTime.setJulianDay(julianStartDay+i);
                // pressure = dayForecast.getDouble(OWM_PRESSURE);
                // humidity = dayForecast.getInt(OWM_HUMIDITY);
                // Log.e(LOG_TAG, "Right Now, for day ["+i+"] humidity is " + humidity);
                // windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                // windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);
                // description = weatherObject.getString(OWM_DESCRIPTION);
                // weatherId = weatherObject.getInt(OWM_WEATHER_ID);
                // high = temperatureObject.getDouble(OWM_MAX);
                // low = temperatureObject.getDouble(OWM_MIN);

                WeatherDisplayDO weatherData = new WeatherDisplayDO();
                weatherData.setDateTime(getWeatherDate(i));
                weatherData.setPressure(dayForecast.getDouble(OWM_PRESSURE));
                weatherData.setHumidity(dayForecast.getInt(OWM_HUMIDITY));
                weatherData.setWindSpeed(dayForecast.getDouble(OWM_WINDSPEED));
                weatherData.setWindDirection(dayForecast.getDouble(OWM_WIND_DIRECTION));
                weatherData.setDescription(weatherObject.getString(OWM_DESCRIPTION));
                weatherData.setWeatherId(weatherObject.getInt(OWM_WEATHER_ID));
                weatherData.setHigh(temperatureObject.getDouble(OWM_MAX));
                weatherData.setLow(temperatureObject.getDouble(OWM_MIN));

                cVVector.add(newWeatherDataCV(locationId, weatherData));
            }
            return cVVector;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
    
    private long saveLocationData(JSONObject cityJson, String locationSetting){
        try {
            String cityName = cityJson.getString(OWM_CITY_NAME);
            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
            return addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
        } catch (Exception exc) { Log.e(LOG_TAG, "Something wrong happnednd " + exc.getMessage()); }
        return 0;
    }

    private int insertContentValuesIntoDB(Vector<ContentValues> contentValues){
        int inserted = 0 ;
        // add to database
        if ( contentValues != null && contentValues.size() > 0 ) {
            ContentValues[] cv_array = new ContentValues[contentValues.size()];
            contentValues.toArray(cv_array);
            inserted = myContentResolver
                    .bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cv_array);

            // Delete old data
            myContentResolver.delete(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ? ",
                    new String[]{Long.toString(getWeatherDate(-1))});
        }
        hit = true;
        return inserted;
    }

    private void verifyDbValues(Vector<ContentValues> contentValues,
                                String locationSetting){

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri =
                WeatherContract.WeatherEntry.
                        buildWeatherLocationWithStartDate(
                                locationSetting,
                                System.currentTimeMillis());

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert

        Cursor cur = myContentResolver.query(weatherForLocationUri,
                null, null, null, sortOrder);

        Vector<ContentValues> dbValues;
        if (cur != null) {
            dbValues = new Vector<>(cur.getCount());
            if ( cur.moveToFirst() ) {
                do {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cur, cv);
                    dbValues.add(cv);
                } while (cur.moveToNext());
            }
            if (contentValues.equals(dbValues)){
                Log.e(LOG_TAG, "Data inserted correctly");
                Log.w(LOG_TAG, "\nCV values" + contentValues.toString());
                Log.w(LOG_TAG, "\nDB values" + dbValues.toString());

                // TODO : Return a boolean value representing their equality
            }
        }
    }

    private long getWeatherDate(int count){
        Time dayTime = new Time();
        dayTime.setToNow();
        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        // now we work exclusively in UTC
        dayTime = new Time();
        return dayTime.setJulianDay(julianStartDay+count);
    }

    private ContentValues newWeatherDataCV(long locationId, WeatherDisplayDO weather){
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, weather.getDateTime());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, weather.getHumidity());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, weather.getPressure());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, weather.getWindSpeed());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, weather.getWindDirection());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, weather.getHigh());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, weather.getLow());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, weather.getDescription());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weather.getWeatherId());
        return weatherValues;
    }

    SunshineRepositoryUtil setMyContentResolver(ContentResolver myContentResolver) {
        this.myContentResolver = myContentResolver;
        if ( myContentResolver == null ) { Log.e(LOG_TAG, "Content resolver is null... please check"); }
        return this;
    }
}
