package com.cc.ftrapid.auth;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.util.Random;

import com.cc.ftrapid.logs.Log;
import com.cc.ftrapid.logs.LogType;

public class Encryption {
    private Integer privKey;
    private Integer p = 103079;
    private Integer g = 7;
    
    private BigInteger pubKey;
    private BigInteger sharedKey;

    private Log logger;

    public Encryption(Log logger) throws SocketException {
        this.logger = logger;
        Random r = new Random();
        privKey = r.nextInt(p);
    }

    public byte[] calcPublicKey() throws IOException {
        // Calculate public Key = (g^PrivKey) % p
        pubKey = new BigInteger(g.toString()).pow(privKey).remainder(new BigInteger(p.toString()));
        logger.write("Calculated public key: PrivKey: " + privKey + " | PubKey: " + pubKey, LogType.GOOD);
        return pubKey.toByteArray();
    }

    public void calcSharedKey(byte[] otherKey) throws IOException {
        // Calculates the shared key = (otherPubKey^PrivKey) % p
        sharedKey = new BigInteger(otherKey).pow(privKey).remainder(new BigInteger(p.toString()));
        logger.write("Calculated shared key: SharedKey: " + sharedKey, LogType.GOOD);
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
