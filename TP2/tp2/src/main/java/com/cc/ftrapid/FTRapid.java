package com.cc.ftrapid;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.cc.ftrapid.client.Client;
import com.cc.ftrapid.server.Server;
import com.cc.ftrapid.utils.ListWrapper;

public class FTRapid implements Runnable {
    public static final String LOG_FOLDER = "logs";
    public static String SYNC_FOLDER;

    public static ListWrapper LW  = new ListWrapper();

    private String[] args;

    public FTRapid(String[] args) {
        this.args = args;
    }

    public void run() {
        Thread server;
        FTRapid.SYNC_FOLDER = args[0];
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
        } catch (NumberFormatException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
