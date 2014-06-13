package com.tenkiv.tekdaqc.application;

import com.tenkiv.tekdaqc.services.CommunicationService.ServiceAction;

public interface TekCast {

	static final String PACKAGE = TekCast.class.getPackage().getName() + ".";

	/**
	 * Broadcast action indicating a board has been found
	 */
	public static final String ACTION_FOUND_BOARD = PACKAGE + "ACTION_FOUND_BOARD";

    public static final String ACTION_SESSION_UPDATED = PACKAGE + "ACTION_SESSION_UPDATED";

    public static final String ACTION_SERVICE_COMMAND = PACKAGE + "ACTION_SERVICE_COMMAND";

    public static final String ACTION_BOARD_CONNECTED = PACKAGE + "ACTION_BOARD_CONNECTED";

    public static final String ACTION_BOARD_DISCONNECTED = PACKAGE + "ACTION_BOARD_DISCONNECTED";

	/**
	 * The {@link ServiceAction} to be processed by a {@link com.tenkiv.tekdaqc.services.DiscoveryService} instance.
	 */
	public static final String EXTRA_SERVICE_ACTION = PACKAGE + "EXTRA_SERVICE_ACTION";

    public static final String EXTRA_SESSION_UUID = PACKAGE + "EXTRA_SESSION_UUID";

    public static final String EXTRA_BOARD_SERIAL = PACKAGE + "EXTRA_BOARD_SERIAL";

    public static final String EXTRA_BOARD_COMMAND = PACKAGE + "EXTRA_BOARD_COMMAND";

    public static final String EXTRA_COMMAND_PARAMS = PACKAGE + "EXTRA_COMMAND_PARAMS";

    public static final String EXTRA_ANALOG_INPUT = PACKAGE + "EXTRA_ANALOG_INPUT";

    public static final String EXTRA_DIGITAL_INPUT = PACKAGE + "EXTRA_DIGITAL_INPUT";

    public static final String EXTRA_DIGITAL_OUTPUT = PACKAGE + "EXTRA_DIGITAL_OUTPUT";

    public static final String EXTRA_NUM_SAMPLES = PACKAGE + "EXTRA_NUM_SAMPLES";

	/**
	 * An instance of {@link com.tenkiv.tekdaqc.LocatorParams}
	 */
	public static final String EXTRA_LOCATOR_PARAMS = PACKAGE + "EXTRA_LOCATOR_PARAMS";

}
