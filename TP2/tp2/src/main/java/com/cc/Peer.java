package com.cc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Peer 
{

    public static void main( String[] args ) throws IOException {
        Thread server;
        Thread client;

        InetAddress ip = InetAddress.getByName(args[2]);
        int listenPort = Integer.parseInt(args[0]);
        int sendPort = Integer.parseInt(args[1]);

        Protocolo serverProtocol = new Protocolo(ip, listenPort, sendPort);
        Protocolo clientProtocol = new Protocolo(ip, sendPort);

        try {         
            server = new Thread(new ServerHandler(serverProtocol));
            client = new Thread(new ClientHandler(clientProtocol));
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
