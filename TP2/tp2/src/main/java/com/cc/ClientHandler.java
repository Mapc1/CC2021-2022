package com.cc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public class ClientHandler implements Runnable {
    private static final int MAX_TRIES = 10;

    DatagramSocket socket;
    InetAddress serverIP;
    int serverPort;
    Encryption e;
    double estimatedRTT = 4000;
    double devRTT = 100;
    int timeout = 4000;

    Log logger;

    String metadata;

    public ClientHandler(String metadata, InetAddress serverIP, int serverPort) throws IOException {
        this.e = new Encryption(logger);
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout((int) estimatedRTT);

        this.metadata = metadata;
        String fileName = metadata.split(";")[0];
        this.logger = new Log(Client.LOG_FOLDER + fileName + ".txt");
        logger.write("Connection open in: " + serverPort + " ip: " + serverIP.getAddress(), LogType.GOOD);
    }

    public void run() {
        try {
            //connect();

            sendGetFile(metadata);
            getFileData(metadata);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void connect() throws IOException {
        byte[] buffer = new byte[Protocol.messageSize];
        byte[] publicKey = e.calcPublicKey();
        short size = (short) publicKey.length;

        ByteBuffer publicKeyBB = ByteBuffer.allocate(3+size);
        publicKeyBB.put(Protocol.KEY_TYPE);
        publicKeyBB.putShort(size);
        publicKeyBB.put(publicKey);

        DatagramPacket packet = new DatagramPacket(publicKeyBB.array(), publicKeyBB.array().length, serverIP, serverPort);

        boolean ackReceived = false;
        while(!ackReceived) {
            try {
                socket.send(packet);
                logger.write("PubKey sent, awaiting response...", LogType.GOOD);

                socket.receive(packet);

                if(packet.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                }
            } catch (SocketTimeoutException e) {
                logger.write("Timeout ocurred. Trying again...", LogType.TIMEOUT);
            }
        }

        logger.write("Ack received!", LogType.GOOD);
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);

        boolean otherKeyReceived = false;
        while(!otherKeyReceived) {
            try {
                socket.receive(response);      
                logger.write("Packet received. Checking contents...", LogType.GOOD);

                ByteBuffer rBB = ByteBuffer.wrap(response.getData());
                byte type = rBB.get();

                if(type == Protocol.KEY_TYPE) {
                    logger.write("Packet type correct. Reading key...", LogType.GOOD);
                    short rSize = rBB.getShort();
                    byte[] otherKey = new byte[rSize];
                    System.arraycopy(rBB.array(), 3, otherKey, 0, rSize);

                    e.calcSharedKey(otherKey);
                    otherKeyReceived = true;

                    byte[] ack = {Protocol.ACK_TYPE};
                    packet = new DatagramPacket(ack, ack.length, serverIP, serverPort);
                    socket.send(packet);
                } else { 
                    logger.write("Wrong type packet. Discarding it...", LogType.ERROR);
                }
            } catch (SocketTimeoutException e) {
                logger.write("Did not receive a key. Still waiting...", LogType.TIMEOUT);
            }
        }
        logger.write("We're connected! YAY", LogType.GOOD);
    }

    private void sendGetFile(String metaData) throws IOException {
        byte[] metaBuff = metaData.getBytes();
        ByteBuffer reqBB = ByteBuffer.allocate(3 + metaBuff.length);

        reqBB.put(Protocol.FILE_REQ_TYPE);
        reqBB.putShort((short) metaBuff.length);
        reqBB.put(metaBuff);

        DatagramPacket packet = new DatagramPacket(reqBB.array(), reqBB.array().length, serverIP, serverPort);
        socket.send(packet);

        byte[] respBuff = new byte[Protocol.messageSize];
        DatagramPacket response = new DatagramPacket(respBuff, respBuff.length);

        boolean ackReceived = false;
        while(!ackReceived) {
            try {
                socket.receive(response);
                
                if(response.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                    serverIP = response.getAddress();
                    serverPort = response.getPort();
                }
            } catch (SocketTimeoutException e) {
                socket.send(packet);
            }
        }
    }

    private void getFileData(String metadata) throws IOException {
        String[] dados = metadata.split(";");
        String filePath = Peer.SYNC_FOLDER + "/" + dados[0];
        File f = new File(filePath);
        FileOutputStream fos = new FileOutputStream(f);

        f.createNewFile();
        
        byte[] buffer = new byte[Protocol.messageSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        long lastSeq = receiveNSeqs() - 1;
        long seqNum = 0;

        long start = System.currentTimeMillis();
        for(long i = 0; i <= lastSeq; i++) {
            boolean received = false;
            while(!received) {
                try {
                    socket.receive(packet);

                    ByteBuffer bb = ByteBuffer.wrap(packet.getData());
                    byte type = bb.get();

                    if(type == Protocol.FILE_TYPE) {
                        sendAck(type, seqNum);
                        long msgSeq = bb.getLong();
                        if(msgSeq == seqNum) {
                            short size = bb.getShort();
                            byte[] msg = new byte[size];
                            System.arraycopy(bb.array(), 11, msg, 0, size); 
                            fos.write(msg);
                            logger.write("Packet nº " + seqNum + " of " + lastSeq + " received.", LogType.GOOD);
                            seqNum++;
                            received = true;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    logger.write("Timeout reached. Resending ACK...", LogType.TIMEOUT);
                    timeout += 1000;
                    socket.setSoTimeout(timeout);
                    sendAck(Protocol.FILE_TYPE, seqNum);
                }
            }
        }
        
        long fileSize = Files.size(f.toPath());
        double end = System.currentTimeMillis();
        double secsElapsed = (end - start) / 1000;
        double bitsPerSec = (fileSize * 8) / secsElapsed;

        logger.newLine();
        logger.write("Transfer complete! Here are some stats... :)", LogType.GOOD);
        logger.write("File size: " + fileSize + " bytes", LogType.INFO);
        logger.write("Time elapsed: " + secsElapsed + "s", LogType.INFO);
        logger.write("Transfer speed: " + bitsPerSec + " bits/s", LogType.INFO);

        FileTime modifiedTime = FileTime.fromMillis(Long.parseLong(dados[3]));
        FileTime accessTime = FileTime.fromMillis(Long.parseLong(dados[5]));
        FileTime createTime = FileTime.fromMillis(Long.parseLong(dados[4]));

        BasicFileAttributeView attr = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class);
        attr.setTimes(modifiedTime, accessTime, createTime);
        fos.close();
    }

    private void calculateRTT(long start, long end) throws SocketException {
        if(start != 0) {
            long sampleRTT = end - start;
            estimatedRTT = 0.875 * estimatedRTT + 0.125 * sampleRTT;
            devRTT = 0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT);

            timeout = (int) (estimatedRTT + 4 * devRTT);
            socket.setSoTimeout(timeout);
        }
    }

    private void sendAck(byte type, long seqNum) throws IOException {
        byte[] buffer = Protocol.createAckMessage((int) type, seqNum);
        //byte[] encrypted = e.encrypt(buffer, buffer.length);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverIP, serverPort);

        socket.send(packet);
    }

    private long receiveNSeqs() throws IOException {
        byte[] buffer = new byte[Protocol.messageSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        long nSeqs = 0;

        boolean received = false;
        while(!received) {
            try {
                socket.receive(packet);

                if(buffer[0] == Protocol.SEQ_TYPE) {
                    nSeqs = ByteBuffer.wrap(buffer).getLong(1);
                    sendAck(Protocol.SEQ_TYPE, nSeqs);
                    received = true;
                }
            } catch (SocketTimeoutException e) {
                logger.write("Timeout. Still waiting...", LogType.TIMEOUT);
            }
        }
        return nSeqs;
    }
}
