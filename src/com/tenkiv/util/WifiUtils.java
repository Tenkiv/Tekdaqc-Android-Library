package com.tenkiv.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class WifiUtils {

    private static final String TAG = "WifiUtils";

	public static String wifiIpAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

		// Convert little-endian to big-endianif needed
		if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
			ipAddress = Integer.reverseBytes(ipAddress);
		}

		byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

		String ipAddressString;
		try {
			ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
		} catch (UnknownHostException ex) {
			Log.e(TAG, "Unable to get host address.");
			ipAddressString = null;
		}

		return ipAddressString;
	}
}
