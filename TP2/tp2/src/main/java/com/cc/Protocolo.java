package com.cc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Protocolo {
    public static final int MTU = 1500;

    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    private int seqNum = 0;
    private Encryption encrypt;
    private double estimatedRTT = 2000;
    private double devRTT = 2000;
    private long begin;

    public Protocolo(InetAddress ip, int port, int otherPort) throws SocketException {
        encrypt = new Encryption();
        socket = new DatagramSocket(port);
        this.port = otherPort;
        this.ip = ip;
    }

    public Protocolo(InetAddress ip, int otherPort) throws SocketException {
        encrypt = new Encryption();
        socket = new DatagramSocket();
        port = otherPort;
        this.ip = ip;
    }

    public InetAddress getAddress() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void connect() throws IOException, ConnectionFailureException {
         // Início da conexão
        byte[] outBuff = "SYN".getBytes(StandardCharsets.UTF_8);
        DatagramPacket outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
        socket.send(outPacket);

        byte[] inBuff = new byte[MTU];
        DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
        socket.receive(inPacket);

        String msg = new String(inPacket.getData(),0,inPacket.getLength(),StandardCharsets.UTF_8);
        if(msg.equals("SYN")) {
            outBuff = "SYN/ACK".getBytes(StandardCharsets.UTF_8);
            outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
            socket.send(outPacket);

            inBuff = new byte[MTU];
            inPacket = new DatagramPacket(inBuff, inBuff.length);
            socket.receive(inPacket);
            msg = new String(inPacket.getData(),0,inPacket.getLength(),StandardCharsets.UTF_8);
            if(!msg.equals("ACK")) {
                throw new ConnectionFailureException();
            }
            System.out.println("ACK received. Connection established...");
        } else if(msg.equals("SYN/ACK")) {
            outBuff = "ACK".getBytes(StandardCharsets.UTF_8);
            outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
            socket.send(outPacket);

            System.out.println("SYN/ACK receiver ACK sent...");
        } else {
            throw new ConnectionFailureException();
        }

        outBuff = encrypt.calcPublicKey().getBytes();
        outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
        socket.send(outPacket);

        // Obtains the other public key
        socket.receive(inPacket);
        msg = new String(inPacket.getData(),0,inPacket.getLength(),StandardCharsets.UTF_8);
        encrypt.calcSharedKey(msg);
    }

    public void sendData(byte[] data) throws IOException, ConnectionFailureException {
        List<byte[]> chunks = splitIntoChunks(data, MTU);
        int tries = 0;

        for(byte[] chunk: chunks) {
            ByteBuffer bb = ByteBuffer.allocate(chunk.length + 4);
            bb.putInt(seqNum);
            for(byte b: chunk) {
                bb.put(b);
            }
            String response = new String();
            byte[] buffer = encrypt.encrypt(bb.array(), bb.array().length);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
            while(!response.equals("ACK")) {
                try {
                    begin = System.currentTimeMillis();
                    socket.send(packet);

                    byte[] buffer2 = new byte[MTU];
                    DatagramPacket packet2 = new DatagramPacket(buffer2, buffer2.length);
                
                    socket.receive(packet2);
                    socket.setSoTimeout((int) devRTT);

                    response = new String(encrypt.decrypt(packet2.getData(), packet2.getLength()), 0, packet2.getLength(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    tries++;
                    if(tries == 3) {
                        throw new ConnectionFailureException();
                    }
                }
            }
        }
    }

    public byte[] receiveData() throws IOException {
        byte[] buffer = new byte[MTU];
        DatagramPacket packet = null;
        byte[] data = null;
        int seq = 0;

        while(seq != seqNum) {
            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            ByteBuffer bb = ByteBuffer.wrap(packet.getData());
            seq = bb.getInt();   
            data = bb.slice().array();

            if(seq == seqNum) {
                byte[] ack = "ACK".getBytes();
                byte[] ackEnc = encrypt.encrypt(ack, ack.length);
                packet = new DatagramPacket(ackEnc, ackEnc.length, ip, port);
                socket.send(packet);

                seqNum += data.length;
            }
        }
        return data;
    }

    private List<byte[]> splitIntoChunks(byte[] data, int size) {
        List<byte[]> chunks = new ArrayList<>();

        for(int i = 0; i < data.length; i+=size) {
            byte[] chunk = new byte[i + size <= data.length ? size : data.length-i];
            for(int n = 0; n < size && n + i < data.length; n++) {
                chunk[n] = data[i+n];
            }
            chunks.add(chunk);
        }

        return chunks;
    }

    private boolean checkSeqNum(byte[] data) {
        IntBuffer ib = ByteBuffer.wrap(data).asIntBuffer();
        return ib.get(0) == seqNum;
    }

    public double calculateRTT() {
        long end = System.currentTimeMillis();
        long sampleRTT = end - begin;

        estimatedRTT = 0.875 * estimatedRTT + 0.125 * sampleRTT;
        devRTT = 0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT);
        return devRTT;
    }
}
