package com.tenkiv.tekdaqc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by ideal on 6/10/14.
 */
public class ParsingExecutor {

    private static final String TAG = "ParsingExecutor";
    private static final String PARSING_THREAD_NAME = "TEKDAQC_PARSING_THREAD";
    private static final int PARSING_THREAD_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND;

    private final ExecutorService mExecutor;

    public ParsingExecutor(int numThreads) {
        mExecutor = Executors.newFixedThreadPool(numThreads, new Factory());
    }

    public void parseMessage(String message) {

    }

    private static final class Factory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r);
            thread.setPriority(PARSING_THREAD_PRIORITY);
            thread.setName(PARSING_THREAD_NAME);
            return thread;
        }
    }
}
