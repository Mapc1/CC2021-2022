package com.cc;

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
import java.util.List;

public class ServerHandler implements Runnable { 
    Encryption e;
    
    DatagramSocket socket;
    int clientPort;
    InetAddress clientIP;

    double estimatedRTT = 4000;
    double devRTT = 100;
    
    DatagramPacket connectPacket;
    DatagramPacket requestPacket;

    Log logger;

    public ServerHandler(DatagramPacket packet, String logFile) throws IOException {
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
            //connect();
/*
            byte[] reqBuff = new byte[Protocol.messageSize];
            DatagramPacket requestPacket = new DatagramPacket(reqBuff, reqBuff.length);

            boolean requestReceived = false;
            while(!requestReceived) {
                try {
                    socket.receive(requestPacket);
                    requestReceived = true;
                } catch (SocketTimeoutException e) {}
            }
*/
            byte[] ackBuff = Protocol.createAckMessage((int) Protocol.LS_TYPE);
            DatagramPacket ackPacket = new DatagramPacket(ackBuff, ackBuff.length, clientIP, clientPort);
            socket.send(ackPacket);

            ByteBuffer requestBB = ByteBuffer.wrap(requestPacket.getData());
            byte requestType = requestBB.get();
		    
            switch(requestType) {
                case Protocol.LS_TYPE : sendMetaData(Peer.SYNC_FOLDER); break;
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

    private void connect() throws IOException {
        byte[] inBuffer = new byte[Protocol.messageSize];

        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        
        byte[] ack = { Protocol.ACK_TYPE };
        DatagramPacket outPacket = new DatagramPacket(ack, ack.length, clientIP, clientPort);
        socket.send(outPacket);

        ByteBuffer otherKeyBB = ByteBuffer.wrap(connectPacket.getData());
        byte type = otherKeyBB.get();
        short size = otherKeyBB.getShort();

        byte[] otherKeyArr = new byte[size];
        System.arraycopy(otherKeyBB.array(), 3, otherKeyArr, 0, size);

        /*boolean otherKeyReceived = false;
        while(!otherKeyReceived) {
            try {
                socket.receive(inPacket);

                ByteBuffer otherKeyBB = ByteBuffer.wrap(inPacket.getData());
                byte type = otherKeyBB.get();
                if(type == Protocol.KEY_TYPE) {
                    short size = otherKeyBB.getShort();

                    otherKeyArr = new byte[size];
                    System.arraycopy(otherKeyBB.array(), 3, otherKeyArr, 0, size);
                    otherKeyReceived = true;

                    serverPort = inPacket.getPort();
                    serverIP = inPacket.getAddress();

                    byte[] ack = new byte[1];
                    ack[0] = Protocol.ACK_TYPE;

                    DatagramPacket outPacket = new DatagramPacket(ack, ack.length, serverIP, serverPort);
                    socket.send(outPacket);
                }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Did not receive anything. Still waiting...");
            }
        }
*/
        byte[] publicKey = e.calcPublicKey();
        e.calcSharedKey(otherKeyArr);

        size = (short) publicKey.length;
        ByteBuffer pubKeyBB = ByteBuffer.allocate(3+size);
        pubKeyBB.put(Protocol.KEY_TYPE);
        pubKeyBB.putShort(size);
        pubKeyBB.put(publicKey);

        outPacket = new DatagramPacket(pubKeyBB.array(), pubKeyBB.array().length, clientIP, clientPort);

        boolean ackReceived = false;
        while(!ackReceived) {
            try {
            socket.send(outPacket);
            socket.receive(inPacket);
            
            if(inPacket.getData()[0] == Protocol.ACK_TYPE) {
                ackReceived = true;
            }
            } catch (SocketTimeoutException e) {
                logger.write("Timeout ocurred. Sending ACK again...", LogType.TIMEOUT);
            }
        }

        logger.write("We're connected! YAY", LogType.GOOD);
    }

    private void sendFileData(String metadata) throws IOException {
        String filePath = Peer.SYNC_FOLDER + "/" + metadata.split(";")[0];

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

            socket.send(packet);
            logger.write("Packet nº " + i + " sent. Awaiting response...", LogType.GOOD);

            boolean sent = false;
            while(!sent) {
                try {
                    socket.receive(ackPacket);
                    ByteBuffer bb = ByteBuffer.wrap(ackPacket.getData());

                    byte answerType = bb.get();
                    long ackSeq = bb.getLong(2);

                    if(answerType == Protocol.ACK_TYPE) {
                        logger.write("ACK received nº " + ackSeq, LogType.GOOD);

                        sent = true;
                        if(ackSeq == i) {
                            i++;
                        } else {
                            i = ackSeq + 1;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    socket.send(packet);
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
        List<byte[]> pacotes = Protocol.createInfoMessage(path);
        byte[] listenBuff = new byte[Protocol.messageSize];

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

                        if(ackSeq == i) {
                            i++;
                        } else {
                            i = ackSeq + 1;
                        }
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
        
        socket.send(packet);
        
        boolean ack = false;
        while(!ack) {
            try {
                socket.receive(ackPacket);
                //byte[] decrypted = e.decrypt(ackPacket.getData(), ackPacket.getData().length);
                long ackSeqs = ByteBuffer.wrap(ackPacket.getData()).getLong(2);
                if(ackBuffer[0] == Protocol.ACK_TYPE && ackSeqs == nSeqs) {
                    ack = true;
                }            
            } catch (SocketTimeoutException e) {
                socket.send(packet);
            }
        }
    }
}
