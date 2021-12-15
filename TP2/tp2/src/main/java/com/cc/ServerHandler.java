package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;

public class ServerHandler implements Runnable { 
    private static final String DEBUG_PREFIX = "Server: ";

    DatagramSocket socket;
    Encryption e;
    int clientPort;
    InetAddress clientIP;
    double estimatedRTT = 4000;
    double devRTT = 100;

    public ServerHandler(Encryption e, int serverPort) throws SocketException {
        this.e = e;
        this.socket = new DatagramSocket(serverPort);
        socket.setSoTimeout((int) estimatedRTT);
        System.out.println(DEBUG_PREFIX + "Connection open in: " + serverPort);
    }
    
    public ServerHandler(int serverPort) throws SocketException {
        this.e = new Encryption(DEBUG_PREFIX);
        this.socket = new DatagramSocket(serverPort);
        socket.setSoTimeout((int) estimatedRTT);
        System.out.println(DEBUG_PREFIX + "Connection open in: " + serverPort);
    }

    public void run() {
        try {
            connect();
			sendMetaData("/home/marco/teste");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
    }

    private void connect() throws IOException {
        byte[] inBuffer = new byte[Protocol.messageSize];
        byte[] otherKeyArr = new byte[Protocol.messageSize];

        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        boolean otherKeyReceived = false;
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

                    clientPort = inPacket.getPort();
                    clientIP = inPacket.getAddress();

                    byte[] ack = new byte[1];
                    ack[0] = Protocol.ACK_TYPE;

                    DatagramPacket outPacket = new DatagramPacket(ack, ack.length, clientIP, clientPort);
                    socket.send(outPacket);
                }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Did not receive anything. Still waiting...");
            }
        }

        byte[] publicKey = e.calcPublicKey();
        e.calcSharedKey(otherKeyArr);

        short size = (short) publicKey.length;
        ByteBuffer pubKeyBB = ByteBuffer.allocate(3+size);
        pubKeyBB.put(Protocol.KEY_TYPE);
        pubKeyBB.putShort(size);
        pubKeyBB.put(publicKey);

        DatagramPacket outPacket = new DatagramPacket(pubKeyBB.array(), pubKeyBB.array().length, clientIP, clientPort);

        boolean ackReceived = false;
        while(!ackReceived) {
            try {
            socket.send(outPacket);
            socket.receive(inPacket);
            
            if(inPacket.getData()[0] == Protocol.ACK_TYPE) {
                ackReceived = true;
            }
            } catch (SocketTimeoutException e) {
                System.err.println(DEBUG_PREFIX + "Timeout ocurred. Sending ACK again...");
            }
        }

        System.out.println(DEBUG_PREFIX + "We're connected! YAY");
    }

    private void sendMetaData(String path) throws IOException {
        List<byte[]> pacotes = Protocol.createInfoMessage(path);
        byte[] listenBuff = new byte[Protocol.messageSize];

        long nSeqs = pacotes.size();
        sendNSeqs(nSeqs);

        long i = 0;
        while(i < pacotes.size()) {
            byte[] pacote = pacotes.get((int) i);
            //byte[] encrypted = e.encrypt(pacote, pacote.length);
            DatagramPacket packet = new DatagramPacket(pacote, pacote.length, clientIP, clientPort);
            DatagramPacket ackPacket = new DatagramPacket(listenBuff, listenBuff.length);
            
            socket.send(packet);
            System.out.println(DEBUG_PREFIX + "Packet nº " + i + " sent. Awaiting response...");

            boolean sent = false;
            while(!sent) {
                try {
                    socket.receive(ackPacket);
                    //byte[] decrypted = e.decrypt(ackPacket.getData(), ackPacket.getData().length);
                    ByteBuffer data = ByteBuffer.wrap(ackPacket.getData());

                    byte answerType = data.get();
                    long ackSeq = data.getLong();

                    if(answerType == Protocol.ACK_TYPE) {
                        System.out.println(DEBUG_PREFIX + "ACK received nº " + ackSeq);

                        sent = true;
                        if(ackSeq == i) {
                            i++;
                        } else {
                            i = ackSeq + 1;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    socket.send(packet);
                    System.err.println(DEBUG_PREFIX + "Timeout. Resending packet nº " + i);

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
                long ackSeqs = ByteBuffer.wrap(ackPacket.getData()).getLong(1);
                if(ackBuffer[0] == Protocol.ACK_TYPE && ackSeqs == nSeqs) {
                    ack = true;
                }            
            } catch (SocketTimeoutException e) {
                socket.send(packet);
            }
        }
    }
}
