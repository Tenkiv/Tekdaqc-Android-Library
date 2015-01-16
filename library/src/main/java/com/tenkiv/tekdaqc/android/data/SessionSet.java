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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class SessionSet {

    /**
     * Logcat tag
     */
    private static final String TAG = "SessionSet";

    private static final String FIELD_SESSIONS = "sessions";

    /**
     * Collection of {@link TekdaqcSession} containing previous device extractions. There are no thread safety
     * guarantees made by the framework for this other than that when the framework iterates over the collection, it
     * will synchronize on the collection.
     */
    protected final Set<String> sessions;

    private transient final Map<String, TekdaqcSession> sessionMap;

    public SessionSet() {
        sessions = new LinkedHashSet<String>();
        sessionMap = new HashMap<String, TekdaqcSession>();
    }

    public static SessionSet fromJSON(String json) {
        Log.d(TAG, "Reading session from json.");
        final SessionSet instance = new SessionSet();
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(json).getAsJsonObject();
        Log.d(TAG, "JSON Object Element Count: " + object.entrySet().size());
        JsonArray sessionNames = object.getAsJsonArray(FIELD_SESSIONS);
        for (int i = 0; i < sessionNames.size(); ++i) {
            final String uuid = sessionNames.get(i).getAsString();
            instance.sessions.add(uuid);
        }
        return instance;
    }

    /**
     * Retrieve the number of {@link TekdaqcSession} objects stored.
     *
     * @return int The count.
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Retrieve an {@link TekdaqcSession} by it UUID.
     *
     * @param uuid {@link String} The session UUID.
     * @return {@link TekdaqcSession} The retrieved session.
     */
    public TekdaqcSession getTekdaqcInteractByUUID(String uuid) {
        if (uuid == null) throw new IllegalArgumentException("Provided UUID must not be null.");
        if (uuid.isEmpty()) throw new IllegalArgumentException("Provided UUID must not be empty.");

        for (String session : sessions) {
            if (uuid.equalsIgnoreCase(session)) {
                return sessionMap.get(uuid);
            }
        }
        return null;
    }

    public void writeJSON(OutputStream output) {
        Log.d(TAG, "Writing JSON for session set...");
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

    @Override
    public String toString() {
        String ret = "";
        Field ms[] = SessionSet.class.getDeclaredFields();
        for (Field f : ms) {
            try {
                ret += "Field Name: " + f.getName() + "  |  Field Value: " + f.get(this) + "\n";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * Adds an {@link TekdaqcSession} to the list of sessions.
     *
     * @param session {@link TekdaqcSession} to add.
     */
    public void addTekdaqcSession(TekdaqcSession session) {
        sessions.add(session.uuid);
        sessionMap.put(session.uuid, session);
    }

    /**
     * Removes an {@link TekdaqcSession} from the list of sessions.
     *
     * @param session {@link TekdaqcSession} to be removed.
     * @return boolean True if the list of sessions was modified.
     */
    public boolean removeTekdaqcSession(TekdaqcSession session) {
        return sessions.remove(session.uuid);
    }

    /**
     * Removes an {@link TekdaqcSession} from the list of sessions.
     *
     * @param uuid String The index to be removed.
     */
    public void removeTekdaqcSessionByUUID(String uuid) {
        sessions.remove(uuid);
    }

    /**
     * Removes all {@link TekdaqcSession}s from the collection of sessions.
     */
    public void removeAllTekdaqcSessions() {
        sessions.clear();
    }

    /**
     * Retrieve a list of all session UUIDs.
     *
     * @return {@link ArrayList} of the UUID {@link String}s.
     */
    public ArrayList<String> getUUIDList() {
        synchronized (sessions) {
            final ArrayList<String> list = new ArrayList<String>(sessions.size());
            for (String s : sessions) {
                list.add(s);
            }
            return list;
        }
    }

    /**
     * Returns a shallow copy of the session list, allowing for safe iteration outside of this class.
     *
     * @return {@link List} of {@link TekdaqcSession} objects.
     */
    public List<TekdaqcSession> getTekdaqcSessionListCopy() {
        final List<TekdaqcSession> list = new ArrayList<TekdaqcSession>(sessions.size());
        synchronized (sessions) {
            for (String s : sessions) {
                list.add(sessionMap.get(s));
            }
        }
        return list;
    }
}
