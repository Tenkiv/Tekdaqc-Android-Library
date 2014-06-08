package com.tenkiv.tekdaqc;

import com.tenkiv.tekdaqc.DiscoveryService.ServiceAction;
import com.tenkiv.tekdaqc.command.Command;
import com.tenkiv.tekdaqc.command.Parameter;

import java.util.ArrayList;

public interface TekCast {

	static final String PACKAGE = TekCast.class.getPackage().getName() + ".";

	/**
	 * Broadcast action indicating a board has been found {@link #EXTRA_TEK_BOARD}
	 */
	public static final String ACTION_FOUND_BOARD = PACKAGE + "ACTION_FOUND_BOARD";

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

	/**
	 * An instance of {@link ATekDAQC}
	 */
	public static final String EXTRA_TEK_BOARD = PACKAGE + "EXTRA_TEK_BOARD_MAP";

	/**
	 * An instance of {@link LocatorParams}
	 */
	public static final String EXTRA_LOCATOR_PARAMS = PACKAGE + "EXTRA_LOCATOR_PARAMS";

}
