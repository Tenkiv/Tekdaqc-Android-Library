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

import android.net.Uri;

/**
 * Content Provider contract for the calibration data content provider.
 *
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public interface TekdaqcDataProviderContract {

    public static final String AUTHORITY = "com.tenkiv.tekdaqc.android.data.provider";
    public static final String BASE_PATH = "data";
    public static final String ANALOG_INPUT_DATA_PATH = BASE_PATH + "/analog_input";
    public static final String CALIBRATION_DATA_PATH = BASE_PATH + "/calibration";
    public static final String DISTINCT_TEMPS_PATH = BASE_PATH + "/temps";
    public static final Uri ANALOG_INPUT_DATA_URI = Uri.parse("content://" + AUTHORITY + "/" + ANALOG_INPUT_DATA_PATH);
    public static final Uri CALIBRATION_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CALIBRATION_DATA_PATH);
    public static final Uri TEMPERATURE_URI = Uri.parse("content://" + AUTHORITY + "/" + DISTINCT_TEMPS_PATH);

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SERIAL = "serial";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_GAIN = "gain";
    public static final String COLUMN_RATE = "rate";
    public static final String COLUMN_BUFFER = "buffer";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_SCALE = "scale";
    public static final String COLUMN_READ_VOLTAGE = "read_voltage";
    public static final String COLUMN_NOMINAL_VOLATGE = "nominal_voltage";
    public static final String COLUMN_CORRECTION_FACTOR = "correction_factor";
    public static final String COLUMN_ANALOG_COUNTS = "analog_counts";
    public static final String COLUMN_TIMESTAMP = "timestamp";
}
