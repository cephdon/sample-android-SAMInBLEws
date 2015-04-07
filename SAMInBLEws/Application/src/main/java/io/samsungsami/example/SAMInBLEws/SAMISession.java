/*
 * Copyright (C) 2015 Samsung Electronics Co., Ltd.
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

package io.samsungsami.example.SAMInBLEws;

import android.os.Looper;
import android.os.Handler;
import android.util.Log;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import io.samsungsami.api.DevicesApi;
import io.samsungsami.api.MessagesApi;
import io.samsungsami.api.UsersApi;

public class SAMISession {
    private static final String TAG = SAMISession.class.getSimpleName();

    public static final String SAMI_AUTH_BASE_URL = "https://accounts.samsungsami.io";
    public static final String CLIENT_ID = "YOUR CLIENT APP ID";
    public static final String REDIRECT_URL = "android-app://redirect";
    public static final String SAMI_REST_URL = "https://api.samsungsami.io/v1.1";
    public static final String SAMI_WEBSOCKET_URL = "wss://api.samsungsami.io/v1.1/websocket?ack=true";

    private static final String AUTHORIZATION = "Authorization";

    // For websocket message
    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String SOURCE_DEVICE = "sdid";
    private static final String TIME_STAMP = "ts";
    private static final String HEART_RATE = "heart_rate";


    // SAMI device type id used by this app
    // device name: "SAMI Example Heart Rate Tracker"
    // As a hacker, get the device type id using the following ways
    //   -- login to https://api-console.samsungsami.io/sami
    //   -- Click "Get Device Types" api
    //   -- Fill in device name as above
    //   -- Click "Try it"
    //   -- The device type id is "id" field in the response body
    // You should be able to get this device type id programmatically.
    // Consult https://blog.samsungsami.io/mobile/development/2015/02/09/developing-with-sami-part-2.html#get-the-withings-device-info
    //
    public static final String DEVICE_TYPE_ID_HEART_RATE_TRACKER = "dtaeaf898b4db9418baab77563b7ea2254";

    private static SAMISession instance;

    private UsersApi mUsersApi = null;
    private DevicesApi mDevicesApi = null;
    private MessagesApi mMessagesApi = null;

    private String mAccessToken = null;
    private String mUserId = null;
    private String mDeviceId = null;

    private Websocket mWebsocket;

    private Handler mHandler;

    public static SAMISession getInstance() {
        if (instance == null) {
            instance = new SAMISession();
        }
        return instance;
    }

    private SAMISession() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "Constructor is not called in UI thread ");
        }
        mHandler = new Handler();
    }

    public String getAuthorizationRequestUri() {
        //https://accounts.samsungsami.io/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=http://localhost:81/samidemo/index.php
        return SAMISession.SAMI_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + SAMISession.CLIENT_ID + "&redirect_uri=" + SAMISession.REDIRECT_URL;
    }

    public String getLogoutRequestUri() {
        //https://accounts.samsungsami.io/logout?redirect_uri=http://localhost:81/samidemo/index.php
        return SAMISession.SAMI_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + SAMISession.CLIENT_ID + "&redirect_uri=" + SAMISession.REDIRECT_URL;
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set a invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public void setupSamiRestApis() {
        // Invoke the appropriate API
        mUsersApi = new UsersApi();
        mUsersApi.setBasePath(SAMI_REST_URL);
        mUsersApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);

        mDevicesApi = new DevicesApi();
        mDevicesApi.setBasePath(SAMI_REST_URL);
        mDevicesApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);

        mMessagesApi = new MessagesApi();
        mMessagesApi.setBasePath(SAMI_REST_URL);
        mMessagesApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);
    }

    public void logout() {
        reset();
    }

    public UsersApi getUsersApi() {
        return mUsersApi;
    }

    public DevicesApi getDevicesApi() {
        return mDevicesApi;
    }

    public MessagesApi getMessagesApi() {
        return mMessagesApi;
    }

    public void setUserId(String uid) {
        if (uid == null || uid.length() <= 0) {
            Log.w(TAG, "setUserId() get null uid");
        }
        mUserId = uid;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setDeviceId(String did) {
        if (did == null || did.length() <= 0) {
            Log.w(TAG, "setDeviceId() get null did");
        }
        mDeviceId = did;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public void reset() {
        mUsersApi = null;
        mDevicesApi = null;
        mMessagesApi = null;

        mAccessToken = null;
        mUserId = null;
        mDeviceId = null;

        mWebsocket = null;
    }

    /**
     *
     *
     */
    public void onNewHeartRate(final int heartRate, final long ts) {
        mHandler.post(new Runnable() {
            @Override
            public void run() { sendViaWebsocket(heartRate, ts);}
        });
    }

    /**
     *
     *
     */
    public void setupWebsocket() {
        mHandler.post(new Runnable() {
            @Override
            public void run() { connectWebsocket();}
        });
    }

    /**
     * Setup websocket bidirectional pipeline and register to SAMI
     */
    private void connectWebsocket() {
        if(mWebsocket == null) {
            mWebsocket = new Websocket();
        }

        if(!mWebsocket.isConnecting() && !mWebsocket.isConnected()) {
            mWebsocket.connect(SAMI_WEBSOCKET_URL, new WebsocketEvents() {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    final String message = getWSRegisterMessage();
                    mWebsocket.send(message);
                    Log.d(TAG, "WebSocket: onOpen calling websocket.send(" + message + ")");
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket: onMessage(" + message + ")");
                 }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket: onClose() code = " + code + "; reason = " + reason + "; remote = " + remote);
                 }

                @Override
                public void onError(Exception ex) {
                    Log.d(TAG, "WebSocket: onError() errorMsg = " + ex.getMessage());
                }
            });
        }
    }

    public void disconnectWebSocket() {
        if (mWebsocket != null && (mWebsocket.isConnecting() || mWebsocket.isConnected())) {
            mWebsocket.disconnect();
        }
    }
    /**
     * Connects to /websocket
     *
     */
    private void sendViaWebsocket(final int heartRate, final long ts) {
        final String message = getWSMessage(heartRate, ts);
        if (mWebsocket == null) {
            mWebsocket = new Websocket();
        }
        if (!mWebsocket.isConnected()) {
            setupWebsocket();
        }
        if (mWebsocket.isConnected()) {
            Log.d(TAG, "sendViaWebsocket: send(" + message +")");
            mWebsocket.send(message);
        }
    }

    /**
     * Returns JSON payload of the registration message for Bi-directional websocket
     * @return
     */
    private String getWSRegisterMessage(){
        JSONObject message = new JSONObject();
        try {
            message.put(TYPE, "register");
            message.put(SOURCE_DEVICE, mDeviceId);
            message.put(AUTHORIZATION, "bearer " + mAccessToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message.toString();
    }

    /**
     * Returns a full JSON payload to send a message for HeartRateTracker device type
     * @param heartRate
     * @return
     */
    private String getWSMessage(int heartRate, long ts){
        JSONObject message = new JSONObject();
        try {
            message.put(SOURCE_DEVICE, mDeviceId);
            message.put(TIME_STAMP, ts);
            JSONObject data = new JSONObject();
            data.put(HEART_RATE, heartRate);
            message.put(DATA, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message.toString();
    }

}
