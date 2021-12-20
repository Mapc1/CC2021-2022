package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server implements Runnable {
    public static final String LOG_FOLDER = Peer.LOG_FOLDER + "/Server";
    private static final String LOG_FILE = "/ServerLog.txt";
    private DatagramSocket socket;
    private int requestNum = 0;
    private Log logger;

    public Server(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        this.logger = new Log(LOG_FOLDER + LOG_FILE);
    }

    public void run() {
        try {
            while(true) {
                byte[] buffer = new byte[Protocol.messageSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);        
                socket.receive(packet);
 
                logger.write("Packet received. Passing it to client handler nº" + requestNum + "...", LogType.GOOD);

                Thread t = new Thread(new ServerHandler(packet, "/Request_" + requestNum));
                requestNum++;

                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
