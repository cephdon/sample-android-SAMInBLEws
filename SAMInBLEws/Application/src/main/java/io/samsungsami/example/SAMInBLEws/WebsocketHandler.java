package io.samsungsami.example.SAMInBLEws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebsocketHandler extends WebSocketClient {
    WebsocketEvents outsideWorld;

    public WebsocketHandler(URI serverURI, WebsocketEvents outsideWorld) {
        super(serverURI);
        this.outsideWorld = outsideWorld;
    }

    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        outsideWorld.onOpen(handshakedata);
    }

    @Override
    public void onMessage( String message ) {
        outsideWorld.onMessage(message);

    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        outsideWorld.onClose(code, reason, remote);
    }

    @Override
    public void onError( Exception ex ) {
        outsideWorld.onError(ex);
    }

}
