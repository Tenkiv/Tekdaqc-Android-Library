/**
 * Copyright 2013 Tenkiv, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tenkiv.tekdaqc.android.content;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content provider for the Tekdaqc. Provides a persistent data store for analog
 * data and calibration data. By default, this provider uses
 * {@link DefaultTekdaqcDatabaseHelper} as its internal helper. If desired, user
 * code can override this by calling {@link #setDatabaseHelperClass(Class)}
 * <b>PRIOR</b> to the content provider being loaded.
 * 
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class TekdaqcDataProvider extends ContentProvider {

	/**
	 * URI Matching indices
	 */
	private static final int ANALOG_INPUT_DATA = 1;
	private static final int ANALOG_INPUT_DATA_POINT = 2;
	private static final int CALIBRATION_DATA = 3;
	private static final int CALIBRATION_DATA_POINT = 4;
	private static final int CALIBRATION_TEMPS = 5;

	/**
	 * List of columns which are available for query/insert of calibration data
	 */
	private static final String[] CALIBRATION_AVAILABLE_COLUMNS = { TekdaqcDataProviderContract.COLUMN_ID,
			TekdaqcDataProviderContract.COLUMN_SERIAL, TekdaqcDataProviderContract.COLUMN_TIME, TekdaqcDataProviderContract.COLUMN_GAIN,
			TekdaqcDataProviderContract.COLUMN_RATE, TekdaqcDataProviderContract.COLUMN_BUFFER, TekdaqcDataProviderContract.COLUMN_SCALE,
			TekdaqcDataProviderContract.COLUMN_READ_VOLTAGE, TekdaqcDataProviderContract.COLUMN_NOMINAL_VOLATGE,
			TekdaqcDataProviderContract.COLUMN_CORRECTION_FACTOR, TekdaqcDataProviderContract.COLUMN_TEMPERATURE };

	/**
	 * List of columns which are available for query/insert of analog input data
	 */
	private static final String[] ANALOG_INPUT_AVAILABLE_COLUMNS = { TekdaqcDataProviderContract.COLUMN_ID,
			TekdaqcDataProviderContract.COLUMN_SERIAL, TekdaqcDataProviderContract.COLUMN_TIMESTAMP,
			TekdaqcDataProviderContract.COLUMN_TIME, TekdaqcDataProviderContract.COLUMN_ANALOG_COUNTS,
			TekdaqcDataProviderContract.COLUMN_GAIN, TekdaqcDataProviderContract.COLUMN_RATE, TekdaqcDataProviderContract.COLUMN_BUFFER,
			TekdaqcDataProviderContract.COLUMN_SCALE };
	
	private static final String[] TEMPERATURE_COLUMNS = { TekdaqcDataProviderContract.COLUMN_ID,
		TekdaqcDataProviderContract.COLUMN_SERIAL, TekdaqcDataProviderContract.COLUMN_TIMESTAMP,
		TekdaqcDataProviderContract.COLUMN_TIME, TekdaqcDataProviderContract.COLUMN_TEMPERATURE};

	private static final List<String> CALIBRATION_AVAILABLE_COLUMNS_LIST = Arrays.asList(CALIBRATION_AVAILABLE_COLUMNS);
	private static final List<String> ANALOG_INPUT_AVAILABLE_COLUMNS_LIST = Arrays.asList(ANALOG_INPUT_AVAILABLE_COLUMNS);
	private static final List<String> TEMPERATURE_COLUMNS_LIST = Arrays.asList(TEMPERATURE_COLUMNS);
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final AtomicBoolean IS_HELPER_SET = new AtomicBoolean(false);
	private static final String TAG = "TekdaqcDataProvider";

	/**
	 * Setup the URI matches
	 */
	static {
		sURIMatcher.addURI(TekdaqcDataProviderContract.AUTHORITY, TekdaqcDataProviderContract.DISTINCT_TEMPS_PATH, CALIBRATION_TEMPS);
		sURIMatcher.addURI(TekdaqcDataProviderContract.AUTHORITY, TekdaqcDataProviderContract.ANALOG_INPUT_DATA_PATH, ANALOG_INPUT_DATA);
		sURIMatcher.addURI(TekdaqcDataProviderContract.AUTHORITY, TekdaqcDataProviderContract.ANALOG_INPUT_DATA_PATH + "/#",
				ANALOG_INPUT_DATA_POINT);
		sURIMatcher.addURI(TekdaqcDataProviderContract.AUTHORITY, TekdaqcDataProviderContract.CALIBRATION_DATA_PATH, CALIBRATION_DATA);
		sURIMatcher.addURI(TekdaqcDataProviderContract.AUTHORITY, TekdaqcDataProviderContract.CALIBRATION_DATA_PATH + "/#",
				CALIBRATION_DATA_POINT);
	}

	/**
	 * The class to instantiate as the internal database helper.
	 */
	private static Class<? extends ATekdaqcDatabaseHelper> sHelperType = DefaultTekdaqcDatabaseHelper.class;

	/**
	 * Internal reference to the database helper.
	 */
	private ATekdaqcDatabaseHelper mDatabase;

	/**
	 * Set's the specific class to use for the content provider's database
	 * helper.
	 * 
	 * @param helper
	 *            {@link Class} extending {@link ATekdaqcDatabaseHelper} to
	 *            instantiate for a database helper.
	 * @return {@link boolean} true if the helper type was set sucessfully,
	 *         false if not (because it has already been set).
	 */
	public static synchronized boolean setDatabaseHelperClass(Class<? extends ATekdaqcDatabaseHelper> helper) {
		if (IS_HELPER_SET.get()) {
			return false;
		} else {
			sHelperType = helper;
			IS_HELPER_SET.set(true);
			return true;
		}
	}

	@Override
	public synchronized boolean onCreate() {
		try {
			IS_HELPER_SET.set(true);
			mDatabase = (ATekdaqcDatabaseHelper) sHelperType.getConstructor(Context.class).newInstance(getContext());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// Uisng SQLiteQueryBuilder instead of query() method
		final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		final int uriType = sURIMatcher.match(uri);
		// Check if the caller has requested a column which does not exists
		checkColumns(projection, uriType);
		switch (uriType) {
			case ANALOG_INPUT_DATA:
				// Set the table
				queryBuilder.setTables(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA);
				break;
			case ANALOG_INPUT_DATA_POINT:
				// Set the table
				queryBuilder.setTables(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA);
				// Adding the ID to the original query
				queryBuilder.appendWhere(TekdaqcDataProviderContract.COLUMN_ID + "=" + uri.getLastPathSegment());
				break;
			case CALIBRATION_DATA:
				// Set the table
				queryBuilder.setTables(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA);
				break;
			case CALIBRATION_DATA_POINT:
				// Set the table
				queryBuilder.setTables(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA);
				// Adding the ID to the original query
				queryBuilder.appendWhere(TekdaqcDataProviderContract.COLUMN_ID + "=" + uri.getLastPathSegment());
				break;
			case CALIBRATION_TEMPS:
				// Set the table
				queryBuilder.setTables(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA);
				// Do a distinct temperature query
				queryBuilder.setDistinct(true);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		final SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		if (sqlDB == null)
			throw new IllegalStateException("Query received a null database object.");
		Log.d(TAG, "Querying with selection statement: " + selection);
		final Cursor cursor = queryBuilder.query(sqlDB, projection, selection, selectionArgs, null, null, sortOrder);
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public synchronized String getType(Uri uri) {
		return null;
	}

	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		final int uriType = sURIMatcher.match(uri);
		final SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		if (sqlDB == null)
			throw new IllegalStateException("Insert received a null database object.");
		long id = 0;
		switch (uriType) {
			case ANALOG_INPUT_DATA:
				id = sqlDB.insert(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, null, values);
			case CALIBRATION_DATA:
				id = sqlDB.insert(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, null, values);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(TekdaqcDataProviderContract.BASE_PATH + "/" + id);
	}

	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriType = sURIMatcher.match(uri);
		final SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		if (sqlDB == null)
			throw new IllegalStateException("Delete received a null database object.");
		int rowsDeleted = 0;
		String id = null;
		switch (uriType) {
			case ANALOG_INPUT_DATA:
				rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, selection, selectionArgs);
				break;
			case ANALOG_INPUT_DATA_POINT:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, TekdaqcDataProviderContract.COLUMN_ID + "="
							+ id, null);
				} else {
					rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, TekdaqcDataProviderContract.COLUMN_ID + "="
							+ id + " and " + selection, selectionArgs);
				}
				break;
			case CALIBRATION_DATA:
				rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, selection, selectionArgs);
				break;
			case CALIBRATION_DATA_POINT:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, TekdaqcDataProviderContract.COLUMN_ID + "="
							+ id, null);
				} else {
					rowsDeleted = sqlDB.delete(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, TekdaqcDataProviderContract.COLUMN_ID + "="
							+ id + " and " + selection, selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		final int uriType = sURIMatcher.match(uri);
		final SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		if (sqlDB == null)
			throw new IllegalStateException("Update received a null database object.");
		int rowsUpdated = 0;
		String id = null;
		switch (uriType) {
			case ANALOG_INPUT_DATA:
				rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, values, selection, selectionArgs);
				break;
			case ANALOG_INPUT_DATA_POINT:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, values, TekdaqcDataProviderContract.COLUMN_ID
							+ "=" + id, null);
				} else {
					rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_ANALOG_INPUT_DATA, values, TekdaqcDataProviderContract.COLUMN_ID
							+ "=" + id + " and " + selection, selectionArgs);
				}
				break;
			case CALIBRATION_DATA:
				rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, values, selection, selectionArgs);
				break;
			case CALIBRATION_DATA_POINT:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, values, TekdaqcDataProviderContract.COLUMN_ID
							+ "=" + id, null);
				} else {
					rowsUpdated = sqlDB.update(ATekdaqcDatabaseHelper.TABLE_CALIBRATION_DATA, values, TekdaqcDataProviderContract.COLUMN_ID
							+ "=" + id + " and " + selection, selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection, int uriType) {
		List<String> compare = null;
		switch (uriType) {
			case ANALOG_INPUT_DATA:
			case ANALOG_INPUT_DATA_POINT:
				compare = ANALOG_INPUT_AVAILABLE_COLUMNS_LIST;
				break;
			case CALIBRATION_DATA:
			case CALIBRATION_DATA_POINT:
				compare = CALIBRATION_AVAILABLE_COLUMNS_LIST;
				break;
			case CALIBRATION_TEMPS:
				compare = TEMPERATURE_COLUMNS_LIST;
				break;
		}
		if (projection != null) {
			for (String s : projection) {
				if (!compare.contains(s)) {
					throw new IllegalArgumentException("Unknown columns in projection");
				}
			}
		}
	}
}
