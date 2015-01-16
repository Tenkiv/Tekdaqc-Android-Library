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

import android.content.Intent;
import android.os.Bundle;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.services.CommunicationService.ServiceAction;
import com.tenkiv.tekdaqc.communication.tasks.ITask;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;
import com.tenkiv.tekdaqc.locator.LocatorParams;

/**
 * Container of {@link String}s for {@link Intent} and {@link Bundle} actions and extras.
 *
 * @author Ian Thomas (toxicbakery@gmail.com)
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public interface TekCast {

    /**
     * The package name for use as a prefix to uniquely identify these commands.
     */
	static final String PACKAGE = TekCast.class.getPackage().getName() + ".";

	/**
	 * Broadcast action indicating a board has been found.
	 */
	public static final String ACTION_FOUND_BOARD = PACKAGE + "ACTION_FOUND_BOARD";

    /**
     * Broadcast action indicating that a session has been updated.
     */
    public static final String ACTION_SESSION_UPDATED = PACKAGE + "ACTION_SESSION_UPDATED";

    /**
     * Broadcast action indicating that a {@link ATekdaqc} has connected.
     */
    public static final String ACTION_BOARD_CONNECTED = PACKAGE + "ACTION_BOARD_CONNECTED";

    /**
     * Broadcast action indicating that a {@link ATekdaqc} has disconnected.
     */
    public static final String ACTION_BOARD_DISCONNECTED = PACKAGE + "ACTION_BOARD_DISCONNECTED";
    
    /**
     * Broadcast action indicating that a connection has timed out.
     */
    public static final String ACTION_CONNECTION_TIMEOUT = PACKAGE + "ACTION_CONNECTION_TIMEOUT";

	/**
	 * The {@link ServiceAction} to be processed by a service.
	 */
	public static final String EXTRA_SERVICE_ACTION = PACKAGE + "EXTRA_SERVICE_ACTION";

    /**
     * A {@link ITask} instance
     */
    public static final String EXTRA_TASK = PACKAGE + "EXTRA_TASK";

    /**
     * A {@link ITaskComplete} instance
     */
    public static final String EXTRA_TASK_COMPLETE_CALLBACK = PACKAGE + "EXTRA_TASK_COMPLETE_CALLBACK";

    /**
     * Extra used for passing boolean success/error states
     */
    public static final String EXTRA_SUCCESS_ERROR_FLAG = PACKAGE + "EXTRA_SUCCESS_ERROR_FLAG";

    /**
     * The serial number of a {@link ATekdaqc}.
     */
    public static final String EXTRA_BOARD_SERIAL = PACKAGE + "EXTRA_BOARD_SERIAL";

    /**
     * A board command.
     */
    public static final String EXTRA_BOARD_COMMAND = PACKAGE + "EXTRA_BOARD_COMMAND";

	/**
	 * An instance of {@link LocatorParams}.
	 */
	public static final String EXTRA_LOCATOR_PARAMS = PACKAGE + "EXTRA_LOCATOR_PARAMS";

}