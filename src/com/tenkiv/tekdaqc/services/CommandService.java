package com.tenkiv.tekdaqc.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * @author Jared Woolston (jwoolston@tenkiv.com)
 */
public class CommandService extends IntentService {

    private static final String TAG = "CommandService";

    public CommandService() {
        super("Tekdaqc Command Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "Pausing for 5 seconds.");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Resuming.");
    }
}
