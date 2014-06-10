package com.tenkiv.tekdaqc;

import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by ideal on 6/9/14.
 */
public class PollingReadExecutor {

    private static final String TAG = "PollingReadExecutor";
    private static final String POLLING_THREAD_NAME = "TEKDAQC_POLLING_THREAD";
    private static final int POLLING_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;

    private final Map<String, TekdaqcCommunicationSession> mSessions;

    private final ExecutorService mExecutor;
    private final Future<Void> mTaskFuture;

    public PollingReadExecutor(Map<String, TekdaqcCommunicationSession> sessions) {
        if (sessions == null) throw new IllegalArgumentException("Session set cannot be null.");
        mSessions = sessions;
        mExecutor = Executors.newSingleThreadExecutor();
        mTaskFuture = mExecutor.submit(new Task());
        Log.d(TAG, "Polling executor task submitted.");
    }

    private final class Task implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            Thread.currentThread().setName(POLLING_THREAD_NAME);
            Thread.currentThread().setPriority(POLLING_THREAD_PRIORITY);
            while (true) {
                final Set<String> keySet = mSessions.keySet();
                Log.d(TAG, "Polling: Key Set Size: " + keySet.size());
                for (String key : keySet) {
                    Log.d(TAG, "Polling board: " + key);
                    TekdaqcCommunicationSession session = mSessions.get(key);
                    final BufferedReader in = session.getBufferedReader();
                    String data;
                    while ((data = in.readLine()) != null) {
                        Log.d("", data);
                    }
                }
            }
        }
    }
}
