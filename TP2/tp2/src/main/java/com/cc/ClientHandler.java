package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private static final String DEBUG_PREFIX = "Client: ";

    InetAddress ip;
    DatagramSocket socket;
    int serverPort;
    Encryption e;
    short nSeqs;
    double estimatedRTT = 4000;
    double devRTT = 5000;

    public ClientHandler(int serverPort, InetAddress ip, Encryption e) throws UnknownHostException, SocketException {
        this.e = e;
        this.ip = ip;
        socket = new DatagramSocket();
        socket.setSoTimeout((int) devRTT);
        this.serverPort = serverPort;
        System.out.println(DEBUG_PREFIX + "Connection open in: " + serverPort + "ip: " + ip.getAddress());
    }

    public ClientHandler(int serverPort, InetAddress ip) throws UnknownHostException, SocketException {
        this.e = new Encryption(DEBUG_PREFIX);
        this.ip = ip;
        socket = new DatagramSocket();
        socket.setSoTimeout((int) devRTT);
        this.serverPort = serverPort;
        System.out.println(DEBUG_PREFIX + "Connection open in: " + serverPort + "ip: " + ip.getAddress());
    }

    public void run() {
        try {
            connect();
			//String metadata = getMetaData(200);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void connect() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(Protocol.SYN_TYPE);
        bb.putShort((short)400);

        byte[] buffer = new byte[Protocol.messageSize];

        DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, ip, serverPort);
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);

        byte[] publicKey = e.calcPublicKey();
        short size = (short) publicKey.length;

        ByteBuffer publicKeyBB = ByteBuffer.allocate(3+size);
        publicKeyBB.put(Protocol.KEY_TYPE);
        publicKeyBB.putShort(size);
        publicKeyBB.put(publicKey);

        packet = new DatagramPacket(publicKeyBB.array(), publicKeyBB.array().length, ip, serverPort);

        int tries = 0;

        boolean ackReceived = false;
        while(!ackReceived) {
            try {
                socket.send(packet);
                System.out.println(DEBUG_PREFIX + "PubKey sent, awaiting response...");

                socket.receive(packet);

                if(packet.getData()[0] == Protocol.ACK_TYPE) {
                    ackReceived = true;
                }
            } catch (SocketTimeoutException e) {
                tries++;
                if(tries == 3) {
                    System.err.println(DEBUG_PREFIX + "Did not receive ACK. Assuming ACK was lost...");
                    ackReceived = true;
                } else {
                    System.err.println(DEBUG_PREFIX + "Timeout ocurred. Trying again... Tries = " + tries);
                }
            }
        }

        System.out.println(DEBUG_PREFIX + "Ack received!");

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
                    packet = new DatagramPacket(ack, ack.length, ip, serverPort);
                    socket.send(packet);
                } else { System.err.println(DEBUG_PREFIX + "Wrong type packet. Discarding it..."); }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Did not receive a key. Still waiting...");
            }
        }
        System.out.println(DEBUG_PREFIX + "We're connected! YAY");
    }

    private String getMetaData(int nSeqs) throws IOException {
        List<byte[]> dados = new ArrayList<>();

        for(int i = 0; i < nSeqs; i++) {
            int received = 0;
            while(received == 0) {
                try {
                    byte[] buffer = new byte[Protocol.messageSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    ByteBuffer bb = ByteBuffer.wrap(packet.getData());

                    byte type = bb.get();
                    short seqNum = bb.getShort();

                    if(type == Protocol.INFO_TYPE && seqNum == i) {
                        received = 1;
                        short size = bb.getShort();
                        byte[] metadata = new byte[size];

                        System.arraycopy(packet.getData(), 5, metadata, 0, size);
                        /*for(int o = 0; o < size; o++) {
                            metadata[o] = bb.get();
                        }*/
                        dados.add(metadata);

                        byte[] ack = Protocol.createAckMessage(i);
                        DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, ip, serverPort);

                        socket.send(ackPacket);
                    }
                } catch (SocketTimeoutException e) {
                    byte[] ack = Protocol.createAckMessage(i);
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, ip, serverPort);

                    socket.send(ackPacket); 
                }
            }
        }


        int totalSize = 0;
        for(byte[] chunk: dados) {
            totalSize += chunk.length;
        }

        byte[] metaDataBytes = new byte[totalSize];
        int pos = 0;
        for(byte[] chunk: dados) {
            System.arraycopy(chunk, 0, metaDataBytes, pos, chunk.length);
            pos += chunk.length;
        }

        String msg = new String(metaDataBytes);

        return msg;
    }
}
