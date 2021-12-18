package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server implements Runnable {
    private String syncFolder;
    private DatagramSocket socket;

    public Server(String syncFolder, int port) throws SocketException {
        this.syncFolder = syncFolder;
        socket = new DatagramSocket(port);
    }

    public void run() {
        try {
            while(true) {
                byte[] buffer = new byte[Protocol.messageSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);        
                socket.receive(packet);
 
                Thread t = new Thread(new ServerHandler(packet, syncFolder));

                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
