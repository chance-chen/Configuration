/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sip;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.NumberFormatException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import com.example.android.sip.Constants;

/*
 * try to use property to communicate with Activity if needed
 **

/**
 * Bluetooth headset manager for Telecom. This class shares the call state with the bluetooth device
 * and accepts call-related commands to perform on behalf of the BT device.
 */
public final class BluetoothPhoneService extends Service {
    /**
     * Request object for performing synchronous requests to the main thread.
     */
    private static class MainThreadRequest {
        Object result;
        int param;

        MainThreadRequest(int param) {
            this.param = param;
        }

        void setResult(Object value) {
            result = value;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private static final String TAG = "BluetoothPhoneService";

    /*
    private static final int MSG_ANSWER_CALL = 1;
    private static final int MSG_HANGUP_CALL = 2;
    private static final int MSG_SEND_DTMF = 3;
    private static final int MSG_PROCESS_CHLD = 4;
    private static final int MSG_GET_NETWORK_OPERATOR = 5;
    private static final int MSG_LIST_CURRENT_CALLS = 6;
    private static final int MSG_QUERY_PHONE_STATE = 7;
    private static final int MSG_GET_SUBSCRIBER_NUMBER = 8;
    */

    // match up with bthf_call_state_t of bt_hf.h
    private static final int CALL_STATE_ACTIVE = 0;
    private static final int CALL_STATE_HELD = 1;
    private static final int CALL_STATE_DIALING = 2;
    private static final int CALL_STATE_ALERTING = 3;
    private static final int CALL_STATE_INCOMING = 4;
    private static final int CALL_STATE_WAITING = 5;
    private static final int CALL_STATE_IDLE = 6;

    // match up with bthf_call_state_t of bt_hf.h
    // Terminate all held or set UDUB("busy") to a waiting call
    private static final int CHLD_TYPE_RELEASEHELD = 0;
    // Terminate all active calls and accepts a waiting/held call
    private static final int CHLD_TYPE_RELEASEACTIVE_ACCEPTHELD = 1;
    // Hold all active calls and accepts a waiting/held call
    private static final int CHLD_TYPE_HOLDACTIVE_ACCEPTHELD = 2;
    // Add all held calls to a conference
    private static final int CHLD_TYPE_ADDHELDTOCONF = 3;

    private int mNumActiveCalls = 0;
    private int mNumHeldCalls = 0;
    private int mBluetoothCallState = CALL_STATE_IDLE;
    private String mRingingAddress = null;
    private int mRingingAddressType = 0;
    public static boolean sAnswerFromBluetooth = false;

    /**
     * Binder implementation of IBluetoothHeadsetPhone. Implements the command interface that the
     * bluetooth headset code uses to control call.
     */
    private final IBluetoothHeadsetPhone.Stub mBinder = new IBluetoothHeadsetPhone.Stub() {
        @Override
        public boolean answerCall() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: answerCall()");
            enforceModifyPermission();
            Log.d(TAG, "IBluetoothHeadsetPhone: answerCall()");
            return sendSynchronousRequest(Constants.MSG_ANSWER_CALL);
        }

        @Override
        public boolean hangupCall() throws RemoteException {
            enforceModifyPermission();
            Log.d(TAG, "IBluetoothHeadsetPhone: hangupCall()");
            return sendSynchronousRequest(Constants.MSG_HANGUP_CALL);
        }

        @Override
        public boolean sendDtmf(int dtmf) throws RemoteException {
            enforceModifyPermission();
            Log.d(TAG, "IBluetoothHeadsetPhone: sendDtmf(): dtmf " + dtmf);
            return sendSynchronousRequest(Constants.MSG_SEND_DTMF, dtmf);
        }

        @Override
        public String getNetworkOperator() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: getNetworkOperator()");
            enforceModifyPermission();
            return sendSynchronousRequest(Constants.MSG_GET_NETWORK_OPERATOR);
        }

        @Override
        public String getSubscriberNumber() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: getSubscriberNumber()");
            enforceModifyPermission();
            return sendSynchronousRequest(Constants.MSG_GET_SUBSCRIBER_NUMBER);
        }

        @Override
        public boolean listCurrentCalls() throws RemoteException {
            // only log if it is after we recently updated the headset state or else it can clog
            // the android log since this can be queried every second.
            boolean logQuery = mHeadsetUpdatedRecently;
            mHeadsetUpdatedRecently = false;
            Log.d(TAG, "IBluetoothHeadsetPhone: listCurrentCalls(): logQuery " + logQuery);
            if (logQuery) {
                Log.d(TAG, "listCurrentCalls()");
            }
            enforceModifyPermission();
            return sendSynchronousRequest(Constants.MSG_LIST_CURRENT_CALLS, logQuery ? 1 : 0);
        }

