package com.cc;

import java.math.BigInteger;
import java.net.SocketException;
import java.util.Random;

public class Encryption {
    private String DEBUG_PREFIX;
    private Integer privKey;
    private Integer p = 103079;
    private Integer g = 7;
    
    private BigInteger pubKey;
    private BigInteger sharedKey;

    public Encryption(String DEBUG_PREFIX) throws SocketException {
        this.DEBUG_PREFIX = DEBUG_PREFIX;
        Random r = new Random();
        privKey = r.nextInt(p);
    }

    public byte[] calcPublicKey() {
        // Calculate public Key = (g^PrivKey) % p
        pubKey = new BigInteger(g.toString()).pow(privKey).remainder(new BigInteger(p.toString()));
        System.out.println(DEBUG_PREFIX + "PrivKey: " + privKey + " PubKey: " + pubKey);
        return pubKey.toByteArray();
    }

    public void calcSharedKey(byte[] otherKey) {
        // Calculates the shared key = (otherPubKey^PrivKey) % p
        sharedKey = new BigInteger(otherKey).pow(privKey).remainder(new BigInteger(p.toString()));
        System.out.println(DEBUG_PREFIX + "SharedKey: " + sharedKey);
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
