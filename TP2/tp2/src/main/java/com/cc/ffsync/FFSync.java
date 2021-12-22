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
    public static final String LOG_FOLDER = "logs";
    public static String SYNC_FOLDER;

    public static ListWrapper LW  = new ListWrapper();

    public static void main( String[] args ) throws IOException {
        Thread server;

        FFSync.SYNC_FOLDER = args[0];
        List<Thread> threads = new ArrayList<>();

        try {
            server = new Thread(new Server());
            server.start();

            for(int i = 1; i < args.length; i++) {
                InetAddress ip = InetAddress.getByName(args[i]);
                Thread t = new Thread(new Client(ip, args[i]));
                t.start();
                threads.add(t);
            }

            for(Thread t : threads) {
                t.join();
            }
            threads = new ArrayList<>();

            server.join();
        } catch (NumberFormatException | SocketException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
