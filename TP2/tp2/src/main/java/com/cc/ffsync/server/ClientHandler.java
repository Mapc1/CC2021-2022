package com.cc.ffsync.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.cc.ffsync.FFSync;
import com.cc.ffsync.auth.Encryption;
import com.cc.ffsync.logs.Log;
import com.cc.ffsync.logs.LogType;
import com.cc.ffsync.protocol.Protocol;
import com.cc.ffsync.utils.FilesHandler;

public class ClientHandler implements Runnable { 
    Encryption e;
    
    DatagramSocket socket;
    int clientPort;
    InetAddress clientIP;

    double estimatedRTT = 500;
    double devRTT = 50;
    int timeout = 500;
    
    DatagramPacket connectPacket;
    DatagramPacket requestPacket;

    Log logger;

    public ClientHandler(DatagramPacket packet, String logFile) throws IOException {
        this.clientPort = packet.getPort();
        this.clientIP = packet.getAddress();

        this.socket = new DatagramSocket();
        this.socket.setSoTimeout((int) estimatedRTT);

        this.requestPacket = packet;
        this.logger = new Log(Server.LOG_FOLDER + logFile);
        this.e = new Encryption(logger);
    }
    
    public void run() {
        try {
            byte[] ackBuff = Protocol.createAckMessage((int) Protocol.LS_TYPE);
            DatagramPacket ackPacket = new DatagramPacket(ackBuff, ackBuff.length, clientIP, clientPort);
            socket.send(ackPacket);

            ByteBuffer requestBB = ByteBuffer.wrap(requestPacket.getData());
            byte requestType = requestBB.get();
		    
            switch(requestType) {
                case Protocol.LS_TYPE : sendMetaData(FFSync.SYNC_FOLDER); break;
                case Protocol.FILE_REQ_TYPE :
                    short size = requestBB.getShort();
                    byte[] buf = new byte[size];
                    System.arraycopy(requestBB.array(), 3, buf, 0, size);
                    String metadata = new String(buf);
                    sendFileData(metadata);
                    break;
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void sendFileData(String metadata) throws IOException {
        String filePath = FFSync.SYNC_FOLDER + "/" + metadata.split(";")[0];
        byte[] listenBuff = new byte[Protocol.messageSize];

        File f = new File(filePath);
        FileInputStream fis = new FileInputStream(f);

        long fileSize = Files.size(Paths.get(filePath));
        long nSeqs = fileSize / (Protocol.messageSize - 11);
        if((fileSize % (Protocol.messageSize - 11)) != 0) {
            nSeqs++;
        }
        sendNSeqs(nSeqs);

        long start = System.currentTimeMillis();

        long i = 0;
        while(i < nSeqs) {
            byte[] pacote = fis.readNBytes(Protocol.messageSize - 11);
            ByteBuffer pacoteBB = ByteBuffer.allocate(Protocol.messageSize);
            pacoteBB.put(Protocol.FILE_TYPE);
            pacoteBB.putLong(i);
            pacoteBB.putShort((short) pacote.length);
            pacoteBB.put(pacote);

            DatagramPacket packet = new DatagramPacket(pacoteBB.array(), pacoteBB.array().length, clientIP, clientPort);
            DatagramPacket ackPacket = new DatagramPacket(listenBuff, listenBuff.length);

            logger.write("Packet nº " + i + " sent. Awaiting response...", LogType.GOOD);

            boolean sent = false;
            while(!sent) {
                try {
                    long startRTT = System.currentTimeMillis();
                    socket.send(packet);
                    socket.receive(ackPacket);
                    ByteBuffer bb = ByteBuffer.wrap(ackPacket.getData());

                    byte answerType = bb.get();
                    long ackSeq = bb.getLong(2);

                    if(answerType == Protocol.ACK_TYPE && ackSeq == i) {
                        long endRTT = System.currentTimeMillis();
                        timeout = Protocol.calculateRTT(startRTT, endRTT, estimatedRTT, devRTT);
                        socket.setSoTimeout(timeout);
                        logger.write("ACK received nº " + ackSeq, LogType.GOOD);

                        sent = true;
                        i++;
                    }
                } catch (SocketTimeoutException e) {
                    // Increase timeout
                    timeout += 50;
                    socket.setSoTimeout(timeout);
                    logger.write("Timeout. Resending packet nº " + i, LogType.TIMEOUT);
                }
            }
        }

        double end = System.currentTimeMillis();
        double secsElapsed = (end - start) / 1000;
        double bitsPerSec = (fileSize * 8) / secsElapsed;

        logger.newLine();
        logger.write("Transfer complete! Here are some stats... :)", LogType.GOOD);
        logger.write("File size: " + fileSize + " bytes", LogType.INFO);
        logger.write("Time elapsed: " + secsElapsed + "s", LogType.INFO);
        logger.write("Transfer speed: " + bitsPerSec + " bits/s", LogType.INFO);
        fis.close();
    }
 
    private void sendMetaData(String path) throws IOException {
        byte[] listenBuff = new byte[Protocol.messageSize];

        List<String> filesName = FilesHandler.readAllFilesMetadata(path);
        filesName = removeSyncing(filesName);
        List<byte[]> pacotes = Protocol.createInfoMessage(filesName);

        long nSeqs = pacotes.size();
        sendNSeqs(nSeqs);

        long i = 0;
        nSeqs--;
        while(i <= nSeqs) {
            byte[] pacote = pacotes.get((int) i);
            //byte[] encrypted = e.encrypt(pacote, pacote.length);
            DatagramPacket packet = new DatagramPacket(pacote, pacote.length, clientIP, clientPort);
            DatagramPacket ackPacket = new DatagramPacket(listenBuff, listenBuff.length);
            
            socket.send(packet);
            logger.write("Packet nº " + i + " sent, Awaiting response...", LogType.GOOD);

            boolean sent = false;
            while(!sent) {
                try {
                    socket.receive(ackPacket);
                    //byte[] decrypted = e.decrypt(ackPacket.getData(), ackPacket.getData().length);
                    ByteBuffer data = ByteBuffer.wrap(ackPacket.getData());

                    byte answerType = data.get();
                    long ackSeq = data.getLong(2);

                    if(answerType == Protocol.ACK_TYPE) {
                        logger.write("ACK received nº " + ackSeq, LogType.GOOD);
                        sent = true;
                        i = ackSeq + 1;
                    }
                } catch (SocketTimeoutException e) {
                    socket.send(packet);
                    logger.write("Timeout. Resending packet nº " + i, LogType.TIMEOUT);
                }
            }
        }
    }

    private void sendNSeqs(long nSeqs) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(9);

        bb.put(Protocol.SEQ_TYPE);
        bb.putLong(nSeqs);
        DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, clientIP, clientPort);

        byte[] ackBuffer = new byte[Protocol.messageSize];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
        
        
        boolean ack = false;
        while(!ack) {
            try {
                socket.send(packet);
                socket.receive(ackPacket);
                //byte[] decrypted = e.decrypt(ackPacket.getData(), ackPacket.getData().length);
                long ackSeqs = ByteBuffer.wrap(ackPacket.getData()).getLong(2);
                if(ackBuffer[0] == Protocol.ACK_TYPE && ackSeqs == nSeqs) {
                    ack = true;
                }            
            } catch (SocketTimeoutException e) {
                //socket.send(packet);
            }
        }
    }

    private List<String> removeSyncing(List<String> files) { 
        List<String> newList = new ArrayList<>();
        for(String file : files) {
            String fileName = file.split(";")[0];
            if(!FFSync.LW.contains(fileName)) {
                newList.add(file);
            }
        }
        return newList;
    }
}
