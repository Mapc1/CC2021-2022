package com.cc.ffsync.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cc.ffsync.FFSync;
import com.cc.ffsync.logs.Log;
import com.cc.ffsync.logs.LogType;
import com.cc.ffsync.protocol.Protocol;
import com.cc.ffsync.server.Server;
import com.cc.ffsync.utils.FilesHandler;

public class Client implements Runnable {
    public static final String LOG_FOLDER = FFSync.LOG_FOLDER + "/Client";
    private static final String LOG_FILE = "/ClientLog.txt";

    private InetAddress serverIP;
    private int serverPort = Server.PORT;
    private DatagramSocket socket;
    private Log logger;
    //private Encryption e;
    
    double estimatedRTT = 500;
    double devRTT = 50;
    private int timeout = 500;

    public Client(InetAddress serverIP) throws IOException {
        this.serverIP = serverIP;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(timeout);
        this.logger = new Log(LOG_FOLDER + LOG_FILE);
        //this.e = new Encryption(logger);
    }

    @Override
    public void run() {
        boolean on = true;
        try {
            List<Thread> threads = new ArrayList<>();
            while(on) {
                //connect();
                System.out.println("[" + serverIP + "] Getting their files...");
                sendLS();

                String theirMetaData = getMetaData();
                String ourMetaData = String.join("//", FilesHandler.readAllFilesName(FFSync.SYNC_FOLDER));
                
                System.out.println("[" + serverIP + "] Comparing folders...");
                List<String> files = cmpFolders(ourMetaData, theirMetaData);

                files = createFolders(files);

                for(String file : files) {
                    String fileName = file.split(";")[0];
                    if(!FFSync.LW.contains(fileName)) {
                        System.out.println("[" + serverIP + "] Requesting file: " + fileName);
                        Thread t = new Thread(new FileRequester(file, serverIP));
                        threads.add(t);
                        t.start();
                    }
                }
                
                for(Thread t : threads) {
                    t.join();
                }

                threads = new ArrayList<>();

                System.out.println("[" + serverIP + "] Folder synced as of " + LocalDateTime.now() + ". Going to sleep... ^^");
                logger.write("[" + serverIP + "] Folder synced as of " + LocalDateTime.now() + ". Going to sleep... ^^", LogType.GOOD);
                logger.newLine();
                TimeUnit.SECONDS.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            on = false;
            System.out.println("[Client] [" + serverIP + "] Shutting down...");
        }
        
    }

    private List<String> createFolders(List<String> metadata) throws IOException {
        int i = 0;
        while(i < metadata.size()) {
            String file = metadata.get(i);

            String[] tokens = file.split(";");
            if(tokens[1].equals("true")) {
                Path path = Paths.get(FFSync.SYNC_FOLDER + tokens[0]);
                Files.createDirectories(path);
                
                FileTime modifiedTime = FileTime.fromMillis(Long.parseLong(tokens[3]));
                FileTime accessTime = FileTime.fromMillis(Long.parseLong(tokens[5]));
                FileTime createTime = FileTime.fromMillis(Long.parseLong(tokens[4]));

                BasicFileAttributeView attr = Files.getFileAttributeView(path, BasicFileAttributeView.class);
                attr.setTimes(modifiedTime, accessTime, createTime);

                metadata.remove(i);
            } else { 
                i++;
            }
        }

        return metadata;
    }
/*
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
                logger.write("PubKey sent. Awaiting response...", LogType.GOOD);

                socket.receive(packet);

                if(packet.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                    this.clientHandlerIP = packet.getAddress();
                    this.clientHandlerPort = packet.getPort();
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
*/

    private List<String> cmpFolders(String ourMetadata, String theirMetadata) throws IOException {
        String[] ourFiles = ourMetadata.split("//");
        String[] theirFiles = theirMetadata.split("//");

        logger.write("Comparing folders...", LogType.INFO);
        logger.write("Our folder:", LogType.INFO);
        for(String file : ourFiles) {
            logger.write("      " + file, LogType.EMPTY);
        }
        logger.write("Their folder:", LogType.INFO);
        for(String file : theirFiles) {
            logger.write("      " + file, LogType.EMPTY);
        }

        List<String> ret = new ArrayList<>();
        if(theirMetadata.equals("")) {
            return ret;
        } else if(!ourMetadata.equals("")) {    
            List<String[]> ourSplitMetaData = new ArrayList<>();
            for(String file : ourFiles) {
                String[] info = file.split(";");
                ourSplitMetaData.add(info);
            }

            List<String[]> theirSplitMetaData = new ArrayList<>();
            for(String file : theirFiles) {
                String[] info = file.split(";");
                theirSplitMetaData.add(info);
            }
        
            for(String[] file : ourSplitMetaData) {
                String fileName = file[0];
                String[] theirFile = findFile(fileName, theirSplitMetaData);

                if(theirFile != null) {
                    long ourLastModifiedTime = Long.parseLong(file[3]);
                    long theirLastModifiedTime = Long.parseLong(theirFile[3]);

                    if(theirLastModifiedTime <= ourLastModifiedTime) {
                        theirSplitMetaData.remove(theirFile);
                    }
                }
            }
            for(String[] file: theirSplitMetaData) {
                ret.add(String.join(";", file));
            }
        } else {
            ret = new ArrayList<>(Arrays.asList(theirFiles));
        }

        return ret;
    }

    private String[] findFile(String fileName, List<String[]> theirSplitMetaData) {
        String[] ret = null;

        for(int i = 0; i < theirSplitMetaData.size() && ret == null; i++) {
            if(fileName.equals(theirSplitMetaData.get(i)[0])) {
                ret = theirSplitMetaData.get(i);
            }
        }

        return ret;
    }

    private void sendLS() throws IOException {
        logger.write("Sending LS request...", LogType.GOOD);
        
        byte[] buffer = {Protocol.LS_TYPE};
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverIP, Server.PORT);
        socket.send(packet);

        byte[] respBuff = new byte[Protocol.messageSize];
        DatagramPacket response = new DatagramPacket(respBuff, respBuff.length);

        boolean ackReceived = false;
        while (!ackReceived) {
            try {
                socket.receive(response);
                if(response.getData()[0] == Protocol.ACK_TYPE) {
                    logger.write("ACK received. Cool...", LogType.GOOD);
                    ackReceived = true;
                    serverPort = response.getPort();
                }
            } catch (SocketTimeoutException s) {
                logger.write("Timeout... :/", LogType.TIMEOUT);
                socket.send(packet);
            }
        }
    }

