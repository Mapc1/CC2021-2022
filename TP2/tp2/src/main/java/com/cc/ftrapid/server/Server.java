package com.cc.ftrapid.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.cc.ftrapid.FTRapid;
import com.cc.ftrapid.logs.Log;
import com.cc.ftrapid.logs.LogType;
import com.cc.ftrapid.protocol.Protocol;

public class Server implements Runnable {
    public static final int PORT = 80;
    public static final String LOG_FOLDER = FTRapid.LOG_FOLDER + "/Server";
    private static final String LOG_FILE = "/ServerLog.txt";
    
    private DatagramSocket socket;
    private int requestNum = 0;
    private Log logger;

    public Server() throws IOException {
        this.socket = new DatagramSocket(PORT);
        this.logger = new Log(LOG_FOLDER + LOG_FILE);
        System.out.println("Listening on port " + PORT);
    }

    public void run() {
        boolean on = true;
        try {
            while(on) {
                byte[] buffer = new byte[Protocol.messageSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);        
                socket.receive(packet);
 
                logger.write("Packet received. Passing it to client handler nº" + requestNum + "...", LogType.GOOD);

                Thread t = new Thread(new ClientHandler(packet, "/Request_" + requestNum + ".txt"));
                requestNum++;

                t.start();
            }
        } catch (IOException e) {
            on = false;
            System.out.println("[Server] Shutting down...");
        }
    }
}