        @Override
        public boolean queryPhoneState() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: queryPhoneState()");
            enforceModifyPermission();
            return sendSynchronousRequest(Constants.MSG_QUERY_PHONE_STATE);
        }

        @Override
        public boolean processChld(int chld) throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: processChld():  chld " + chld);
            enforceModifyPermission();
            return sendSynchronousRequest(Constants.MSG_PROCESS_CHLD, chld);
        }

        @Override
        public void updateBtHandsfreeAfterRadioTechnologyChange() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: updateBtHandsfreeAfterRadioTechnologyChange(): RAT change");
            // deprecated
        }

        @Override
        public void cdmaSetSecondCallState(boolean state) throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: cdmaSetSecondCallState(): cdma 1, state " + state);
            // deprecated
        }

        @Override
        public void cdmaSwapSecondCallState() throws RemoteException {
            Log.d(TAG, "IBluetoothHeadsetPhone: cdmaSwapSecondCallState(): cdma 2");
            // deprecated
        }
    };

    /**
     * Main-thread handler for BT commands.  Since telecom logic runs on a single thread, commands
     * that are sent to it from the headset need to be moved over to the main thread before
     * executing. This handler exists for that reason.
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request = msg.obj instanceof MainThreadRequest ?
                    (MainThreadRequest) msg.obj : null;
            if (request == null)
            {
                Log.i(TAG, "handleMessage: request is null");
                return;
            }

            switch (msg.what) {
                case Constants.MSG_ANSWER_CALL:
                    Log.d(TAG, "MSG_ANSWER_CALL");
		    try {
		        Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_ANSWER_CALL);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;

                case Constants.MSG_HANGUP_CALL:
                    Log.d(TAG, "MSG_HANGUP_CALL");
		    try {
		        Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_HANGUP_CALL);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;

                case Constants.MSG_SEND_DTMF:
                    Log.d(TAG, "MSG_SEND_DTMF");
		    try {
		        Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_SEND_DTMF);
			intent.putExtra("dtmf", request.param);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;

                case Constants.MSG_PROCESS_CHLD:
                    Log.d(TAG, "MSG_PROCESS_CHLD");
		    try {
			Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_PROCESS_CHLD);
			intent.putExtra("chld", request.param);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;

                case Constants.MSG_GET_SUBSCRIBER_NUMBER:
                    Log.d(TAG, "MSG_GET_SUBSCRIBER_NUMBER");
		    request.setResult(Constants.SUBSCRIBER_NUMBER);
                    break;

                case Constants.MSG_GET_NETWORK_OPERATOR:
                    Log.d(TAG, "MSG_GET_NETWORK_OPERATOR");
		    request.setResult(Constants.NETWORK_OPERATOR);
                    break;

                case Constants.MSG_LIST_CURRENT_CALLS:
                    Log.d(TAG, "MSG_LIST_CURRENT_CALLS");
		    try {
			Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_LIST_CURRENT_CALLS);
			intent.putExtra("shouldLog", request.param == 1);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;

                case Constants.MSG_QUERY_PHONE_STATE:
                    Log.d(TAG, "MSG_QUERY_PHONE_STATE");
		    try {
			Intent intent = new Intent(getApplicationContext(), IncomingCallReceiver.class);
			intent.setAction(Constants.ACTION_QUERY_PHONE_STATE);
			sendBroadcast(intent);
                    } finally {
		        request.setResult(true);
                    }
                    break;
            }
        }
    };

    /**
     * Listens to connections and disconnections of bluetooth headsets.  We need to save the current
     * bluetooth headset so that we know where to send call updates.
     */
    private BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothHeadset = (BluetoothHeadset) proxy;
            Log.d(TAG, "mProfileListener: onServiceConnected() mBluetoothHeadset " + mBluetoothHeadset);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "mProfileListener: onServiceDisconnected() set mBluetoothHeadset null");
            mBluetoothHeadset = null;
        }
    };

    /**
     * Receives events for global state changes of the bluetooth adapter.
     */
    private final BroadcastReceiver mBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
	    /*
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON) {
                mHandler.sendEmptyMessage(MSG_QUERY_PHONE_STATE);
            }
	    */
        }
    };

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;


    private boolean mHeadsetUpdatedRecently = false;

    public BluetoothPhoneService() {
        Log.d(TAG, "BluetoothPhoneService(): Constructor");
    }

    public static final void start(Context context) {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            Log.d(TAG, "start(): start service");
            context.startService(new Intent(context, BluetoothPhoneService.class));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind(): Binding service");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "onCreate(): BluetoothPhoneService shutting down, no BT Adapter found.");
            return;
        }
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothAdapterReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void enforceModifyPermission() {
        enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
    }

    private <T> T sendSynchronousRequest(int message) {
        return sendSynchronousRequest(message, 0);
    }

    private <T> T sendSynchronousRequest(int message, int param) {

        MainThreadRequest request = new MainThreadRequest(param);
        mHandler.obtainMessage(message, request).sendToTarget();

        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete.
                }
            }
        }
	
        if (request.result != null) {
            @SuppressWarnings("unchecked")
            T retval = (T) request.result;
            return retval;
        }
        return null;
    }
}
