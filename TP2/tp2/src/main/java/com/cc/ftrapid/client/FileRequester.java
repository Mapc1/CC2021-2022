package com.cc.ftrapid.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import com.cc.ftrapid.FTRapid;
import com.cc.ftrapid.auth.Encryption;
import com.cc.ftrapid.logs.Log;
import com.cc.ftrapid.logs.LogType;
import com.cc.ftrapid.protocol.Protocol;
import com.cc.ftrapid.server.Server;

public class FileRequester implements Runnable {
    DatagramSocket socket;
    InetAddress serverIP;
    int serverPort = Server.PORT;
    Encryption e;
    double estimatedRTT = 500;
    double devRTT = 50;
    int timeout = 500;

    Log logger;

    String metadata;

    public FileRequester(String metadata, InetAddress serverIP) throws IOException {
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(timeout);

        this.serverIP = serverIP;

        this.metadata = metadata;
        String fileName = metadata.split(";")[0];
        this.logger = new Log(Client.LOG_FOLDER + fileName + ".txt");
        this.e = new Encryption(logger);
        logger.write("Connection open in: " + serverPort + " ip: " + serverIP.getAddress(), LogType.GOOD);
    }

    public void run() {
        try {
            sendGetFile();
            getFileData();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void sendGetFile() throws IOException {
        String fileName = metadata.split(";")[0];
        byte[] fNameBuff = Protocol.createGETFile(fileName); 

        logger.write("Sending file request...", LogType.GOOD);

        DatagramPacket packet = new DatagramPacket(fNameBuff, fNameBuff.length, serverIP, serverPort);
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
                    logger.write("ACK received starting file transfer...", LogType.GOOD);
                } else {
                    socket.send(packet);
                }
            } catch (SocketTimeoutException e) {
                socket.send(packet);
            }
        }
    }

    private void getFileData() throws IOException {
        String[] dados = metadata.split(";");
        String filePath = FTRapid.SYNC_FOLDER + "/" + dados[0];
        File f = new File(filePath);
        FileOutputStream fos = new FileOutputStream(f);

        FTRapid.LW.add(dados[0]);

        /* Setting all times to 0 so that in case of an error the other machine does not
         * try to fetch these broken files
        */
         FileTime modifiedTime = FileTime.fromMillis(0);
        FileTime accessTime = FileTime.fromMillis(0);
        FileTime createTime = FileTime.fromMillis(0);

        BasicFileAttributeView attr = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class);
        
        f.createNewFile();
        attr.setTimes(modifiedTime, accessTime, createTime);

        byte[] buffer = new byte[Protocol.messageSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        long nSeqs = receiveNSeqs();
        long seqNum = 0;

        long start = System.currentTimeMillis();
        long startRTT = 0;
        for(long i = 0; i < nSeqs; i++) {
            boolean received = false;
            while(!received) {
                try {
                    socket.receive(packet);
                    long endRTT = System.currentTimeMillis();
                    timeout = Protocol.calculateRTT(startRTT, endRTT, estimatedRTT, devRTT);
                    socket.setSoTimeout(timeout);

                    ByteBuffer bb = ByteBuffer.wrap(packet.getData());
                    byte type = bb.get();

                    long msgSeq = bb.getLong();
                    if(type == Protocol.FILE_TYPE && msgSeq == seqNum) {
                            sendAck(seqNum);
                            startRTT = System.currentTimeMillis();
                            short size = bb.getShort();
                            byte[] msg = new byte[size];
                            System.arraycopy(bb.array(), 11, msg, 0, size); 
                            fos.write(msg);
                            logger.write("Packet nº " + seqNum + " of " + nSeqs + " received.", LogType.GOOD);
                            seqNum++;
                            received = true;
                    } else {
                        logger.write("Wrong packet received: Nº " + msgSeq + ", Type: " + type + " Resending ACK...", LogType.ERROR);
                        if(type == Protocol.SEQ_TYPE) {
                            sendAck(nSeqs);
                        } else {
                            sendAck(seqNum - 1);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    logger.write("Timeout reached. Resending ACK nº" + (seqNum - 1) + "...", LogType.TIMEOUT);
                    timeout += 50;
                    socket.setSoTimeout(timeout);
                    sendAck(seqNum - 1);
                }
            }
        }
        
        long fileSize = Files.size(f.toPath());
        double end = System.currentTimeMillis();
        double secsElapsed = (end - start) / 1000;
        double bitsPerSec = (fileSize * 8) / secsElapsed;

        System.out.println("File " + dados[0] + " received!");

        logger.newLine();
        logger.write("Transfer complete! Here are some stats... :)", LogType.GOOD);
        logger.write("File size: " + fileSize + " bytes", LogType.INFO);
        logger.write("Time elapsed: " + secsElapsed + "s", LogType.INFO);
        logger.write("Transfer speed: " + bitsPerSec + " bits/s", LogType.INFO);

        modifiedTime = FileTime.fromMillis(Long.parseLong(dados[3]));
        accessTime = FileTime.fromMillis(Long.parseLong(dados[5]));
        createTime = FileTime.fromMillis(Long.parseLong(dados[4]));

        attr.setTimes(modifiedTime, accessTime, createTime);
        fos.close();

        FTRapid.LW.remove(dados[0]);
    }

    private void sendAck(long seqNum) throws IOException {
        byte[] buffer = Protocol.createAckMessage(seqNum);
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
                    sendAck(nSeqs);
                    received = true;
                    logger.write("Received number of sequences. They are " + nSeqs + " packets.", LogType.GOOD);
                }
            } catch (SocketTimeoutException e) {
                logger.write("Timeout. Still waiting...", LogType.TIMEOUT);
            }
        }
        return nSeqs;
    }
}
