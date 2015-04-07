package io.samsungsami.example.SAMInBLEws;

import org.java_websocket.handshake.ServerHandshake;

/**
 * Created by ari
 */
public interface WebsocketEvents {

    public void onOpen(ServerHandshake handshakedata);

    public void onMessage(String message);

    public void onClose(int code, String reason, boolean remote);

    public void onError(Exception ex);
}
