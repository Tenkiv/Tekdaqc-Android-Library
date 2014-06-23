package com.tenkiv.tekdaqc.services;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekDAQC;
import com.tenkiv.tekdaqc.ATekDAQC.CONNECTION_METHOD;
import com.tenkiv.tekdaqc.application.TekCast;
import com.tenkiv.tekdaqc.communication.ascii.ASCIICommunicationSession;
import com.tenkiv.tekdaqc.communication.ascii.command.ASCIICommand;
import com.tenkiv.tekdaqc.communication.tasks.ITask;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Android service for communicating with one or more Tekdaqcs. Handles connecting, disconnect, transmission of
 * ommands and receipt of messages/data.
 *
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class CommunicationService extends Service {

    /**
     * Logcat tag.
     */
    protected static final String TAG = CommunicationService.class.getSimpleName();

    /**
     * Map of {@link com.tenkiv.tekdaqc.communication.ascii.ASCIICommunicationSession}s keyed by the associated board's serial number.
     */
    private Map<String, ASCIICommunicationSession> mCommSessions;

    /**
     * @link Looper} for the background thread.
     */
    private Looper mServiceLooper;

    /**
     * {@link Handler} which allows for serialized processing of commands to this {@link Service} in the background.
     */
    private ServiceHandler mServiceHandler;

    /**
     * Application local {@link Intent} broadcast manager.
     */
    private LocalBroadcastManager mLocalBroadcastMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CommunicationService onCreate()");

        // Setup the background thread and its controls
        HandlerThread thread = new HandlerThread("TekDAQC Communication Service", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        // Initialize the session map
        mCommSessions = new ConcurrentHashMap<String, ASCIICommunicationSession>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Extract the service instruction
        final String action = intent.getAction();
        Log.d(TAG, "Received start command: " + action);
        // Build the message parameters
        Bundle extras = intent.getExtras();
        if (extras == null)
            extras = new Bundle();
        extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

        // Run each task in a background thread.
        final Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        Log.d(TAG, "Sending message to background thread.");
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Send shutdown action
        Bundle extras = new Bundle();
        extras.putString(TekCast.EXTRA_SERVICE_ACTION, ServiceAction.STOP.name());
        final Message msg = mServiceHandler.obtainMessage();
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);
        super.onDestroy();
    }

    /**
     * Broadcast through the application that a board has connected.
     *
     * @param serial {@link String} The serial number of the board which has connected.
     */
    private void notifyOfBoardConnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_CONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    /**
     * Broadcast through the application that a board has disconnected.
     *
     * @param serial {@link String} The serial number of the board which has disconnected.
     */
    private void notifyOfBoardDisconnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_DISCONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    /**
     * Processable actions by the {@link CommunicationService}.
     *
     * @author Ian Thomas (toxicbakery@gmail.com)
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since 1.0.0.0
     */
    public static enum ServiceAction {
        /**
         * Connect to a specific TekDAQC board based on the information returned by the {@link DiscoveryService}.
         */
        CONNECT,

        /**
         * Disconnect from a specific TekDAQC board.
         */
        DISCONNECT,

        /**
         * Issue a command to a specific TekDAQC board.
         */
        COMMAND,

        /**
         * Execute a provided {@link ITask}.
         */
        EXECUTE_TASK,

        /**
         * Force a shutdown of the {@link CommunicationService}.
         */
        STOP;
    }

    /**
     * Worker thread {@link Handler} for handling incoming {@link DiscoveryService} {@link ServiceAction} requests.
     *
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since 1.0.0.0
     */
    private static final class ServiceHandler extends Handler {

        /**
         * The {@link CommunicationService} which this {@link Handler} is serving.
         */
        private CommunicationService mService;

        /**
         * Constructor
         *
         * @param looper {@link Looper} The worker thread's {@link Looper}.
         * @param service {@link CommunicationService} The {@link CommunicationService} this {@link Handler} is serving.
         */
        public ServiceHandler(Looper looper, CommunicationService service) {
            super(looper);
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            // Fetch the task parameters
            final Bundle data = msg.getData();
            final ServiceAction action = ServiceAction.valueOf(data.getString(TekCast.EXTRA_SERVICE_ACTION));
            final ATekDAQC tekdaqc = ATekDAQC.getTekdaqcForSerial(data.getString(TekCast.EXTRA_BOARD_SERIAL));
            switch (action) {
                case CONNECT:
                    // Connect to a tekdaqc
                    Log.d(TAG, "Processing CONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc == null) {
                        throw new IllegalArgumentException("Missing required board extra.");
                    } else if (tekdaqc.isConnected()) {
                        throw new IllegalStateException("Board " + tekdaqc.getSerialNumber() + " is already connected!");
                    } else {
                        // Create the communication session
                        final ASCIICommunicationSession session = new ASCIICommunicationSession(tekdaqc);
                        try {
                            // Connect
                        	Log.d(TAG, "Calling session connect method.");
                            session.connect(CONNECTION_METHOD.ETHERNET);
                            // Add this session to the session map
                            Log.d(TAG, "Adding session to the service session map.");
                            mService.mCommSessions.put(tekdaqc.getSerialNumber(), session);
                            // Notify the application that the new board is connected
                            Log.d(TAG, "Notifying service of board connection.");
                            mService.notifyOfBoardConnection(tekdaqc.getSerialNumber());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "Returning from service connect message handler.");
                    break;
                case DISCONNECT:
                    // Disconnect from a tekdaqc
                    Log.d(TAG, "Processing DISCONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc == null) {
                        throw new IllegalArgumentException("Missing required board extra.");
                    } else if (!tekdaqc.isConnected()) {
                        throw new IllegalStateException("Board " + tekdaqc.getSerialNumber() + " is already disconnected!");
                    } else {
                        try {
                            // Retrieve the communication session
                            final ASCIICommunicationSession session = mService.mCommSessions.get(tekdaqc.getSerialNumber());
                            if (session != null) {
                                // Disconnect
                                session.disconnect();
                                // Remove the session from the map
                                mService.mCommSessions.remove(tekdaqc.getSerialNumber());
                                // Notify the application of the disconnect
                                mService.notifyOfBoardDisconnection(tekdaqc.getSerialNumber());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case COMMAND:
                    // Issue a command to a tekdaqc
                    if (tekdaqc == null) {
                        throw new IllegalArgumentException("Missing required board extra.");
                    } else if (!tekdaqc.isConnected()) {
                        throw new IllegalStateException("Board " + tekdaqc.getSerialNumber() + " is not connected!");
                    } else {
                        final ASCIICommunicationSession session = mService.mCommSessions.get(tekdaqc.getSerialNumber());
                        final ASCIICommand command = (ASCIICommand) data.getSerializable(TekCast.EXTRA_BOARD_COMMAND);
                        session.executeCommand(command);
                    }
                    break;
                case EXECUTE_TASK:
                    // Execute an ITask
                    if (tekdaqc == null) {
                        throw new IllegalArgumentException("Missing required board extra.");
                    } else if (!tekdaqc.isConnected()) {
                        throw new IllegalStateException("Board " + tekdaqc.getSerialNumber() + " is not connected!");
                    } else {
                        final ASCIICommunicationSession session = mService.mCommSessions.get(tekdaqc.getSerialNumber());
                        final ITask task = (ITask) data.getSerializable(TekCast.EXTRA_TASK);
                        task.setSession(session);
                        Log.d(TAG, "Calling execute on task: " + task);
                        task.execute((ITaskComplete) data.getSerializable(TekCast.EXTRA_TASK_COMPLETE_CALLBACK));
                    }
                    break;
                case STOP:
                    // Stop execution of this service cleanly
                    for (String key : mService.mCommSessions.keySet()) {
                        try {
                            // Disconnect from all known connected tekdaqc's.
                            final ASCIICommunicationSession session = mService.mCommSessions.get(key);
                            session.disconnect();
                            mService.notifyOfBoardDisconnection(key);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mService.stopSelf();
                    mService.mServiceLooper.quit();
                    break;
            }
        }
    }
}
