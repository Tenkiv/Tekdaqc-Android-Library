package com.tenkiv.tekdaqc.android.application.util

import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import java.io.Serializable

// Keys for Bundle Packets.
const val SERVICE_TEKDAQC_REGISTER = "SERVICE_TEKDAQC_REGISTER"
const val SERVICE_SERIAL_KEY = "SERVICE_SERIAL_KEY"
const val SERVICE_COMMAND_KEY = "SERVICE_COMMAND_KEY"
const val SERVICE_TASK_KEY = "SERVICE_TASK_KEY"
const val SERVICE_TEKDAQC_CONNECT = "SERVICE_TEKDAQC_CONNECT"
const val SERVICE_TEKDAQC_DISCONNECT = "SERVICE_TEKDAQC_DISCONNECT"
const val DATA_MESSSAGE_TEKDAQC = "DATA_MESSSAGE_TEKDAQC"
const val DATA_MESSSAGE = "DATA_MESSSAGE"
const val DATA_MESSSAGE_UID = "DATA_MESSSAGE_UID"

// Integers for service Message handling.
const val SERVICE_MSG_REGISTER = 0
const val SERVICE_MSG_UNREGISTER = 1
const val SERVICE_MSG_COMMAND = 2
const val SERVICE_MSG_TASK = 3
const val SERVICE_MSG_CONNECT = 4
const val SERVICE_MSG_DISCONNECT = 5

// Integers for application side Message handling.
const val TEKDAQC_ERROR_MESSAGE = 0
const val TEKDAQC_ANALOG_INPUT_MESSAGE = 1
const val TEKDAQC_DIGITAL_INPUT_MESSAGE = 2
const val TEKDAQC_DIGITAL_OUTPUT_MESSAGE = 3
const val TEKDAQC_STATUS_MESSAGE = 4
const val TEKDAQC_DEBUG_MESSAGE = 5
const val TEKDAQC_COMMAND_MESSAGE = 6
const val TEKDAQC_TASK_COMPLETE = 7
const val TEKDAQC_TASK_FAILURE = 8

// Locator Service constants
const val LOCATOR_RESPONSE = 0
const val LOCATOR_FIRST = 1
const val LOCATOR_LOST = 2
const val BROADCAST_URI = "com.tenkiv.tekdaqc.LOCATOR"
const val BROADCAST_TEKDAQC_RESPONSE = "LOCATED_TEKDAQC"
const val BROADCAST_CALL_TYPE = "LOCATED_CALL_TYPE"
const val DEFAULT_DELAY: Long = 0
const val DEFAULT_PERIOD: Long = 3000

infix fun <T>Message.with(msgData: Pair<String,T> ): Message{
    val bundle = Bundle()
    when (msgData.second) {
        is Int -> bundle.putInt(msgData.first, msgData.second as Int)
        is Float -> bundle.putFloat(msgData.first, msgData.second as Float)
        is String -> bundle.putString(msgData.first, msgData.second as String)
        is Double -> bundle.putDouble(msgData.first, msgData.second as Double)
        is Boolean -> bundle.putBoolean(msgData.first, msgData.second as Boolean)
        is Parcelable -> bundle.putParcelable(msgData.first, msgData.second as Parcelable)
        is BooleanArray -> bundle.putBooleanArray(msgData.first, msgData.second as BooleanArray)
        is Serializable -> bundle.putSerializable(msgData.first, msgData.second as Serializable)
    }
    this.data = bundle
    return this
}