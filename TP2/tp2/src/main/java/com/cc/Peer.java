package com.cc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class Peer 
{
    public static void main( String[] args ) throws IOException {
        Thread server;
        Thread client;

        InetAddress ip = InetAddress.getByName(args[3]);
        int listenPort = Integer.parseInt(args[1]);
        int sendPort = Integer.parseInt(args[2]);

        try {
            server = new Thread(new Server(args[0], listenPort));
            client = new Thread(new Client(args[0], sendPort, ip));
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
