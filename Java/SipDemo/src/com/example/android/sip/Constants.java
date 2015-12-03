
package com.example.android.sip;

public class Constants {

    public static final int CALL_STATE_ACTIVE = 0;
    public static final int CALL_STATE_HELD = 1;
    public static final int CALL_STATE_DIALING = 2;
    public static final int CALL_STATE_ALERTING = 3;
    public static final int CALL_STATE_INCOMING = 4;
    public static final int CALL_STATE_WAITING = 5;
    public static final int CALL_STATE_IDLE = 6;

    public static final int MSG_ANSWER_CALL = 1;
    public static final int MSG_HANGUP_CALL = 2;
    public static final int MSG_SEND_DTMF = 3;
    public static final int MSG_PROCESS_CHLD = 4;
    public static final int MSG_GET_NETWORK_OPERATOR = 5;
    public static final int MSG_LIST_CURRENT_CALLS = 6;
    public static final int MSG_QUERY_PHONE_STATE = 7;
    public static final int MSG_GET_SUBSCRIBER_NUMBER = 8;
    public static final int MSG_INCOMING_CALL = 9;

    public static final String ACTION_INCOMING_CALL = "android.SipDemo.INCOMING_CALL";
    public static final String ACTION_ANSWER_CALL = "ANSWER_CALL";
    public static final String ACTION_HANGUP_CALL = "HANGUP_CALL";
    public static final String ACTION_SEND_DTMF = "SEND_DTMF";
    public static final String ACTION_PROCESS_CHLD = "PROCESS_CHLD";
    public static final String ACTION_GET_NETWORK_OPERATOR = "GET_NETWORK_OPERATOR";
    public static final String ACTION_LIST_CURRENT_CALLS = "LIST_CURRENT_CALLS";
    public static final String ACTION_QUERY_PHONE_STATE = "QUERY_PHONE_STATE";
    public static final String ACTION_GET_SUBSCRIBER_NUMBER = "GET_SUBSCRIBER_NUMBER";
    //extra methods
    public static final String ACTION_MAKE_AUDIO_CALL = "MAKE_AUDIO_CALL";

    public static final String SUBSCRIBER_NUMBER = "0910123456";
    public static final String NETWORK_OPERATOR = "iptel";
    //SIP Address of the remote side, this is used in a outgoing call
    public static final String REMOTE_SIP_ADDRESS = "user@iptel.org";
    public static final String USERNAME = "username";
    public static final String DOMAIN = "iptel.org";
    public static final String PASSWORD = "passwd";
}
