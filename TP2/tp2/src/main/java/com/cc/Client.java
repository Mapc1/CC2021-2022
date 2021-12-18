package com.cc;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client implements Runnable {
    private String syncFolder;
    private int serverPort;
    private InetAddress serverIP;
    private int clientHandlerPort;
    private InetAddress clientHandlerIP;
    private DatagramSocket socket;
    private Encryption e = new Encryption(DEBUG_PREFIX);

    private static final String DEBUG_PREFIX = "RealClient: ";

    public Client(String syncFolder, int serverPort, InetAddress serverIP) throws SocketException {
        this.syncFolder = syncFolder;
        this.serverPort = serverPort;
        this.serverIP = serverIP;
        socket = new DatagramSocket();
    }

    @Override
    public void run() {
        try {
            List<Thread> threads = new ArrayList<>();
            //while(true) {
                //connect();
                sendLS();
                String theirMetaData = getMetaData();

                String ourMetaData = String.join("//", FilesHandler.readAllFilesName(syncFolder));
                
                List<String> files;
                if(!ourMetaData.equals("")) {
                    files = cmpFolders(ourMetaData, theirMetaData);
                } else {
                    files = new ArrayList<String>(Arrays.asList(theirMetaData.split("//")));
                }

                files = createFolders(files);

                for(String file : files) {
                    Thread t = new Thread(new ClientHandler(file, syncFolder, serverIP, serverPort));
                    threads.add(t);
                    t.start();
                }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private List<String> createFolders(List<String> metadata) throws IOException {
        for(int i = 0; i < metadata.size(); i++) {
            String file = metadata.get(i);

            String[] tokens = file.split(";");
            if(tokens[1].equals("true")) {
                Path path = Paths.get(syncFolder + tokens[0]);
                Files.createDirectories(path);
                
                FileTime modifiedTime = FileTime.fromMillis(Long.parseLong(tokens[3]));
                FileTime accessTime = FileTime.fromMillis(Long.parseLong(tokens[5]));
                FileTime createTime = FileTime.fromMillis(Long.parseLong(tokens[4]));

                BasicFileAttributeView attr = Files.getFileAttributeView(path, BasicFileAttributeView.class);
                attr.setTimes(modifiedTime, accessTime, createTime);

                metadata.remove(i);
            }
        }

        return metadata;
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
                System.out.println(DEBUG_PREFIX + "PubKey sent, awaiting response...");

                socket.receive(packet);

                if(packet.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                    this.clientHandlerIP = packet.getAddress();
                    this.clientHandlerPort = packet.getPort();
                }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Timeout ocurred. Trying again...");
            }
        }

        System.out.println(DEBUG_PREFIX + "Ack received!");
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);

        boolean otherKeyReceived = false;
        while(!otherKeyReceived) {
            try {
                socket.receive(response);      
                System.out.println(DEBUG_PREFIX + "Packet received. Checking contents...");

                ByteBuffer rBB = ByteBuffer.wrap(response.getData());
                byte type = rBB.get();

                if(type == Protocol.KEY_TYPE) {
                    System.out.println(DEBUG_PREFIX + "Packet type correct. Reading key...");
                    short rSize = rBB.getShort();
                    byte[] otherKey = new byte[rSize];
                    System.arraycopy(rBB.array(), 3, otherKey, 0, rSize);

                    e.calcSharedKey(otherKey);
                    otherKeyReceived = true;

                    byte[] ack = {Protocol.ACK_TYPE};
                    packet = new DatagramPacket(ack, ack.length, serverIP, serverPort);
                    socket.send(packet);
                } else { System.err.println(DEBUG_PREFIX + "Wrong type packet. Discarding it..."); }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Did not receive a key. Still waiting...");
            }
        }
        System.out.println(DEBUG_PREFIX + "We're connected! YAY");
    }


    private List<String> cmpFolders(String ourMetadata, String theirMetadata) {
        String[] ourFiles = ourMetadata.split("//");
        String[] theirFiles = theirMetadata.split("//");

        List<String> ret = new ArrayList<>();

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

            /*boolean found = false;
            String theirFile = null;
            for(int i = 0; i < theirSplitMetaData.size() && !found; i++) {
                if(fileName.equals(theirSplitMetaData.get(i)[0])) {
                    theirFile = theirFiles[i];
                    found = true;
                }
            }
*/
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
        byte[] buffer = {Protocol.LS_TYPE};
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverIP, serverPort);
        socket.send(packet);

        byte[] respBuff = new byte[Protocol.messageSize];
        DatagramPacket response = new DatagramPacket(respBuff, respBuff.length);

        boolean ackReceived = false;
        while (!ackReceived) {
            try {
                socket.receive(response);
                if(response.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                    clientHandlerIP = response.getAddress();
                    clientHandlerPort = response.getPort();
                }
            } catch (SocketTimeoutException s) {
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

        long start = 0;
        for(int i = 0; i < nSeqs; i++) {
            boolean received = false;
            while(!received) {
                try {
                    socket.receive(recvPacket);
                    //byte[] decrypted = e.decrypt(recvPacket.getData(), recvPacket.getData().length);
                    long end = System.currentTimeMillis();
                    //calculateRTT(start, end);

                    ByteBuffer bb = ByteBuffer.wrap(recvPacket.getData());
                    byte type = bb.get();

                    if(type == Protocol.INFO_TYPE) {
                        sendAck(Protocol.INFO_TYPE, seqNum);
                        start = System.currentTimeMillis();
                        short msgSeq = bb.getShort();
                        if(msgSeq == seqNum) {
                            short size = bb.getShort(); 
                            byte[] msg = new byte[size];
                            System.arraycopy(bb.array(), 5, msg, 0, size);
                            dados.add(msg);
                            System.out.println(DEBUG_PREFIX + "Packet nº " + seqNum + " received");
                            seqNum++;
                            received = true;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println(DEBUG_PREFIX + "Timeout wtf");
                    sendAck(Protocol.INFO_TYPE, seqNum);
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

        String s = new String(metadata);
        System.out.println(s);
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
                }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Timeout. Still waiting");
            }
        }
        return nSeqs;
    }

    private void sendAck(byte type, long nSeqs) throws IOException {
        byte[] buffer = Protocol.createAckMessage((int) type, nSeqs);
        //byte[] encrypted = e.encrypt(buffer, buffer.length);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientHandlerIP, clientHandlerPort);

        socket.send(packet);
    }
}