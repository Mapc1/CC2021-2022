package com.cc;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private Protocolo p;

    public ClientHandler(Protocolo p) throws UnknownHostException, SocketException {
        this.p = p;
        System.out.println("Connection open in: " + p.getPort() + " ip: " + p.getAddress().toString());
    }

    public void run() {
        try {
            p.connect();

            String msg = new String();
            while(!msg.equals("FYN")) {
                byte[] data = p.receiveData();

                msg = new String(data,0, data.length, StandardCharsets.UTF_8);
                if(!msg.equals("FYN"))
                        System.out.print(msg);

                byte[] buffer2 ="ACK".getBytes();
                p.sendData(buffer2);
            }
            System.out.println();
        } catch (IOException | ConnectionFailureException e) {
            e.printStackTrace();
        }
    }
}
