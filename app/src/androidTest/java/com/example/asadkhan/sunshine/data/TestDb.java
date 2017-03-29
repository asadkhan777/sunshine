/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.asadkhan.sunshine.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    private static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    private void deleteTheDatabase() {
        Log.e(LOG_TAG, "And we deleted the DB");
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        Log.e(LOG_TAG, "Run the setup");
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        c.close();
        db.close();
    }

    /*
        You'll want to look in TestUtilities where you can uncomment out the
        "createNorthPoleLocationValues" function.
    */
    public void testLocationTable() {
        // Create ContentValues of what you want to insert
        // (you can use the createNorthPoleLocationValues if you wish)
        // Insert ContentValues into database and get a row ID back
        ContentValues cvl = TestUtilities.createNorthPoleLocationValues();
        long np_location_row_id = insertLocation(cvl);
        //Log.e(LOG_TAG, "\nRow ID for inserted loc = " + np_location_row_id);
    }

    /*
        You'll want to look in TestUtilities where you can use the "createWeatherValues" function.
     */
    public void testWeatherTable() {
        // First insert the location, and then use the locationRowId to insert
        // the weather. Make sure to cover as many failure cases as you can.
        ContentValues default_loc_cvl = TestUtilities.createNorthPoleLocationValues();
        long np_location_row_id = insertLocation(default_loc_cvl);
        ContentValues test_weather_cvl = TestUtilities.createWeatherValues(np_location_row_id);
        long np_weather_row_id = insertWeather(test_weather_cvl);
        //Log.e(LOG_TAG, "Row ID for inserted Weather = " + np_weather_row_id);
    }

    /*
        Private helper methods for test cases
     */
    private long insertLocation(ContentValues location_cvl) {
        // Students: This is a helper method for the testWeatherTable quiz. You can move your
        // code from testLocationTable to here so that you can call this code from both
        //  testWeatherTable and testLocationTable.

        // First step: Get reference to writable database
        SQLiteDatabase db;
        long locationRowId = 0;
        Cursor cursor_location;
        try {
            db = new WeatherDbHelper(
                    this.mContext).getWritableDatabase();
            assertEquals(true, db.isOpen());

            // insert our test records into the database

            locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, location_cvl);

            // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
            // the round trip.

            // Verify we got a row back.
            assertTrue("Error: Failure to insert Location Values", locationRowId != -1);

            // Fourth Step: Query the database and receive a Cursor back
            // A cursor_location is your primary interface to the query results.
            cursor_location = db.query(
                    WeatherContract.LocationEntry.TABLE_NAME,  // Table to Query
                    null, // all columns
                    null, // Columns for the "where" clause
                    null, // Values for the "where" clause
                    null, // columns to group by
                    null, // columns to filter by row groups
                    null // sort order
            );
            db.close();

            // Move the cursor_location to a valid database row and check to see if we got any records back
            // from the query
            assertTrue( "Error: No Records returned from location query", cursor_location.moveToFirst() );

            // Fifth Step: Validate data in resulting Cursor with the original ContentValues
            // (you can use the validateCurrentRecord function in TestUtilities to validate the
            // query if you like)
            TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed",
                    cursor_location, location_cvl);

            // Move the cursor_location to demonstrate that there is only one record in the database
            assertFalse( "Error: More than one record returned from location query",
                    cursor_location.moveToNext() );

            // Sixth Step: Close Cursor and Database
            cursor_location.close();

        } catch (Exception e){
            cursor_location = null;
            db = null;
        }
        finally {
            cursor_location = null;
            db = null;
        }
        assertTrue("Insert Loc method failed, got a 0", (locationRowId != 0));
        return locationRowId;
    }

    private long insertWeather(ContentValues weather_cvl) {
        SQLiteDatabase db;
        long weatherRowId = 0;
        Cursor cursor_weather;
        try {
            // First step: Get reference to writable database
            db = new WeatherDbHelper(
                    this.mContext).getWritableDatabase();
            assertEquals(true, db.isOpen());

            // insert our test records into the database
            weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weather_cvl);

            // Verify we got a row back.
            assertTrue("Error: Failure to insert Location Values", weatherRowId != -1);

            // Fourth Step: Query the database and receive a Cursor back
            // A cursor is your primary interface to the query results.
            cursor_weather = db.query(
                    WeatherContract.WeatherEntry.TABLE_NAME,  // Table to Query
                    null, // all columns
                    null, // Columns for the "where" clause
                    null, // Values for the "where" clause
                    null, // columns to group by
                    null, // columns to filter by row groups
                    null // sort order
            );
            db.close();

            // Move the cursor_location to a valid database row and check to see if we got any records back
            // from the query
            assertTrue( "Error: No Records returned from location query", cursor_weather.moveToFirst() );

            TestUtilities.validateCursor("Weather Cursor done fucked up", cursor_weather, weather_cvl);

            // Move the cursor to demonstrate that there is only one record in the database
            assertFalse( "Error: More than one record returned from location query",
                    cursor_weather.moveToNext() );

            cursor_weather.close();

        }  catch (Exception e) {
            cursor_weather = null;
            db = null;
        }
        finally {
            cursor_weather = null;
            db = null;
        }
        // Can also return id of inserted row
        // return cursor_weather.getLong(0);
        assertTrue("Insert Weather method failed, got a 0", (weatherRowId != 0));
        return weatherRowId;
    }

    private String checkdb(Cursor testcur){

        // Test code
        int cols = testcur.getColumnCount();
        String result = "\n";
        Log.e(LOG_TAG, "Row present ? : " + Boolean.toString(testcur.moveToFirst()));
        for (int i = 0; i < cols; i++) {
            String col_name = testcur.getColumnName(i) + " = ";
            String col_val = testcur.getString(i) + "\n" ;
            String data = col_name + col_val;
            result += data;
        }
        // Log.e(LOG_TAG, result);
        testcur.close();
        return result;
    }
}
