package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import com.tenkiv.tekdaqc.communication.ascii.executors.ASCIIParsingExecutor;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.AASCIIMessage;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIMessageUtils;

import java.util.concurrent.Callable;

/**
 * A parser used to create wrapped ASCII messages which can be more easily passed between processes in Android.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class AndroidRemoteParser extends ASCIIParsingExecutor {

    /**
     * Constructor.
     *
     * @param numThreads int The number of threads to use in the parsing pool.
     */
    public AndroidRemoteParser(int numThreads) {
        super(numThreads);
    }

    /**
     * Submit a message for parsing. When parsing is complete, the specified callback will be called with the result.
     *
     * @param messageData {@link String} The raw message data to parse.
     * @param callback    The callback to be called when parsing is complete.
     */
    public void parseMessage(final String messageData, final IParsingListener callback) {
        mExecutor.submit(new ParsingTask(messageData, callback));
    }

    @Override
    public void parseMessage(byte[] messageData, IParsingListener callback) {
        parseMessage(new String(messageData), callback);
    }

    /**
     * Parsing task to be submitted to the executor for ASCII coded messages. For each message which is received,
     * a new {@link ParsingTask} is generated and submitted for execution.
     *
     * @author Tenkiv (software@tenkiv.com)
     * @since v1.0.0.0
     */
    private static final class ParsingTask extends AParsingTask implements Callable<Void> {

        /**
         * The raw message data
         */
        private final String mMessageData;

        /**
         * Constructor.
         *
         * @param messageData {@link String} The raw message data.
         * @param callback    The callback for parsed messages.
         */
        public ParsingTask(final String messageData, final IParsingListener callback) {
            super(callback);
            mMessageData = messageData;
        }

        @Override
        public Void call() throws Exception {
            try {
                final AASCIIMessage message = sortMessage(mMessageData);
                if (message != null) mCallback.onParsingComplete(message);
            } catch (final Exception e) {
                System.out.println("ASCIIParsingExecutor absorbing Exception: ");
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Factory method to produce the appropriate messages from the provided raw
         * message data.
         *
         * @param messageData {@link String} The raw message data.
         * @return {@link AASCIIMessage} The constructed message.
         */
        public static AASCIIMessage sortMessage(final String messageData) {
            final AASCIIMessage message;
            try {
                if (messageData == null)
                    return null;
            /*
			 * The order here is important because debug/status/error messages
			 * may contain tags which could register as other message types.
			 */
                if (messageData.contains(ASCIIMessageUtils.DEBUG_MESSAGE_HEADER)) {
                    // This is an ASCII Debug message
                    message = new ParcelableDebugMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.STATUS_MESSAGE_HEADER)) {
                    // This is an ASCII Status message
                    message = new ParcelableStatusMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.ERROR_MESSAGE_HEADER)) {
                    // This is an ASCII Error message
                    message = new ParcelableErrorMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.COMMAND_MESSAGE_HEADER)) {
                    message = new ParcelableCommandMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.V1_ANALOG_INPUT_HEADER)
                        || messageData.contains(ASCIIMessageUtils.V2_ANALOG_INPUT_HEADER)) {
                    // This is an ASCII Analog Input Data message
                    message = new ParcelableAnalogInputMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.V1_DIGITAL_INPUT_HEADER)
                        || messageData.contains(ASCIIMessageUtils.V2_DIGITAL_INPUT_HEADER)) {
                    // This is an ASCII Digital Input Data message
                    message = new ParcelableDigitalInputMessage(messageData);
                } else if (messageData.contains(ASCIIMessageUtils.V1_DIGITAL_OUTPUT_HEADER)) {
                    // This is an ASCII Digital Output Data message
                    message = new ParcelableDigitalOutputMessage(messageData);
                } else {
                    // This is an unrecognized message format
                    message = null;
                }
            } catch (final Exception e) {
			/*System.err.println("Detected exception parsing message ("
					+ e.getClass().getSimpleName() + "). Message Data:");
			System.err.println(messageData);
			e.printStackTrace();*///TODO REMOVE THIS ONCE FIRMWARE BECOMES UN-FUCKED.
                return null;
            }
            return message;
        }
    }
}