    private String getMetaData() throws IOException {
        List<byte[]> dados = new ArrayList<>();

        byte[] recvBuff = new byte[Protocol.messageSize];
        DatagramPacket recvPacket = new DatagramPacket(recvBuff, recvBuff.length);

        long nSeqs = receiveNSeqs();
        short seqNum = 0;

        long startRTT = 0;
        for(int i = 0; i < nSeqs; i++) {
            boolean received = false;
            while(!received) {
                try {
                    socket.receive(recvPacket);
                    long endRTT = System.currentTimeMillis();
                    //byte[] decrypted = e.decrypt(recvPacket.getData(), recvPacket.getData().length);
                    timeout = Protocol.calculateRTT(startRTT, endRTT, estimatedRTT, devRTT);
                    socket.setSoTimeout(timeout);

                    ByteBuffer bb = ByteBuffer.wrap(recvPacket.getData());
                    byte type = bb.get();

                    short msgSeq = bb.getShort();
                    if(type == Protocol.INFO_TYPE && msgSeq == seqNum) {
                        sendAck(Protocol.INFO_TYPE, seqNum);
                        short size = bb.getShort(); 
                        byte[] msg = new byte[size];
                        System.arraycopy(bb.array(), 5, msg, 0, size);
                        dados.add(msg);
                        logger.write("Packet nº " + seqNum + " received", LogType.GOOD);
                        seqNum++;
                        received = true;
                    } else {
                        logger.write("Wrong packet received. Nº: " + msgSeq + " s " + seqNum  + ", Type: " + type, LogType.ERROR);
                        if(type == Protocol.SEQ_TYPE) {
                            sendAck(Protocol.SEQ_TYPE, nSeqs);
                        } else {
                            sendAck(Protocol.INFO_TYPE, seqNum - 1);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    logger.write("Timeout WTF", LogType.TIMEOUT);
                    sendAck(Protocol.INFO_TYPE, seqNum - 1);
                }
            }
        }

        int totalSize = 0;
        for(byte[] chunk : dados) {
            totalSize += chunk.length;
        }

        byte[] metadata = new byte[totalSize];
        int pos = 0;
        for(byte[] chunk : dados) {
            System.arraycopy(chunk, 0, metadata, pos, chunk.length);
            pos += chunk.length;
        }

        logger.write("Metadata received!", LogType.GOOD);

        String s = new String(metadata);
        return s;
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
                    logger.write("Received number of sequences. They are " + nSeqs + " packets.", LogType.GOOD);
                }
            } catch (SocketTimeoutException e) {
                logger.write("Timeout. Still waiting", LogType.TIMEOUT);
            }
        }
        return nSeqs;
    }

    private void sendAck(byte type, long nSeqs) throws IOException {
        byte[] buffer = Protocol.createAckMessage((int) type, nSeqs);
        //byte[] encrypted = e.encrypt(buffer, buffer.length);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverIP, serverPort);

        socket.send(packet);
    }
}
