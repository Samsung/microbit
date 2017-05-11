package com.samsung.microbit.utils;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.CharacteristicUUIDs;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.GattFormats;
import com.samsung.microbit.data.constants.GattServiceUUIDs;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.ServiceIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.service.IPCService;

/**
 * Provides additional functionality to work with services,
 * such as create messages, create connection, disconnect from services.
 */
public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();

    private ServiceUtils() {
    }

    /**
     * Send some reply message to the ipc.
     *
     * @param mbsService MbsService of reply.
     * @param cmd        Command should be sent, as reply.
     */
    public static void sendReplyCommand(int mbsService, CmdArg cmd) {
        MBApp application = MBApp.getApp();

        Intent intent = new Intent(application, IPCService.class);
        intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.CATEGORY_REPLY);
        intent.putExtra(IPCConstants.INTENT_REPLY_TO, ServiceIds.SERVICE_PLUGIN);
        intent.putExtra(IPCConstants.INTENT_MBS_SERVICE, mbsService);
        intent.putExtra(IPCConstants.INTENT_CMD_ARG, cmd);
        application.startService(intent);
    }

    /**
     * Compose message for IPC communication
     *
     * @param messageType   Android or microbit message. One of the {@link com.samsung.microbit.data.constants.IPCConstants#MESSAGE_ANDROID},
     *                      {@link com.samsung.microbit.data.constants.IPCConstants#MESSAGE_MICROBIT}
     * @param eventCategory Event category listed in {@link EventCategories}
     * @param serviceId     Identifier of service. Detect where message should be delivered to. Can be one of possible
     *                      values - {@link ServiceIds#SERVICE_NONE}, {@link ServiceIds#SERVICE_BLE}, and
     *                      {@link ServiceIds#SERVICE_PLUGIN}
     * @param cmd           Command argument.
     * @param args          Array of data.
     */
    public static Message composeMessage(int messageType, int eventCategory, @ServiceIds int serviceId, CmdArg cmd,
                                         NameValuePair[] args) {
        if(messageType != IPCConstants.MESSAGE_ANDROID && messageType != IPCConstants.MESSAGE_MICROBIT) {
            return null;
        }
        Message msg = Message.obtain(null, messageType);

        msg.arg1 = eventCategory;
        msg.arg2 = serviceId;

        Bundle bundle = new Bundle();
        if(cmd != null) {
            bundle.putInt(IPCConstants.BUNDLE_DATA, cmd.getCMD());
            bundle.putString(IPCConstants.BUNDLE_VALUE, cmd.getValue());
        }

        if(args != null) {
            for(NameValuePair arg : args) {
                bundle.putSerializable(arg.getName(), arg.getValue());
            }
        }

        msg.setData(bundle);

        return msg;
    }

    /**
     * Compose message for BLE sensor notifications.
     *
     * @param value characteristic value for creating final message
     */
    public static Message composeBLECharacteristicMessage(int value) {
        NameValuePair[] args = new NameValuePair[4];
        args[0] = new NameValuePair(IPCConstants.BUNDLE_SERVICE_GUID, GattServiceUUIDs.EVENT_SERVICE.toString());
        args[1] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_GUID, CharacteristicUUIDs.ES_CLIENT_EVENT.toString());
        args[2] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_VALUE, value);
        args[3] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_TYPE, GattFormats.FORMAT_UINT32);

        return composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories.IPC_WRITE_CHARACTERISTIC,
                ServiceIds.SERVICE_BLE, null, args);
    }

    /**
     * Copy values from old message to new one.
     *
     * @param oldMessage Old messages, values should be copied from.
     * @param serviceId  Identifier of service. Detect where message should be delivered to. Can be one of possible
     *                   values - {@link ServiceIds#SERVICE_NONE}, {@link ServiceIds#SERVICE_BLE}, and
     *                   {@link ServiceIds#SERVICE_PLUGIN}
     */
    public static Message copyMessageFromOld(Message oldMessage, @ServiceIds int serviceId) {
        Message newMessage = Message.obtain(null, oldMessage.what);
        newMessage.arg1 = oldMessage.arg1;
        newMessage.arg2 = serviceId;
        newMessage.setData(new Bundle(oldMessage.getData()));
        return newMessage;
    }

    /**
     * Sent connect /or disconnect/ messages to the corresponding service.
     *
     * @param connect true, if message about connection establishing should be sent. False, if message of aborting
     *                connection should be sent.
     */
    public static void sendConnectDisconnectMessage(boolean connect) {
        MBApp application = MBApp.getApp();

        if(connect) {
            Intent intent = new Intent(application, IPCService.class);
            intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_CONNECT);
            int connectionType = MBApp.getApp().isJustPaired() ? IPCConstants.JUST_PAIRED : IPCConstants
                    .PAIRED_EARLIER;
            intent.putExtra(IPCConstants.INTENT_CONNECTION_TYPE, connectionType);
            application.startService(intent);
        } else {
            Intent intent = new Intent(application, IPCService.class);
            intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_DISCONNECT);
            application.startService(intent);
        }
    }

    /**
     * Used for make ipc interaction.
     */
    public interface IMessengerFinder extends ServiceConnection {
        Messenger getMessengerForService(String serviceName);
    }
}
