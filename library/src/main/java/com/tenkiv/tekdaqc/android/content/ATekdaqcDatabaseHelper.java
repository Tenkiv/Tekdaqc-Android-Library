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

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

/**
 * Abstract SQLLite Database helper implementation for the Tekdaqc data database.
 *
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public abstract class ATekdaqcDatabaseHelper extends SQLiteOpenHelper {

	/**
	 * The default database name
	 */
    public static final String DEFAULT_DATABASE_NAME = "TEKDAQC_DATA.db";
    
    /**
     * The default database file directory
     */
    public static final String DEFAULT_FILE_DIR = Environment.getExternalStorageDirectory()
            + File.separator + "TEKDAQC_DATA" + File.separator;
    
    /**
     * The default database version
     */
    private static final int DEFAULT_DATABASE_VERSION = 1;

    /**
     * The calibration data table name
     */
    public static final String TABLE_CALIBRATION_DATA = "calibration_data";
    
    /**
     * The analog data table name
     */
    public static final String TABLE_ANALOG_INPUT_DATA = "analog_input_data";
    
    // Database creation sql statement
    //@formatter:off
    /**
     * SQL Creation statement for calibration data
     */
    private static final String TABLE_CALIBRATION_CREATE = "create table "
            + TABLE_CALIBRATION_DATA + "("
            + TekdaqcDataProviderContract.COLUMN_ID + " integer primary key autoincrement, "
            + TekdaqcDataProviderContract.COLUMN_SERIAL + " text not null, "
            + TekdaqcDataProviderContract.COLUMN_TIME + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_GAIN + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_RATE + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_BUFFER + " text not null, "
            + TekdaqcDataProviderContract.COLUMN_SCALE + " text not null, "
            + TekdaqcDataProviderContract.COLUMN_TEMPERATURE + " real not null, "
            + TekdaqcDataProviderContract.COLUMN_READ_VOLTAGE + " real not null, "
            + TekdaqcDataProviderContract.COLUMN_NOMINAL_VOLATGE + " real not null, "
            + TekdaqcDataProviderContract.COLUMN_CORRECTION_FACTOR + " real not null"
            + ");";
    
    /**
     * SQL Creation statement for analog input data
     */
    private static final String TABLE_ANALOG_INPUT_DATA_CREATE = "create table "
    		+ TABLE_ANALOG_INPUT_DATA + "("
    		+ TekdaqcDataProviderContract.COLUMN_ID + " integer primary key autoincrement, "
    		+ TekdaqcDataProviderContract.COLUMN_SERIAL + " text not null, "
    		+ TekdaqcDataProviderContract.COLUMN_TIMESTAMP + " integer not null, "
    		+ TekdaqcDataProviderContract.COLUMN_TIME + " integer not null, "
    		+ TekdaqcDataProviderContract.COLUMN_ANALOG_COUNTS + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_GAIN + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_RATE + " integer not null, "
            + TekdaqcDataProviderContract.COLUMN_BUFFER + " text not null, "
            + TekdaqcDataProviderContract.COLUMN_SCALE + " text not null"
    		+ ");";
    //@formatter:on
    
    /**
     * Default constructor for the helper. If the default parameters are to be used, user code can simply 
     * call through to this constructor. If custom parameters are to be used, user code should call the alternative
     * constructor which instead takes them as parameters.
     * 
     * @param context {@link Context} The calling application's context.
     */
    public ATekdaqcDatabaseHelper(Context context) {
        super(context, DEFAULT_FILE_DIR + DEFAULT_DATABASE_NAME, null, DEFAULT_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    	db.execSQL(TABLE_ANALOG_INPUT_DATA_CREATE);
        db.execSQL(TABLE_CALIBRATION_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ATekdaqcDatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANALOG_INPUT_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALIBRATION_DATA);
        onCreate(db);
    }
}
