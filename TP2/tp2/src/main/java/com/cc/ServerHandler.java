package com.cc;

import java.io.IOException;
import java.net.SocketException;

public class ServerHandler implements Runnable { 
    private Protocolo p;

    public ServerHandler(Protocolo p) throws SocketException {
        this.p = p;
        System.out.println("Connection open in: " + p.getSocket().getLocalPort());
    }

    public void run() {
        try {
            p.connect();
            byte[] data = "The quick brown fox jumps over the lazy dog".getBytes();
            p.sendData(data);

            byte[] fyn = "FYN".getBytes();
            p.sendData(fyn);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConnectionFailureException e) {
            e.printStackTrace();
            return;
        }
    }
}
