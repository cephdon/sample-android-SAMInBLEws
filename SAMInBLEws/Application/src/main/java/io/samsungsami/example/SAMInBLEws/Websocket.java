package io.samsungsami.example.SAMInBLEws;

import android.util.Log;

import org.java_websocket.WebSocketImpl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by ari
 */
public class Websocket {
    private WebsocketHandler wssClient;

    public void connect(String url, WebsocketEvents websocketEvents){
        System.setProperty("http.keepAlive", "false");
        HttpsURLConnection
                .setDefaultHostnameVerifier(new HostnameVerifier() {

                    public boolean verify(String hostname,
                                          SSLSession session) {
                        return true;
                    }
                });
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        WebSocketImpl.DEBUG = true;
        try {
            wssClient = new WebsocketHandler( new URI( url ), websocketEvents );
            wssClient.setSocket( factory.createSocket() );
            wssClient.connectBlocking();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d("Err", e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Err", e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d("Err", e.toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d("Err", e.toString());
        }
    }

    public void disconnect(){
        if(wssClient != null)
            wssClient.close();
    }

    public void send(String message){
        if(wssClient != null)
            wssClient.send(message);
    }

    public boolean isConnected(){
        return (wssClient != null && wssClient.isOpen());
    }
    public boolean isConnecting(){
        return (wssClient != null && wssClient.isConnecting());
    }
}
