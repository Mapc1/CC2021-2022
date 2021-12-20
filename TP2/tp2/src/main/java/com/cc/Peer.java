package com.cc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class Peer 
{
    //public static final String LOG_FOLDER = System.getProperty("user.home") + "/logs";
    public static final String LOG_FOLDER =  "logs";
    public static String SYNC_FOLDER;
    public static void main( String[] args ) throws IOException {
        Thread server;
        Thread client;

        Peer.SYNC_FOLDER = args[0];
        int listenPort = Integer.parseInt(args[1]);
        int sendPort = Integer.parseInt(args[2]);
        InetAddress ip = InetAddress.getByName(args[3]);

        try {
            server = new Thread(new Server(listenPort));
            client = new Thread(new Client(sendPort, ip));
            client.start();
            server.start();

            server.join();
            client.join();
        } catch (NumberFormatException | SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
