package com.tenkiv.tekdaqc;

import com.tenkiv.tekdaqc.CommunicationService.ServiceAction;
import com.tenkiv.tekdaqc.command.Command;
import com.tenkiv.tekdaqc.command.Parameter;

import java.util.ArrayList;

public interface TekCast {

	static final String PACKAGE = TekCast.class.getPackage().getName() + ".";

	/**
	 * Broadcast action indicating a board has been found {@link #EXTRA_TEK_BOARD}
	 */
	public static final String ACTION_FOUND_BOARD = PACKAGE + "ACTION_FOUND_BOARD";

    public static final String ACTION_SESSION_UPDATED = PACKAGE + "ACTION_SESSION_UPDATED";

    public static final String ACTION_SERVICE_COMMAND = PACKAGE + "ACTION_SERVICE_COMMAND";

    public static final String ACTION_BOARD_CONNECTED = PACKAGE + "ACTION_BOARD_CONNECTED";

    public static final String ACTION_BOARD_DISCONNECTED = PACKAGE + "ACTION_BOARD_DISCONNECTED";

	/**
	 * The {@link ServiceAction} to be processed by a {@link DiscoveryService} instance.
	 */
	public static final String EXTRA_SERVICE_ACTION = PACKAGE + "EXTRA_SERVICE_ACTION";

	/**
	 * The {@link Command} to processed when requesting {@link ServiceAction#COMMAND} action.
	 */
	public static final String EXTRA_SERVICE_COMMAND = PACKAGE + "EXTRA_SERVICE_COMMAND";

	/**
	 * An {@link ArrayList} of {@link Parameter}s.
	 */
	public static final String EXTRA_SERVICE_PARAMS = PACKAGE + "EXTRA_SERVICE_PARAMS";

    public static final String EXTRA_SESSION_UUID = PACKAGE + "EXTRA_SESSION_UUID";

    public static final String EXTRA_BOARD_SERIAL = PACKAGE + "EXTRA_BOARD_SERIAL";

	/**
	 * An instance of {@link ATekDAQC}
	 */
	public static final String EXTRA_TEK_BOARD = PACKAGE + "EXTRA_TEK_BOARD_MAP";

	/**
	 * An instance of {@link LocatorParams}
	 */
	public static final String EXTRA_LOCATOR_PARAMS = PACKAGE + "EXTRA_LOCATOR_PARAMS";

}
