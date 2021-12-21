package com.cc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

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

        List<Thread> threads = new ArrayList<>();

        try {
            server = new Thread(new Server(listenPort));
            //client = new Thread(new Client(sendPort, ip));
            //client.start();
            server.start();

            for(int i = 2; i < args.length; i+=2) {
                InetAddress ip1 = InetAddress.getByName(args[i+1]);
                int porta = Integer.parseInt(args[i]);
                Thread t = new Thread(new Client(porta, ip1));
                t.start();
                threads.add(t);
            }

            for(Thread t : threads) {
                t.join();
            }
            server.join();
        } catch (NumberFormatException | SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
