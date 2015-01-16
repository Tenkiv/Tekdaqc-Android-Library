package com.tenkiv.tekdaqc.android.content;

import android.content.Context;

/**
 * Default implementation of the {@link ATekdaqcDatabaseHelper}. This is simply a thin
 * wrapper around the abstract class, allowing the use of the default parameters. The database
 * will be created at the location specified by {@link ATekdaqcDatabaseHelper#DEFAULT_FILE_DIR}.
 * 
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class DefaultTekdaqcDatabaseHelper extends ATekdaqcDatabaseHelper {

	public DefaultTekdaqcDatabaseHelper(Context context) {
		super(context);
	}
}
