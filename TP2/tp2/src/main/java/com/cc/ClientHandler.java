package com.cc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    InetAddress ip;
    DatagramSocket socket;
    Scanner sc = new Scanner(System.in);
    int serverPort;

    public ClientHandler(int serverPort, String addr) throws UnknownHostException, SocketException {
        ip = InetAddress.getByName(addr);
        socket = new DatagramSocket();
        this.serverPort = serverPort;
        System.out.println("Connection open in: " + serverPort + "ip: " + addr);
    }

    public void run() {
        String msg = sc.nextLine();
        while(msg != "OFF") {
            byte[] buffer = new byte[512];

            try {
                buffer = msg.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

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
