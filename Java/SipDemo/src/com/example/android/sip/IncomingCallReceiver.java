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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.*;
import android.util.Log;
import com.example.android.sip.Constants;

/**
 * Listens for incoming SIP calls, intercepts and hands them off to WalkieTalkieActivity.
 */
public class IncomingCallReceiver extends BroadcastReceiver {

    private static final String TAG = "IncomingCallReceiver";
    private static WalkieTalkieActivity wtActivity = null;

    public void setContext(Context context) {
        wtActivity = (WalkieTalkieActivity) context;
    }

    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     * use the following cmd line to start activity first
     * am start com.example.android.sip/.WalkieTalkieActivity
     * use the following cmd line to trigger onReceive
     * am broadcast -a ANSWER_CALL  -n com.example.android.sip/.IncomingCallReceiver
     * am broadcast -a SEND_DTMF --ei dtmf 2 -n com.example.android.sip/.IncomingCallReceiver
     * am broadcast -a PROCESS_CHLD --ei chld 2 -n com.example.android.sip/.IncomingCallReceiver
     * am broadcast -a LIST_CURRENT_CALLS --ez shouldLog true -n com.example.android.sip/.IncomingCallReceiver
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        SipAudioCall incomingCall = null;

	if(wtActivity == null) {
	    Log.d(TAG, "onReceive should start Activity first!");
	    return;
	}
	//Log.d(TAG, "incoming intent " + intent);
	if(intent.getAction().equals(Constants.ACTION_INCOMING_CALL)) {//equalsIgnoreCase
	    Log.d(TAG, "ACTION_INCOMING_CALL received");
	} else if(intent.getAction().equals(Constants.ACTION_ANSWER_CALL)) {
	    Log.d(TAG, "ACTION_ANSWER_CALL received");
	    wtActivity.answerCall();
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_HANGUP_CALL)) {
	    Log.d(TAG, "ACTION_HANGUP_CALL received");
	    wtActivity.hangupCall();
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_SEND_DTMF)) {
	    Log.d(TAG, "ACTION_SEND_DTMF received");
	    int dtmf = intent.getIntExtra("dtmf", 0);
	    wtActivity.sendDtmf(dtmf);
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_PROCESS_CHLD)) {
	    Log.d(TAG, "ACTION_PROCESS_CHLD received");
	    int chld = intent.getIntExtra("chld", 0);
	    wtActivity.processChld(chld);
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_GET_NETWORK_OPERATOR)) {
	    Log.d(TAG, "ACTION_GET_NETWORK_OPERATOR received");
	    wtActivity.getNetworkOperator();
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_LIST_CURRENT_CALLS)) {
	    Log.d(TAG, "ACTION_LIST_CURRENT_CALLS received");
	    boolean shouldLog = intent.getBooleanExtra("shouldLog", false);
	    wtActivity.listCurrentCalls(shouldLog);
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_QUERY_PHONE_STATE)) {
	    Log.d(TAG, "ACTION_QUERY_PHONE_STATE received");
	    wtActivity.queryPhoneState();
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_GET_SUBSCRIBER_NUMBER)) {
	    Log.d(TAG, "ACTION_GET_SUBSCRIBER_NUMBER received");
	    wtActivity.getSubscriberNumber();
	    return;
	} else if(intent.getAction().equals(Constants.ACTION_MAKE_AUDIO_CALL)) {
	    Log.d(TAG, "ACTION_MAKE_AUDIO_CALL received");
	    wtActivity.makeAudioCall();
	    return;
	} else {
	    Log.d(TAG, "others received");
	    return;
        }

        try {

            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {		    
		    Log.d(TAG, "onRinging");
		    //wtActivity.incomingCall();
                    /*
                    try {
                        call.answerCall(30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
		    */
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
		    Log.d(TAG, "onCallEstablished");
		    wtActivity.incomingCallEstablished();
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
		    Log.d(TAG, "onCallEnded");
		    wtActivity.incomingCallEnded();
		}
            };
	    
            incomingCall = wtActivity.manager.takeAudioCall(intent, listener);
	    wtActivity.incomingCall(incomingCall);
	    //wtActivity.updateStatus("incomingCall....");
	    /*
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if(incomingCall.isMuted()) {
                incomingCall.toggleMute();
		Log.d(TAG, "incomingCall.isMuted toggleMute");
            }
	    */
        } catch (Exception e) {
	    Log.d(TAG, "incomingCall...exception:" + e);
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }

}
