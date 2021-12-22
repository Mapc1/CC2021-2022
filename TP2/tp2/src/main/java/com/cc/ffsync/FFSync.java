package com.cc.ffsync;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.cc.ffsync.client.Client;
import com.cc.ffsync.http.HttpServer;
import com.cc.ffsync.server.Server;
import com.cc.ffsync.utils.ListWrapper;

public class FFSync implements Runnable {
    public static final String LOG_FOLDER = "logs";
    public static String SYNC_FOLDER;

    public static ListWrapper LW  = new ListWrapper();

    private String[] args;

    public FFSync(String[] args) {
        this.args = args;
    }

    public void run() {
        Thread server;
        Thread httpServer;

        // FFSync.SYNC_FOLDER = args[0];
        List<Thread> threads = new ArrayList<>();

        try {
            server = new Thread(new Server());
            server.start();
            httpServer = new Thread(new HttpServer());
            httpServer.start();

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
        } catch (NumberFormatException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
