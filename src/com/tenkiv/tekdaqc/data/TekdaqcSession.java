package com.tenkiv.tekdaqc.data;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tenkiv.tekdaqc.ATekDAQC;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by ideal on 6/6/14.
 */
public class TekdaqcSession {

    private static final String TAG = "TekDAQC Session";

    private static final String FIELD_UUID = "uuid";

    public String uuid;
    public transient ATekDAQC tekdaqc;

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
