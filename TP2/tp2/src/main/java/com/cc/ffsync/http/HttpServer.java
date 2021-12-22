package com.cc.ffsync.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer implements Runnable {
    private Boolean on = true;

    @Override
    public void run() {
        try {
            final ServerSocket server = new ServerSocket(8080);
            while (on) {
                Socket socket = server.accept();
                Thread t = new Thread(new HttpHandler(socket));
                t.start();
            }
            server.close();
        } catch (IOException e) {
            on = false;
            System.out.println("[HttpServer] Shutting down...");
            e.printStackTrace();
        }
    }
}