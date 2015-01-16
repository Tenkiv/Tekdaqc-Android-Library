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
package com.tenkiv.tekdaqc.android.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.data.SessionSet;
import com.tenkiv.tekdaqc.android.data.TekdaqcSession;
import com.tenkiv.tekdaqc.android.services.DiscoveryService;
import com.tenkiv.tekdaqc.locator.LocatorParams;

/**
 * Custom application class which handles aspects of the library in an Android specific manner.
 * This primarily consists of device discovery.
 *  
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class TekdaqcApplication extends Application {

	/**
	 * Logcat Tag
	 */
    private static final String TAG = "TekdaqcApplication";
    
    /**
     * Synchronization lock for session loading and saving.
     */
    private static final Object SESSION_LOCK = new Object();
    
    /**
     * Path to the session files relative to the root of the private package directory
     */
    private static final String SESSION_SET_FILENAME = "SessionSet";
    private static final String TEKDAQC_SESSION_FILENAME = "tekdaqc_session_";

    /**
     * The application context.
     */
    protected static Context CONTEXT;
    
    /**
     * {@link SessionSet}
     */
    protected SessionSet mSession;

    /**
     * List of known Tekdaqcs
     */
    private List<ATekdaqc> mBoards;
    
    /**
     * Broadcast receiver notified when a board is found by the locator
     */
    private DeviceDiscoveryReceiver mDiscoveryReceiver;
    
    /**
     * Local broadcast manager for sending events
     */
    private LocalBroadcastManager mLocalBroadcastMgr; 

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mBoards = new ArrayList<ATekdaqc>();
        mDiscoveryReceiver = new DeviceDiscoveryReceiver(this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        mLocalBroadcastMgr.registerReceiver(mDiscoveryReceiver, new IntentFilter(TekCast.ACTION_FOUND_BOARD));
        
        CONTEXT = getApplicationContext();
        loadSessionSet();
        if (mSession == null) {
            Log.d(TAG, "Session was null after loading. Creating a new one.");
            mSession = getSession();
            saveSessionSet(false);
        }
    }
    
    public static final Context getTenkivApplicationContext() {
        return CONTEXT;
    }

    /**
     * Clears the known boards list and causes a new device locator packet to be sent. This
     * effectively means that only un-connected boards will known to this list. Boards which
     * were previously discovered and have an active connection will need to be tracked by 
     * the user application.
     */
    public final void refreshDeviceList() {
        mBoards.clear();
        final LocatorParams.Builder builder = new LocatorParams.Builder();
        final Intent intent = new Intent(getApplicationContext(), DiscoveryService.class);
        intent.setAction(DiscoveryService.ServiceAction.SEARCH.toString());
        intent.putExtra(TekCast.EXTRA_LOCATOR_PARAMS, builder.build());
        getApplicationContext().startService(intent);
    }
    
    /**
     * Gets a stream reading from the session file in the private package of the application
     *
     * @param ctx - {@link Context}
     * @return {@link InputStream}
     */
    protected static final InputStream getSessionInputStream(Context ctx) {
        synchronized (SESSION_LOCK) {
            final File inputFile = getOutputFile(ctx, false, SESSION_SET_FILENAME);

            if (!inputFile.exists())
                throw new RuntimeException("Session data file does not yet exist.");

            try {
                final FileInputStream fis = new FileInputStream(inputFile);
                return fis;
            } catch (Exception e) {
                // Attempt to go back in history
            }

            return null;
        }
    }
    
    /**
     * Gets a stream reading from the session file in the private package of the application
     *
     * @param ctx - {@link Context}
     * @return {@link InputStream}
     */
    protected static final InputStream getInteractInputStream(Context ctx, String uuid) {
        synchronized (SESSION_LOCK) {
            final File inputFile = getOutputFile(ctx, true, TEKDAQC_SESSION_FILENAME + uuid);

            if (!inputFile.exists())
                throw new RuntimeException("Session data file does not yet exist.");

            try {
                final FileInputStream fis = new FileInputStream(inputFile);
                return fis;
            } catch (Exception e) {
                // Attempt to go back in history
            }

            return null;
        }
    }
    
    /**
     * Gets a stream writing to the session file in the private package of the application
     *
     * @param ctx - {@link android.content.Context}
     * @return {@link java.io.OutputStream}
     */
    protected static final OutputStream getSessionOutputStream(Context ctx) {
        synchronized (SESSION_LOCK) {
            final FileOutputStream fos;

            try {
                fos = new FileOutputStream(getOutputFile(ctx, false, SESSION_SET_FILENAME));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return fos;
        }
    }

    protected static final OutputStream getTekdaqcSessionOutputStream(Context ctx, String uuid) {
        synchronized (SESSION_LOCK) {
            final FileOutputStream fos;

            try {
                fos = new FileOutputStream(getOutputFile(ctx, true, TEKDAQC_SESSION_FILENAME + uuid));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return fos;
        }
    }
    
    /**
     * Gets a file handle pointing to the session file.
     *
     * @param ctx      {@link Context}
     * @param fileName Defines the file name to be used.
     * @return {@link File} pointing at the session
     */
    private static final File getOutputFile(Context ctx, boolean isSession, String fileName) {
        Log.d(TAG, "Getting output file for name: " + fileName);
        final File file = new File(getStorageDirectory(isSession), fileName + ".json");
        return file;
    }

    public static File getStorageDirectory(boolean isSession) {
        final File extern = Environment.getExternalStorageDirectory();
        final File storage = new File(extern.getAbsolutePath() + "/Tenkiv_Calibration_Data" + ((isSession) ? "/Sessions" : ""));
        storage.mkdirs();
        return storage;
    }
    
    /**
     * Save the {@link SessionSet} to disk optionally saving all known {@link TekdaqcSession} instances. Returns
     * true on success.
     *
     * @param saveAllTekdaqcSessions
     * @return true on success.
     */
    public boolean saveSessionSet(boolean saveAllTekdaqcSessions) {
        Log.d(TAG, "Saving session set.");
        synchronized (SESSION_LOCK) {
            final long start = System.currentTimeMillis();

            OutputStream sessionOutputStream = null;
            try {
                sessionOutputStream = getSessionOutputStream(getApplicationContext());

                if (mSession != null) {
                    mSession.writeJSON(sessionOutputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (sessionOutputStream != null) {
                    try {
                        sessionOutputStream.flush();
                        sessionOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Log.i(TAG, "Saved session in: " + (System.currentTimeMillis() - start));
                    }
                }
            }

            try {
                if (mSession != null && saveAllTekdaqcSessions) {
                    /*
                     * TODO I had attempted to write this with threading launching up to 4 threads for writing. Testing
					 * this lead to very strange and very bad behavior on the GS4 testing with Trackcomm. It would be
					 * nice to thread this though or at least understand why it can not be threaded.
					 */
                    for (String s : mSession.getUUIDList()) {
                        if (s != null) {
                            saveTekdaqcSession(mSession.getTekdaqcInteractByUUID(s));
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    /**
     * Save the given interact session to disk.
     *
     * @param tekdaqcSession session to be saved.
     * @return true on success.
     */
    public boolean saveTekdaqcSession(TekdaqcSession tekdaqcSession) {
        if (tekdaqcSession == null) return false;
        if (tekdaqcSession.uuid == null) return false;
        Log.d(TAG, "Saving TekDAQC Session: " + tekdaqcSession.uuid);
        synchronized (SESSION_LOCK) {
            if (mSession == null) {
                Log.i(TAG, "Aborting interact save request for null interact session.");
                return false;
            }

            final long start = System.currentTimeMillis();

            if (!mSession.getUUIDList().contains(tekdaqcSession.uuid)) {
                Log.i(TAG, "Aborting interact save request; unable to locate interact in session instance.");
                return false;
            }

            OutputStream sessionOutputStream = null;
            try {
                sessionOutputStream = getTekdaqcSessionOutputStream(getApplicationContext(), tekdaqcSession.uuid);
                tekdaqcSession.writeJSON(sessionOutputStream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (sessionOutputStream != null) {
                    try {
                        sessionOutputStream.flush();
                        sessionOutputStream.close();
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Log.i(TAG, "Saved tekdaqc session in: " + (System.currentTimeMillis() - start));
                    }
                }
            }

            return false;
        }
    }

    public SessionSet getSession() {
        Log.d(TAG, "Getting session set");
        if (mSession == null) {
            Log.d(TAG, "Session set was null. Creating.");
            mSession = new SessionSet();
        }
        return mSession;
    }

    /**
     * Load an {@link SessionSet} from disk. If loading of the newest session fails, this will attempt to load
     * sessions up to the limit of backup sessions before failing entirely.
     */
    protected void loadSessionSet() {
        Log.d(TAG, "Loading Session Set.");
        synchronized (SESSION_LOCK) {
            final long start = System.currentTimeMillis();
            try {
                final InputStream inputStream = getSessionInputStream(getApplicationContext());
                if (inputStream != null) {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder json = new StringBuilder();
                    String line = null;
                    try {
                        while (true) {
                            line = reader.readLine();
                            if (line != null) {
                                json.append(line);
                            } else {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mSession = SessionSet.fromJSON(json.toString());

                    // read in the Interacts
                    if (mSession != null) {
                        List<String> uuids = mSession.getUUIDList();
                        for (String uuid : uuids) {
                            try {
                                final InputStream sessionInputStream = getInteractInputStream(
                                        getApplicationContext(), uuid);
                                json = new StringBuilder();
                                line = null;
                                try {
                                    while (true) {
                                        line = reader.readLine();
                                        if (line != null) {
                                            json.append(line);
                                        } else {
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mSession.addTekdaqcSession(TekdaqcSession.fromJSON(json.toString()));
                                sessionInputStream.close();
                                inputStream.close();
                                break;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    inputStream.close();
                    Log.i(TAG, "Loaded session in: " + (System.currentTimeMillis() - start));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Broadcast receiver which will be called when the locator has discovered a board(s).
     * 
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since v1.0.0.0
     */
    private static final class DeviceDiscoveryReceiver extends BroadcastReceiver {

    	/**
    	 * Reference to the owning application.
    	 */
        private final TekdaqcApplication mApplication;

        /**
         * Constructor.
         * 
         * @param app {@link TekdaqcApplication} The calling application.
         */
        DeviceDiscoveryReceiver(TekdaqcApplication app) {
            mApplication = app;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final ATekdaqc board = ATekdaqc.getTekdaqcForSerial(intent.getStringExtra(TekCast.EXTRA_BOARD_SERIAL));
            if (board != null) {
                mApplication.mBoards.add(board);
            }
        }
    }
}
