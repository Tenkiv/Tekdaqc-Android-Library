package com.tenkiv.tekdaqc.data;

import android.util.Log;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by ideal on 6/6/14.
 */
public class SessionSet {

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
