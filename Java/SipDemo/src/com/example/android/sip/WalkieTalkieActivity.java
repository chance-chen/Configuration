/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
//import android.net.sip.SipAudioCall;
import android.net.sip.*;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import android.media.AudioManager;
import android.content.Context;
import java.text.ParseException;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import com.example.android.sip.Constants;

/**
 * Handles all calling, receiving calls, and UI interaction in the WalkieTalkie app.
 */
public class WalkieTalkieActivity extends Activity implements View.OnTouchListener {

    public String sipAddress = null;

    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;
    //when there is an incoming call,incomingCall will be set and incomingCall() method will be invoked
    public SipAudioCall incomingCall = null;
    //public boolean isincomingcall = false;

    //number of active calls
    private int mNumActive = 0;
    //number of held calls
    private int mNumHeld = 0;
    //call state: CALL_STATE_ACTIVE CALL_STATE_HELD CALL_STATE_DIALING 
    //CALL_STATE_ALERTING CALL_STATE_INCOMING CALL_STATE_WAITING CALL_STATE_IDLE
    private int mCallState = Constants.CALL_STATE_IDLE;
    private String mNumber = Constants.SUBSCRIBER_NUMBER;
    private int mType = 129;


    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;
    private static final int START_BT_SCO = 5;
    private static final int STOP_BT_SCO = 6;
    private AudioManager mAudioManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    
    private static final String TAG = "WalkieTalkieActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkietalkie);

        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
        pushToTalkButton.setOnTouchListener(this);

        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_INCOMING_CALL);
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);
	callReceiver.setContext(this);

        // "Push to talk" can be a serious pain when the screen keeps turning off.
        // Let's prevent that.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeManager();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
		Log.d(TAG, "BluetoothPhoneService shutting down, no BT Adapter found.");
		return;
        }
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
    }

    @Override
    public void onStart() {
        super.onStart();
        // When we get back from the preference setting Activity, assume
        // settings have changed, and re-login with new auth info.
        initializeManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (call != null) {
            call.close();
	    call = null;
        }

        closeLocalProfile();

        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
    }

    public void initializeManager() {
        if(manager == null) {
          manager = SipManager.newInstance(this);
        }

	mAudioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

        initializeLocalProfile();
    }

    /**
     * Logs you into your SIP provider, registering this device as the location to
     * send SIP calls to for your SIP address.
     */
    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }

        if (me != null) {
            closeLocalProfile();
        }

	/*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = prefs.getString("namePref", "");
        String domain = prefs.getString("domainPref", "");
        String password = prefs.getString("passPref", "");
	*/

        String username = Constants.USERNAME;
        String domain = Constants.DOMAIN;
        String password = Constants.PASSWORD;

        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            me = builder.build();

            Intent i = new Intent();
            i.setAction(Constants.ACTION_INCOMING_CALL);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);

            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.

            manager.setRegistrationListener(me.getUriString(), new SipRegistrationListener() {
                    public void onRegistering(String localProfileUri) {
                        updateStatus("Registering with SIP Server...");
                    }

                    public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        updateStatus("Ready");
                    }

                    public void onRegistrationFailed(String localProfileUri, int errorCode,
                            String errorMessage) {
                        updateStatus("Registration failed.  Please check settings.");
			Log.d(TAG, "Registration faile " + errorMessage);
                    }
                });
        } catch (ParseException pe) {
            updateStatus("Connection Error.");
        } catch (SipException se) {
            updateStatus("Connection error.");
        }
    }

    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */
    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }
        try {
            if (me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d("WalkieTalkieActivity/onDestroy", "Failed to close local profile.", ee);
        }
    }

    /**
     * Make an outgoing call.
     */
    public void initiateCall() {

	if(sipAddress == null) {
	    sipAddress = Constants.REMOTE_SIP_ADDRESS;
	}
        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                @Override
                public void onCallEstablished(SipAudioCall c) {
		    Log.d(TAG, "onCallEstablished");
                    call.startAudio();
                    //call.setSpeakerMode(true);
                    //call.toggleMute();
                    updateStatus(call);
		    mNumActive++;
		    mCallState = Constants.CALL_STATE_ACTIVE;
		    updateCallState();
                }

		//onCallEnded not received if hangup by this side
                @Override
                public void onCallEnded(SipAudioCall c) {
		    Log.d(TAG, "onCallEnded");
                    updateStatus("Ready.");
		    mNumActive--;
		    mCallState = Constants.CALL_STATE_IDLE;
		    updateCallState();
		    call.close();
		    call = null;
                }

                @Override
                public void onChanged(SipAudioCall c) {
		    Log.d(TAG, "onChanged");
                }
            };

            call = manager.makeAudioCall(me.getUriString(), sipAddress, listener, 30);

        }
        catch (Exception e) {
            Log.i("WalkieTalkieActivity/InitiateCall", "Error when trying to close manager.", e);
            if (me != null) {
                try {
                    manager.close(me.getUriString());
                } catch (Exception ee) {
                    Log.i("WalkieTalkieActivity/InitiateCall",
                            "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
		call = null;
            }
        }
    }

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.sipLabel);
                labelView.setText(status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current call.
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
          useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates whether or not the user's voice is muted, depending on whether the button is pressed.
     * @param v The View where the touch event is being fired.
     * @param event The motion to act on.
     * @return boolean Returns false to indicate that the parent view should handle the touch event
     * as it normally would.
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call != null && call.isMuted()) {
		//call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
		//call.toggleMute();
        }

        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP Info.");
        menu.add(0, HANG_UP, 0, "End Current Call.");
        menu.add(0, START_BT_SCO, 0, "Start Bluetooth SCO");
        menu.add(0, STOP_BT_SCO, 0, "Stop Bluetooth SCO");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                if(call != null) {
                    try {
                      call.endCall();
                    } catch (SipException se) {
                        Log.d("WalkieTalkieActivity/onOptionsItemSelected",
                                "Error ending call.", se);
                    }
                    call.close();
		    call = null;
                }
                break;
	case START_BT_SCO:
		Log.d(TAG, "mAudioManager.startBluetoothSco");
		mAudioManager.startBluetoothSco();
		break;
	case STOP_BT_SCO:
		Log.d(TAG, "mAudioManager.stopBluetoothSco");
		mAudioManager.stopBluetoothSco();
		break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:

                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone.")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();

                                    }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
        }
        return null;
    }

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
    }

    private BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
		mBluetoothHeadset = (BluetoothHeadset) proxy;
	        Log.d(TAG, "mBluetoothHeadset connected");
        }

        @Override
        public void onServiceDisconnected(int profile) {
	        Log.d(TAG, "mBluetoothHeadset disconnected");
		mBluetoothHeadset = null;
        }
    };

    /* Start Implementation of IBluetoothHeadsetPhone methods */
    public void answerCall() {
        Log.d(TAG, "answerCall");
	try {
	    if(incomingCall != null) {
		Log.d(TAG, "answerCall incomingCall != null");
	        incomingCall.answerCall(30);
	        incomingCall.startAudio();
	        incomingCall.setSpeakerMode(true);
	        if(incomingCall.isMuted()) {
	            incomingCall.toggleMute();
		    Log.d(TAG, "incomingCall.isMuted toggleMute");
	        }
                updateStatus(incomingCall);
	    }
	} catch (Exception e) {
	    Log.d(TAG, "exception:" + e);
	    if (incomingCall != null) {
	        incomingCall.close();
		incomingCall = null;
	    }
        }
    }

    public void hangupCall() {
        Log.d(TAG, "hangupCall");
        if(call != null) {
            try {
                call.endCall();
            } catch (SipException se) {
                Log.d("hangupCall",
                                "Error ending call.", se);
            }
            //onCallEnded will not be called if close the call here
	    /*
            call.close();
	    call = null;
	    */
        }
        if(incomingCall != null) {
            try {
                incomingCall.endCall();
            } catch (SipException se) {
                Log.d("hangupCall",
                                "Error ending call.", se);
            }
	    /*
            incomingCall.close();
	    incomingCall = null;
	    */
        }
    }

    public void sendDtmf(int dtmf) {
        Log.d(TAG, "sendDtmf:" + dtmf);
    }

    //should never be called
    public void getNetworkOperator() {
        Log.d(TAG, "getNetworkOperator");
    }

    //should never be called
    public void getSubscriberNumber() {
        Log.d(TAG, "getSubscriberNumber");
    }

    public void listCurrentCalls(boolean shouldLog) {
        Log.d(TAG, "listCurrentCalls:" + shouldLog);
    }

    public void queryPhoneState() {
        Log.d(TAG, "queryPhoneState");
    }

    public void processChld(int chld) {
        Log.d(TAG, "processChld:" + chld);
    }

    public void makeAudioCall() {
        Log.d(TAG, "makeAudioCall");
	initiateCall();
    }
    /* End Implementation of IBluetoothHeadsetPhone methods */

    //there is an incoming call
    public void incomingCall(SipAudioCall incoming) {
        Log.d(TAG, "incomingCall");
	if(incoming != null) {
	    incomingCall = incoming;
            updateStatus("incomingCall....");
            mCallState = Constants.CALL_STATE_INCOMING;
            updateCallState();
	}
    }

    public void incomingCallEstablished() {
        Log.d(TAG, "incomingCallEstablished");
	mNumActive++;
	mCallState = Constants.CALL_STATE_ACTIVE;
	updateCallState();
    }

    public void incomingCallEnded() {
        Log.d(TAG, "incomingCallEnded");
        if (incomingCall != null) {
            incomingCall.close();
	    incomingCall = null;
	    mNumActive--;
	    mCallState = Constants.CALL_STATE_IDLE;
	    updateCallState();
            updateStatus("Ready.");
        }
    }

    private void updateCallState() {
        Log.d(TAG, "updateCallState");
	if(mBluetoothHeadset != null) {
	    Log.d(TAG, "updateCallState...");
	    mBluetoothHeadset.phoneStateChanged(mNumActive, mNumHeld, mCallState, mNumber, mType);
	}
    }
}
