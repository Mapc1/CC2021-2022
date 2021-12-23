package com.cc.ftrapid.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    public static Integer messageSize = 1420;
    public static Integer dataByteSize = 2;

    public static final byte LS_TYPE = 1;
    public static final byte INFO_TYPE = 2;
    public static final byte FILE_REQ_TYPE = 3;
    public static final byte FILE_TYPE = 4;
    public static final byte ACK_TYPE = 5;
    public static final byte SEQ_TYPE = 6;

    // Tipo 1
    // -----------------------------------
    // | Tipo | Nº Seq | Size | Metadados|
    // -----------------------------------
    // 1B 2B 2B
    public static List<byte[]> createInfoMessage(List<String> pathFiles) throws IOException {
        Integer seqNByteSize = 2;
        Short sequenceNumber = 0;
        Integer metadataMaxSize = messageSize - 1 - seqNByteSize - dataByteSize;

        List<byte[]> res = new ArrayList<>();
        ByteBuffer bb;

        StringBuilder allMetaDataSB = new StringBuilder();
        for (String fileName : pathFiles) {
            allMetaDataSB.append(fileName).append("//");
        }

        String metaDataString = allMetaDataSB.toString();

        byte[] metaDataBytes = metaDataString.getBytes(StandardCharsets.UTF_8);
        Integer metaDataL = metaDataBytes.length;

        for (int i = 0; metaDataL - i * metadataMaxSize > metadataMaxSize; i++) {
            bb = ByteBuffer.allocate(messageSize);
            // primeiro byte que define o tipo
            bb.put(Protocol.INFO_TYPE);

            byte[] partMetaData = ByteBuffer.allocate(metadataMaxSize).array();
            System.arraycopy(metaDataBytes, 0, partMetaData, 0, metadataMaxSize);
            metaDataBytes = cutByteArray(metaDataBytes, metadataMaxSize, metaDataBytes.length - metadataMaxSize);
            // bytes para o nº da sequencia
            //byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber++).array();
            
            bb.putShort(sequenceNumber++);

            // dois bytes para o tamanho dos dados
            //byte[] metaDataSize = ByteBuffer.allocate(4).putInt(partMetaData.length).array();
            bb.putShort((short) partMetaData.length);

            // adiciona os bytes com os metadados à mensagem
            bb.put(partMetaData);

            res.add(bb.array());
        }
        bb = ByteBuffer.allocate(messageSize);

        // primeiro byte que define o tipo
        bb.put(Protocol.INFO_TYPE);

        // bytes para o nº da sequencia
        //byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
        bb.putShort(sequenceNumber);

        // dois bytes para o tamanho dos dados
        //byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
        bb.putShort((short) metaDataBytes.length);

        // adiciona os bytes com os metadados à mensagem
        bb.put(metaDataBytes);

        res.add(bb.array());

        return res;
    }

    public static byte[] cutByteArray(byte[] oldArray, Integer start, Integer size) {
        byte[] newArray = ByteBuffer.allocate(size).array();
        System.arraycopy(oldArray, start, newArray, 0, size);
        return newArray;
    }

    public static void readInfoMessage(List<byte[]> messages) throws IOException {
        Integer seqNByteSize = 2;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte[] message : messages) {
            System.out.println("Tipo Mensagem: " + (int) message[0]);
            byte[] sequenceNum = { message[1],
                    message[2] };
            ByteBuffer sequenceBB = ByteBuffer.wrap(sequenceNum);
            int seqN = (int) sequenceBB.getShort();
            System.out.println("Sequencia: " + seqN);

            byte[] sizeBytes = { message[1 + seqNByteSize],
                    message[2 + seqNByteSize] };
            ByteBuffer sizeBB = ByteBuffer.wrap(sizeBytes);
            int sizeMetaData = (int) sizeBB.getShort();
            System.out.println("Tamanho Metadados: " + sizeMetaData);

            byte[] metaDataBytes = ByteBuffer.allocate(sizeMetaData).array();
            System.arraycopy(message, 1 + seqNByteSize + dataByteSize, metaDataBytes, 0, sizeMetaData);
            outputStream.write(metaDataBytes);
        }
    }

    // Tipo 4
    // -----------------------------
    // | Tipo | Size | Metadadados |
    // -----------------------------
    // 1B 2B
    public static byte[] createAskFileTransferMessage(String metadata) {
        ByteBuffer bb = ByteBuffer.allocate(messageSize);

        byte[] metaDataBytes = metadata.getBytes(StandardCharsets.UTF_8);

        // primeiro byte que define o tipo
        bb.put(Protocol.LS_TYPE);

        // dois bytes para o tamanho dos dados
        // byte[] metaDataSize =
        // ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
        bb.putShort((short) metaDataBytes.length);

        // adiciona os bytes com os metadados à mensagem
        bb.put(metaDataBytes);

        return bb.array();
    }

    /**
     * Método que devolve os metadados do ficheiro que foi pedido para transferência
     * 
     * @param message Mensagem onde está a informação
     * @return Metadados do ficheiro pretendido
     * @throws IOException Se ocorrer um erro I/O
     */
    public static String readAskFileTransferMessage(byte[] message) {
        byte[] sizeBytes = { message[1], message[2] };
        ByteBuffer sizeBB = ByteBuffer.wrap(sizeBytes);
        int sizeMetaData = (int) sizeBB.getShort();

        byte[] metaDataBytes = ByteBuffer.allocate(sizeMetaData).array();
        System.arraycopy(message, 1 + dataByteSize, metaDataBytes, 0, sizeMetaData);

        return new String(metaDataBytes);
    }

    // Tipo 6
    // ---------------------------------
    // | Tipo | Tipo Mensagem | Nº Seq |
    // ---------------------------------
    // 1B 1B 5B
    public static byte[] createAckMessage(Integer messageType, Long seqNumber) {
        ByteBuffer bb = ByteBuffer.allocate(messageSize);
        // primeiro byte que define o tipo
        bb.put(Protocol.ACK_TYPE);
        bb.put((byte) (int) messageType);

        // bytes com o nº de sequências que esta transferência terá
        //byte[] numSequences = ByteBuffer.allocate(8).putLong(seqNumber).array();
        bb.putLong(seqNumber);

        return bb.array();
    }

    public static byte[] createAckMessage(Integer messageType) {
        ByteBuffer bb = ByteBuffer.allocate(messageSize);

        // primeiro byte que define o tipo
        bb.put(Protocol.ACK_TYPE);

        // byte que indica o tipo da mensagem a que está a responder
        bb.put((byte) (int) messageType);

        return bb.array();
    }

    /**
     * Método que devolve o tipo da mensagem
     * 
     * @param message Mensagem a analisar
     * @return Tipo da mensagem
     */
    public static Integer getMessageType(byte[] message) {
        return (int) message[0];
    }

    /**
     * Calcula a duração do próximo timeout
     * @param start Inicio do rtt em milissegundos
     * @param end   Fim do rtt em milissegundos
     * @param estimatedRTT Último rtt estimado
     * @param devRTT Último desvio do rtt
     * @return Timeout seguinte
     */
    public static int calculateRTT(long start, long end, double estimatedRTT, double devRTT) {
        int timeout = 400;
        if(start != 0) {
            long sampleRTT = end - start;
            estimatedRTT = 0.875 * estimatedRTT + 0.125 * sampleRTT;
            devRTT = 0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT);

            timeout = (int) (estimatedRTT + 4 * devRTT);
        }
        return timeout;
    }
}
