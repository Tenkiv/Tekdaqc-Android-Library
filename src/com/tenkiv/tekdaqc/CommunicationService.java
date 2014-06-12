package com.tenkiv.tekdaqc;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.tenkiv.tekdaqc.command.Command;
import com.tenkiv.tekdaqc.communication.TekdaqcCommunicationSession;
import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput;
import com.tenkiv.tekdaqc.peripherals.digital.DigitalInput;
import com.tenkiv.tekdaqc.peripherals.digital.DigitalOutput;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommunicationService extends Service {

    protected static final String TAG = CommunicationService.class.getSimpleName();

    private Map<String, TekdaqcCommunicationSession> mCommSessions;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastMgr;

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("TekDAQC Discovery Service", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        mCommSessions = new ConcurrentHashMap<String, TekdaqcCommunicationSession>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null)
            extras = new Bundle();

        extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

        // Run each command in a separate thread.
        final Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    private void notifyOfBoardConnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_CONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    private void notifyOfBoardDisconnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_DISCONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    private void processCommand(ATekDAQC tekdaqc, Command command, Bundle params) throws IOException {
        switch (command) {
            case DISCONNECT:
                tekdaqc.disconnect();
                break;
            case UPGRADE:
                tekdaqc.upgrade();
                break;
            case IDENTIFY:
                tekdaqc.identify();
                break;
            case SAMPLE:
                tekdaqc.sample(params.getInt(TekCast.EXTRA_NUM_SAMPLES, 0));
                break;
            case HALT:
                tekdaqc.halt();
                break;
            case SET_RTC:
                break;
            case SET_USER_MAC:
                break;
            case SET_STATIC_IP:
                break;
            case NONE:
                tekdaqc.none();
                break;
            case LIST_ANALOG_INPUTS:
                tekdaqc.listAnalogInputs();
                break;
            case READ_ADC_REGISTERS:
                break;
            case READ_ANALOG_INPUT:
                break;
            case ADD_ANALOG_INPUT: {
                final AAnalogInput input = (AAnalogInput) params.getSerializable(TekCast.EXTRA_ANALOG_INPUT);
                tekdaqc.addAnalogInput(input);
                break;
            }
            case REMOVE_ANALOG_INPUT: {
                final AAnalogInput input = (AAnalogInput) params.getSerializable(TekCast.EXTRA_ANALOG_INPUT);
                tekdaqc.removeAnalogInput(input);
                break;
            }
            case CHECK_ANALOG_INPUT:
                break;
            case SYSTEM_GAIN_CAL:
                tekdaqc.systemGainCalibrate();
                break;
            case SYSTEM_CAL:
                tekdaqc.systemCalibrate();
                break;
            case LIST_DIGITAL_INPUTS:
                break;
            case READ_DIGITAL_INPUT:
                break;
            case ADD_DIGITAL_INPUT: {
                final DigitalInput input = (DigitalInput) params.getSerializable(TekCast.EXTRA_DIGITAL_INPUT);
                //TODO: Add digital input on tekdaqc
                break;
            }
            case REMOVE_DIGITAL_INPUT: {
                final DigitalInput input = (DigitalInput) params.getSerializable(TekCast.EXTRA_DIGITAL_INPUT);
                //TODO: Remove digital input on tekdaqc
                break;
            }
            case LIST_DIGITAL_OUTPUTS:
                break;
            case SET_DIGITAL_OUTPUT:
                break;
            case READ_DIGITAL_OUTPUT:
                break;
            case ADD_DIGITAL_OUTPUT: {
                final DigitalOutput output = (DigitalOutput) params.getSerializable(TekCast.EXTRA_DIGITAL_OUTPUT);
                //TODO: Add digital output on tekdaqc
                break;
            }
            case REMOVE_DIGITAL_OUTPUT: {
                final DigitalOutput output = (DigitalOutput) params.getSerializable(TekCast.EXTRA_DIGITAL_OUTPUT);
                //TODO: Remove digital output on tekdaqc
                break;
            }
            case CLEAR_DIGITAL_OUTPUT_FAULT:
                break;
        }
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
         * Force a shutdown of the {@link CommunicationService}.
         */
        STOP;
    }

    /**
     * Worker thread for handling incoming {@link DiscoveryService} {@link ServiceAction} requests.
     *
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since 1.0.0.0
     */
    private static final class ServiceHandler extends Handler {

        private CommunicationService mService;

        public ServiceHandler(Looper looper, CommunicationService service) {
            super(looper);
            mService = service;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            final Bundle data = msg.getData();
            final ServiceAction action = ServiceAction.valueOf(data.getString(TekCast.EXTRA_SERVICE_ACTION));
            final ATekDAQC tekdaqc = (ATekDAQC) data.getSerializable(TekCast.EXTRA_TEK_BOARD);
            switch (action) {
                case CONNECT:
                    Log.d(TAG, "Processing CONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc != null) {
                        final TekdaqcCommunicationSession session = new TekdaqcCommunicationSession(tekdaqc);
                        try {
                            session.connect();
                            mService.mCommSessions.put(tekdaqc.getSerialNumber(), session);
                            mService.notifyOfBoardConnection(tekdaqc.getSerialNumber());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Missing required board extra.");
                    }
                    break;
                case DISCONNECT:
                    Log.d(TAG, "Processing DISCONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc != null) {
                        try {
                            final TekdaqcCommunicationSession session = mService.mCommSessions.get(tekdaqc.getSerialNumber());
                            if (session != null) {
                                session.disconnect();
                                mService.notifyOfBoardDisconnection(tekdaqc.getSerialNumber());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Missing required board extra.");
                    }
                    break;
                case COMMAND:
                    if (tekdaqc != null) {
                        final String commandStr = data.getString(TekCast.EXTRA_BOARD_COMMAND);
                        final Command command = Command.toCommand(commandStr);
                        final Bundle params = data.getBundle(TekCast.EXTRA_COMMAND_PARAMS);
                        try {
                            mService.processCommand(tekdaqc, command, params);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case STOP:
                    for (String key : mService.mCommSessions.keySet()) {
                        try {
                            final TekdaqcCommunicationSession session = mService.mCommSessions.get(key);
                            session.disconnect();
                            mService.notifyOfBoardDisconnection(key);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }
}
