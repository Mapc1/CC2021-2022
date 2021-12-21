package com.cc.ffsync;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.cc.ffsync.client.Client;
import com.cc.ffsync.server.Server;
import com.cc.ffsync.utils.ListWrapper;

public class FFSync 
{
    public static final String LOG_FOLDER = System.getProperty("user.home") + "/logs";
    //public static final String LOG_FOLDER =  "logs";
    public static String SYNC_FOLDER;

    public static ListWrapper LW  = new ListWrapper();

    public static void main( String[] args ) throws IOException {
        Thread server;
        //Thread client;

        FFSync.SYNC_FOLDER = args[0];
//        InetAddress ip = InetAddress.getByName(args[1]);
        List<Thread> threads = new ArrayList<>();

        try {
            server = new Thread(new Server());
            //client = new Thread(new Client(ip));

//            client.start();
            server.start();

            for(int i = 1; i < args.length; i++) {
                InetAddress ip = InetAddress.getByName(args[i]);
                Thread t = new Thread(new Client(ip));
                t.start();
                threads.add(t);
            }

            for(Thread t : threads) {
                t.join();
            }
            threads = new ArrayList<>();

//            client.join();
            server.join();
        } catch (NumberFormatException | SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
