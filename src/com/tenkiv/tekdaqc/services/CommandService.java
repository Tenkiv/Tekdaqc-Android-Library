package com.tenkiv.tekdaqc.services;

import android.app.IntentService;
import android.content.Intent;

/**
 * @author Jared Woolston (jwoolston@tenkiv.com)
 */
public class CommandService extends IntentService {

    public CommandService() {
        super("Tekdaqc Command Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
