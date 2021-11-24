package com.cc;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Peer 
{
    public static void main( String[] args ) {
        Thread server;
        Thread client;
        try {
            server = new Thread(new ServerHandler(Integer.parseInt(args[0])));
            client = new Thread(new ClientHandler(Integer.parseInt(args[1]), args[2]));
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
