package com.tenkiv.tekdaqc.android.application.client

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import com.tenkiv.tekdaqc.android.application.service.CommunicationService
import com.tenkiv.tekdaqc.android.application.util.IServiceListener
import com.tenkiv.tekdaqc.android.application.util.*
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback
import com.tenkiv.tekdaqc.communication.command.queue.Task
import com.tenkiv.tekdaqc.communication.command.queue.values.IQueueObject
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.ClientMessageHandler
import java.io.Serializable

/**
 * Class which manages the connection between commands and data sent through [Tekdaqc] objects and [CommunicationService].

 * @author Tenkiv (software@tenkiv.com)
 * *
 * @since v2.0.0.0
 */
class TekdaqcCommunicationManager
/**
 * Singleton constructor to ensure that there exists only one per process.

 * @param context The context.
 * *
 * @param listener The listener callback for completion.
 */
private constructor(
        /**
         * The Context.
         */
        private val mContext: Context,
        /**
         * Callback for notification that service has been connected to.
         */
        private val mServiceListener: IServiceListener) : ServiceConnection {

    private val mMessageHandler: ClientMessageHandler

    /**
     * This process's reference to the [CommunicationService]'s [Messenger].
     */
    private var mService: Messenger? = null

    /**
     * The [Messenger] for this client.
     */
    private val mMessenger: Messenger


    init {

        tekdaqcCommunicationsManager = this

        mMessageHandler = ClientMessageHandler(mContext, Tekdaqc.getMessageBroadcaster())

        mMessenger = Messenger(mMessageHandler)

        val comService = Intent(mContext, CommunicationService::class.java)
        mContext.bindService(comService, this, Context.BIND_AUTO_CREATE)

    }


    /**
     * Unbinds the manager from the service.
     */
    fun selfStopCommunicationManager() {
        mContext.unbindService(this)

    }


    /**
     * Client side call to attempt to execute a command on a [ATekdaqc] on the [CommunicationService].

     * @param serial The [String] of the serial number.
     * *
     * @param command The [IQueueObject].
     */
    fun executeCommand(serial: String, command: IQueueObject?) {

        if (command == null) {
            throw NullPointerException()
        }

        val dataBundle = Bundle()
        dataBundle.putString(SERVICE_SERIAL_KEY, serial)
        dataBundle.putSerializable(SERVICE_COMMAND_KEY, command)
        val msg = Message.obtain(null, SERVICE_MSG_COMMAND)
        msg.data = dataBundle
        msg.replyTo = mMessenger

        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }


    /**
     * Client side call to attempt to execute a [Task] on a [ATekdaqc] on the [CommunicationService].

     * @param serial The [String] of the serial number.
     * *
     * @param task The [Task].
     */
    fun executeTask(serial: String, task: Task?) {

        if (task == null) {
            throw NullPointerException()
        }

        val commandList = task.commandList

        for (command in commandList) {
            if (command is QueueCallback) {
                mMessageHandler.addTaskToMap(mUIDAssign, command)
                command.uid = mUIDAssign
                mUIDAssign++
            }
        }

        val dataBundle = Bundle()
        dataBundle.putString(SERVICE_SERIAL_KEY, serial)
        dataBundle.putSerializable(SERVICE_TASK_KEY, commandList as Serializable)
        val msg = Message.obtain(null, SERVICE_MSG_TASK)
        msg.data = dataBundle
        msg.replyTo = mMessenger

        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }


    /**
     * Call to have the [CommunicationService] attempt to connect to the specified [ATekdaqc].

     * @param tekdaqc The [ATekdaqc] to connect to.
     */
    fun connectToTekdaqc(tekdaqc: ATekdaqc?) {

        if (tekdaqc == null) {
            throw NullPointerException()
        }

        mMessageHandler.addTekdaqcToMap(tekdaqc)

        val msg = Message.obtain(null, SERVICE_MSG_CONNECT) with Pair(SERVICE_TEKDAQC_CONNECT, tekdaqc.locatorResponse)
        msg.replyTo = mMessenger

        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    /**
     * Method to attempt to safely disconnect from the selected [ATekdaqc].

     * @param tekdaqc The [ATekdaqc] to disconnect from.
     */
    fun disconnectFromTekdaqc(tekdaqc: ATekdaqc?) {

        if (tekdaqc == null) {
            throw NullPointerException()
        }

        mMessageHandler.removeTekdaqcFromMap(tekdaqc.serialNumber)

        val msg = Message.obtain(null, SERVICE_MSG_DISCONNECT) with Pair(SERVICE_TEKDAQC_DISCONNECT, tekdaqc)
        msg.replyTo = mMessenger

        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }


    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mService = Messenger(service)

        mServiceListener.onManagerServiceCreated(this)
    }


    override fun onServiceDisconnected(name: ComponentName) {}

    companion object {

        /**
         * Gets the [TekdaqcCommunicationManager].

         * @return The [TekdaqcCommunicationManager], returns null if not created yet.
         */
        var tekdaqcCommunicationsManager: TekdaqcCommunicationManager? = null
            private set

        /**
         * Variable used to assign UIDs.
         */
        @Volatile private var mUIDAssign = 0.0


        /**
         * Method used for creating this singleton class.

         * @param context The context.
         * *
         * @param listener The listener callback for completion.
         */
        fun startCommunicationService(context: Context, listener: IServiceListener) {
            if (tekdaqcCommunicationsManager == null) {
                tekdaqcCommunicationsManager = TekdaqcCommunicationManager(context, listener)

            } else {
                listener.onManagerServiceCreated(tekdaqcCommunicationsManager)
            }
        }

        fun stopCommunicationManager() {
            tekdaqcCommunicationsManager?.selfStopCommunicationManager()
        }


        /**
         * Static method to determine if the [CommunicationService] is running.

         * @param context The context.
         * *
         * @return The current status of the [CommunicationService].
         */
        fun isComServiceRunning(context: Context): Boolean {

            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (CommunicationService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }


}
