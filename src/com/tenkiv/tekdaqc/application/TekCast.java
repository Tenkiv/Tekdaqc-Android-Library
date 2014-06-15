package com.tenkiv.tekdaqc.application;

import android.os.Bundle;
import android.content.Intent;
import com.tenkiv.tekdaqc.ATekDAQC;
import com.tenkiv.tekdaqc.locator.LocatorParams;
import com.tenkiv.tekdaqc.services.CommunicationService.ServiceAction;
import com.tenkiv.tekdaqc.services.DiscoveryService;

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
     * Broadcast action indicating that a {@link ATekDAQC} has connected.
     */
    public static final String ACTION_BOARD_CONNECTED = PACKAGE + "ACTION_BOARD_CONNECTED";

    /**
     * Broadcast action indicating that a {@link ATekDAQC} has disconnected.
     */
    public static final String ACTION_BOARD_DISCONNECTED = PACKAGE + "ACTION_BOARD_DISCONNECTED";

	/**
	 * The {@link ServiceAction} to be processed by a {@link DiscoveryService} instance.
	 */
	public static final String EXTRA_SERVICE_ACTION = PACKAGE + "EXTRA_SERVICE_ACTION";

    /**
     * The serial number of a {@link ATekDAQC}.
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
