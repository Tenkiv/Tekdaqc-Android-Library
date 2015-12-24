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
package com.tenkiv.tekdaqc.android.application.util;


import android.os.Bundle;
import android.os.Messenger;


/**
 * Util class of {@link String}s for {@link Messenger} and {@link Bundle} actions and extras.
 */
public interface TekCast {

    // Keys for Bundle Packets.
    public static final String SERVICE_TEKDAQC_REGISTER = "SERVICE_TEKDAQC_REGISTER";
    public static final String SERVICE_SERIAL_KEY = "SERVICE_SERIAL_KEY";
    public static final String SERVICE_COMMAND_KEY = "SERVICE_COMMAND_KEY";
    public static final String SERVICE_TASK_KEY = "SERVICE_TASK_KEY";
    public static final String SERVICE_TEKDAQC_CONNECT = "SERVICE_TEKDAQC_CONNECT";
    public static final String SERVICE_TEKDAQC_DISCONNECT = "SERVICE_TEKDAQC_DISCONNECT";
    public static final String DATA_MESSSAGE_TEKDAQC = "DATA_MESSSAGE_TEKDAQC";
    public static final String DATA_MESSSAGE = "DATA_MESSSAGE";
    public static final String DATA_MESSSAGE_UID = "DATA_MESSSAGE_UID";

    // Inetgers for service Message handling.
    public static final int SERVICE_MSG_REGISTER = 0;
    public static final int SERVICE_MSG_UNREGISTER = 1;
    public static final int SERVICE_MSG_COMMAND = 2;
    public static final int SERVICE_MSG_TASK = 3;
    public static final int SERVICE_MSG_CONNECT = 4;
    public static final int SERVICE_MSG_DISCONNECT = 5;

    // Integers for application side Message handling.
    public static final int TEKDAQC_ERROR_MESSAGE = 0;
    public static final int TEKDAQC_ANALOG_INPUT_MESSAGE = 1;
    public static final int TEKDAQC_DIGITAL_INPUT_MESSAGE = 2;
    public static final int TEKDAQC_DIGITAL_OUTPUT_MESSAGE = 3;
    public static final int TEKDAQC_STATUS_MESSAGE = 4;
    public static final int TEKDAQC_DEBUG_MESSAGE = 5;
    public static final int TEKDAQC_COMMAND_MESSAGE = 6;
    public static final int TEKDAQC_TASK_COMPLETE = 7;
    public static final int TEKDAQC_TASK_FAILURE = 8;

    // Locator Service constants
    public static final int LOCATOR_RESPONSE = 0;
    public static final int LOCATOR_FIRST = 1;
    public static final int LOCATOR_LOST = 2;
    public static final String BROADCAST_URI = "com.tenkiv.tekdaqc.LOCATOR";
    public static final String BROADCAST_TEKDAQC_RESPONSE = "LOCATED_TEKDAQC";
    public static final String BROADCAST_CALL_TYPE = "LOCATED_CALL_TYPE";
    public static final long DEFAULT_DELAY = 0;
    public static final long DEFAULT_PERIOD = 3000;


}
