package com.cc;

import com.cc.ftrapid.FTRapid;
import com.cc.http.HttpServer;

public class FFSync {
    public static void main(String[] args) throws InterruptedException {
        Thread ffsync = new Thread(new FTRapid(args));
        Thread httpServer = new Thread(new HttpServer());

        ffsync.start();
        httpServer.start();

        ffsync.join();
        httpServer.join();
    }
}
