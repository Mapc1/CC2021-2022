package com.cc;

import com.cc.ffsync.FFSync;

public class FTRapid {
    public static void main(String[] args) throws InterruptedException {
        Thread ffsync = new Thread(new FFSync(args));

        ffsync.start();
        ffsync.join();
    }
}
