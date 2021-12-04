package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ServerHandler implements Runnable { 
    DatagramSocket socket;
    Encryption e;

    public ServerHandler(DatagramSocket listenSocket, Encryption e) throws SocketException {
        this.e = e;
        this.socket = listenSocket;
        System.out.println("Connection open in: " + socket.getLocalPort());
    }

    public void run() {
        String msg = "AYAYA";
        byte[] buffer = new byte[512];

        while(msg != "OFF") {
            Arrays.fill(buffer, (byte) 0);
            
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] dataBuffer = packet.getData(); 

            String newMsg = new String(e.decrypt(packet.getData(),packet.getLength()),0,packet.getLength(),StandardCharsets.UTF_8);

            Arrays.fill(dataBuffer, (byte) 0);

            System.out.println(newMsg);
        }
    }
}
