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
package com.tenkiv.tekdaqc.android.data;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.ATekdaqc.ANALOG_SCALE;

/**
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class TekdaqcSession {

    /**
     * Logcat tag
     */
    private static final String TAG = "TekDAQC Session";

    private static final String FIELD_UUID = "uuid";

    public String uuid;
    public float progress;
    public String currentStep;
    public ANALOG_SCALE currentScale;
    public transient ATekdaqc tekdaqc;

    private TekdaqcSession() {
        // Do nothing
    }

    public TekdaqcSession(String uuid) {
        Log.d(TAG, "Creating new TekDAQC Session with UUID: " + uuid);
        this.uuid = uuid;
    }

    public static TekdaqcSession fromJSON(String json) {
        Log.d(TAG, "Reading session from json.");
        final TekdaqcSession instance = new TekdaqcSession();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json);
        Log.d(TAG, "Element: " + element);
        //Log.d(TAG, "JSON Object Element Count: " + object.entrySet().size());
        //instance.uuid = object.getAsJsonPrimitive(FIELD_UUID).getAsString();
        return instance;
    }

    public void writeJSON(OutputStream output) {
        Log.d(TAG, "Writing JSON for session...");
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String json = gson.toJson(this);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(output);
            out.write(json.getBytes());
            out.flush();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
