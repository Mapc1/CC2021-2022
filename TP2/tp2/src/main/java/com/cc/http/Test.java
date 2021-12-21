package com.cc.http;

import java.net.ServerSocket;
import java.net.Socket;

public class Test {

    public static void main(String[] args) throws Exception {
        final ServerSocket server = new ServerSocket(8080);
        System.out.println("Listening for connection on port 8080 ....");
        while (true) {
            Socket socket = server.accept();
            Thread t = new Thread(new HttpHandler(socket));
            t.start();
        }
    }
}