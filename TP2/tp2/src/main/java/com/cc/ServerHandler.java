package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ServerHandler implements Runnable { 
    DatagramSocket socket;
    byte[] buffer = new byte[512];

    public ServerHandler(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        String msg = "AYAYA";

        while(msg != "OFF") {
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] dataBuffer = packet.getData(); 

            msg = new String(dataBuffer, StandardCharsets.UTF_8);

            System.out.println(msg);
        }
    }
}
