package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    InetAddress ip;
    DatagramSocket socket;
    Scanner sc = new Scanner(System.in);
    int serverPort;
    Encryption e;

    public ClientHandler(int serverPort, String addr, Encryption e) throws UnknownHostException, SocketException {
        this.e = e;
        ip = InetAddress.getByName(addr);
        socket = new DatagramSocket();
        this.serverPort = serverPort;
        System.out.println("Connection open in: " + serverPort + "ip: " + addr);
    }

    public void run() {
        String msg = sc.nextLine();
        byte[] buffer = new byte[512];
        while(msg != "OFF") {
            Arrays.fill(buffer, (byte) 0);

            byte[] binary = msg.getBytes();
            buffer = e.encrypt(binary,binary.length);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, serverPort);

            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            msg = sc.nextLine();
        }
    }
}
