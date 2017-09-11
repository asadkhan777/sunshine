package com.example.asadkhan.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.example.asadkhan.sunshine.MainForecastActivity;
import com.example.asadkhan.sunshine.R;
import com.example.asadkhan.sunshine.Utility;
import com.example.asadkhan.sunshine.data.WeatherContract;


public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    public final static String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute)  180 = 3 hours
    // public static final int SYNC_INTERVAL = 180 * 60;
    public static final int SYNC_INTERVAL = 30;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    // Notifications
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.e(LOG_TAG, "onPerformSync Called.");
        getData();
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if ( accountManager == null )
        { Log.e(LOG_TAG, "Account manager is null! Please check."); return null;}

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        }
        return newAccount;
    }

    private void getData(){
        Context context = getContext();
        if ( context == null) { Log.e(LOG_TAG, "Null context not allowed"); return; }
        if ( context.getContentResolver() == null) { Log.e(LOG_TAG, "Null ContentResolver not allowed"); return; }

        String location = Utility.getPreferredLocation(context);
        SunshineRepositoryUtil repositoryUtil =
                new SunshineRepositoryUtil()
                .setMyContentResolver(context.getContentResolver());
        Boolean dataRefreshed = repositoryUtil.getDataFromAPI(location);
        if ( dataRefreshed ) { notifyWeather(); }
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if ( account == null ) {Log.e(LOG_TAG, "Account is null"); return;}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        try { getSyncAccount(context); }
        catch (Exception e) { Log.e(LOG_TAG, e.getMessage()); }
    }

    private void notifyWeather(){
        Context context = getContext();
        Cursor cursor = null;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = pref.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( !displayNotifications ) { Log.d(LOG_TAG, "Notifications unwanted"); return;}

        String lastNotified = context.getString(R.string.pref_last_notification);
        long lastSync = pref.getLong(lastNotified, 0);
        try {
            long now = System.currentTimeMillis();
            if ((now - lastSync) >= DAY_IN_MILLIS) {
                String locationQ = Utility.getPreferredLocation(context);
                Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQ, now);
                cursor = context.getContentResolver().query(
                        weatherUri,
                        NOTIFY_WEATHER_PROJECTION,
                        null,
                        null,
                        null);

                if (cursor == null) { Log.e(LOG_TAG, "Cursor returned null .... pls check"); return; }

                if (cursor.moveToFirst()) {
                    int weatherID = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);
                    cursor.close();
                    cursor = null;
                    boolean isMetric = Utility.isMetric(context);
                    String contentTex = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high, isMetric),
                            Utility.formatTemperature(context, low, isMetric));

                    int iconId = Utility.getIconResourceForWeatherCondition(weatherID);
                    String title = context.getString(R.string.app_name);

                    //build your notification here.

                    NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                            .setSmallIcon(iconId)
                            .setContentTitle(title)
                            .setContentText(contentTex);

                    Intent resultIntent = new Intent(context, MainForecastActivity.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.setContentIntent(resultPendingIntent);
                    NotificationManager notificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager == null) {
                        Log.e(LOG_TAG, "Notification manager is null ");
                        return;
                    }

                    notificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
                    //refreshing last sync
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong(lastNotified, System.currentTimeMillis());
                    editor.apply();
                }
            }
        }

        catch (Exception exc) { Log.e(LOG_TAG, "Some error occurred " + exc.getMessage()); }

        finally {
            if ( cursor != null ) { if ( !cursor.isClosed() ) { cursor.close(); } }
        }
    }
}