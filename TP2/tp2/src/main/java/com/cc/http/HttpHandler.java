package com.cc.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class HttpHandler implements Runnable {
    private Socket socket;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            String[] words = line.split(" ", 3);
            String type = words[1];

            String today = "";
            String httpResponse = "";
            if (type.equals("/")) {
                
                today = "Sou lindo";
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + today;
            } else {
                today = "Sou Feio";
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n" + today;

            }


            socket.getOutputStream()
                    .write(httpResponse.getBytes("UTF-8"));
            socket.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
