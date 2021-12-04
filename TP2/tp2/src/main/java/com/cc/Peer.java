package com.cc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Peer 
{
    public static void main( String[] args ) throws IOException {
        Thread server;
        Thread client;

        InetAddress ip = InetAddress.getByName(args[2]);
        DatagramSocket listenSocket = new DatagramSocket(Integer.parseInt(args[0]));
        int sendPort = Integer.parseInt(args[1]);

        Encryption encrypt = new Encryption(ip, listenSocket, sendPort);

        try {
            encrypt.auth();

            server = new Thread(new ServerHandler(listenSocket,encrypt));
            client = new Thread(new ClientHandler(Integer.parseInt(args[1]), args[2], encrypt));
            client.start();
            server.start();

            server.join();
            client.join();
        } catch (NumberFormatException | UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
