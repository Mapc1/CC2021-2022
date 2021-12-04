package com.cc;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Encryption {
    Integer privKey;
    Integer p = 103079;
    Integer g = 7;
    
    BigInteger pubKey;
    BigInteger sharedKey;

    DatagramSocket listener;
    DatagramSocket sender;

    InetAddress ip;
    int port;

    public Encryption(InetAddress ip, DatagramSocket listenSocket, int sendPort) throws SocketException {
        listener = listenSocket;
        sender = new DatagramSocket();
        this.ip = ip;
        port = sendPort;

        Random r = new Random();
        privKey = r.nextInt(p);
    }

    public void auth() throws IOException {
        // Calculate public Key = (g^PrivKey) % p
        pubKey = new BigInteger(g.toString()).pow(privKey).remainder(new BigInteger(p.toString()));

        System.out.println("PrivKey: " + privKey + " PubKey: " + pubKey);

        // Início da conexão
        byte[] outBuff = "SYN".getBytes(StandardCharsets.UTF_8);
        DatagramPacket outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
        sender.send(outPacket);

        byte[] inBuff = new byte[512];
        DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
        listener.receive(inPacket);

        String msg = new String(inPacket.getData(),0,inPacket.getLength(),StandardCharsets.UTF_8);
        if(msg.equals("SYN")) {
            outBuff = "SYN/ACK".getBytes(StandardCharsets.UTF_8);
            outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
            sender.send(outPacket);
        }
        System.out.println("ACK received. Connection established...");

        // Sends this machine's public key
        outBuff = pubKey.toString().getBytes(StandardCharsets.UTF_8);
        outPacket = new DatagramPacket(outBuff, outBuff.length, ip, port);
        sender.send(outPacket);

        // Obtains the other public key
        listener.receive(inPacket);
        msg = new String(inPacket.getData(),0,inPacket.getLength(),StandardCharsets.UTF_8);

        // Calculates the shared key = (otherPubKey^PrivKey) % p
        sharedKey = new BigInteger(msg).pow(privKey).remainder(new BigInteger(p.toString()));

        System.out.println("Other: " + msg + " SharedKey: " + sharedKey);
    }

    public byte[] encrypt(byte[] array, int length) {
        byte[] cypher = new byte[length];
        
        for(int i = 0; i < length; i++) {
            // Encrypts the byte with (byte+sharedKey) % p
            BigInteger encrypted = new BigInteger(""+array[i]).add(sharedKey).remainder(new BigInteger(p+""));
            cypher[i] = encrypted.byteValue();
        }

        return cypher;
    } 

    public byte[] decrypt(byte[] cypher, int length) {
        byte[] input = new byte[length];

        for(int i = 0; i < length; i+=1) {
            // Decrypts the byte with (byte-sharedKey) % p
            BigInteger decrypted = new BigInteger(cypher[i]+"").subtract(sharedKey).remainder(new BigInteger(p.toString()));
            input[i] = decrypted.byteValue();
        }

        return input;
    }
}
