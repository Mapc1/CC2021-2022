package com.cc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    private static Integer messageSize = 1420;
    private static Integer dataByteSize = 2;

    // Tipo 1
    // -----------------------------------
    // | Tipo | Nº Seq | Size | Metadados|
    // -----------------------------------
    // 1B 2B 2B
    public static List<byte[]> createInfoMessage(String path) {
        Integer seqNByteSize = 2;
        Integer sequenceNumber = 0;
        Integer metadataMaxSize = messageSize - 1 - seqNByteSize - dataByteSize;

        List<byte[]> res = new ArrayList<>();
        ByteBuffer bb;

        List<String> pathFiles = FilesHandler.readAllFilesName(path, "");

        try {
            StringBuilder allMetaDataSB = new StringBuilder();
            for (String fileName : pathFiles) {
                File file = new File(path + fileName);
                allMetaDataSB.append(FilesHandler.createMetaDataString(file));
            }

            String metaDataString = allMetaDataSB.toString();

            byte[] metaDataBytes = metaDataString.getBytes(StandardCharsets.UTF_8);
            Integer metaDataL = metaDataBytes.length;

            for (int i = 0; metaDataL - i * metadataMaxSize > metadataMaxSize; i++) {
                bb = ByteBuffer.allocate(messageSize);
                // primeiro byte que define o tipo
                bb.put(0, (byte) 1);

                byte[] partMetaData = ByteBuffer.allocate(metadataMaxSize).array();
                System.arraycopy(metaDataBytes, 0, partMetaData, 0, metadataMaxSize);
                metaDataBytes = cutByteArray(metaDataBytes, metadataMaxSize, metaDataBytes.length - metadataMaxSize);
                // bytes para o nº da sequencia
                byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber++).array();
                bb.put(1, seqBytes, 2, 2);

                // dois bytes para o tamanho dos dados
                byte[] metaDataSize = ByteBuffer.allocate(4).putInt(partMetaData.length).array();
                bb.put(1 + seqNByteSize, metaDataSize, 2, 2);

                // adiciona os bytes com os metadados à mensagem
                bb.put(1 + seqNByteSize + dataByteSize, partMetaData);

                res.add(bb.array());
            }
            bb = ByteBuffer.allocate(messageSize);

            // primeiro byte que define o tipo
            bb.put(0, (byte) 1);

            // bytes para o nº da sequencia
            byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
            bb.put(1, seqBytes, 2, 2);

            // dois bytes para o tamanho dos dados
            byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
            bb.put(1 + seqNByteSize, metaDataSize, 2, 2);

            // adiciona os bytes com os metadados à mensagem
            bb.put(1 + seqNByteSize + dataByteSize, metaDataBytes);

            res.add(bb.array());

        } catch (IOException e) {
            System.out.println("Erro ao ler o ficheiro");
            e.printStackTrace();
        }

        // byte[] bytes = bb.array();

        // for (byte b : bytes) {
        // System.out.format("0x%02X ", b);
        // }
        // System.out.println();
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
            System.out.println("Tamanho Metadados: " + seqN);

            byte[] sizeBytes = { message[1 + seqNByteSize],
                    message[2 + seqNByteSize] };
            ByteBuffer sizeBB = ByteBuffer.wrap(sizeBytes);
            int sizeMetaData = (int) sizeBB.getShort();
            System.out.println("Tamanho Metadados: " + sizeMetaData);

            byte[] metaDataBytes = ByteBuffer.allocate(sizeMetaData).array();
            System.arraycopy(message, 1 + seqNByteSize + dataByteSize, metaDataBytes, 0, sizeMetaData);
            outputStream.write(metaDataBytes);

        }

        // String metaData = new String(outputStream.toByteArray());
        // String[] parts = metaData.split("//");

        // for (String part : parts) {
        // System.out.println("Metadados: " + part);
        // }

    }

    // Tipo 2
    // -----------------------------
    // | Tipo | Size | Metadadados |
    // -----------------------------
    // 1B 2B
    public static byte[] createAskFileTransferMessage(String metadata) {
        ByteBuffer bb = ByteBuffer.allocate(messageSize);

        byte[] metaDataBytes = metadata.getBytes(StandardCharsets.UTF_8);

        // primeiro byte que define o tipo
        bb.put(0, (byte) 2);

        // dois bytes para o tamanho dos dados
        byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
        bb.put(1, metaDataSize, 2, 2);

        // adiciona os bytes com os metadados à mensagem
        bb.put(1 + dataByteSize, metaDataBytes);

        return bb.array();
    }

    // Tipo 3
    // ------------------------------------------
    // | Tipo | Nº Sequências | Size | Metadata |
    // ------------------------------------------
    // 1B 5B 2B
    public static byte[] createTransferInfoMessage(String path) {
        Integer numSequencesBytes = 5;
        ByteBuffer bb = ByteBuffer.allocate(messageSize);

        File file = new File(path);
        String metadata;
        try {
            metadata = FilesHandler.createMetaDataString(file);
            byte[] metaDataBytes = metadata.getBytes(StandardCharsets.UTF_8);

            // primeiro byte que define o tipo
            bb.put(0, (byte) 3);

            // bytes com o nº de sequências que esta transferência terá
            Long nSeq = file.length() / (messageSize - 1 - numSequencesBytes);

            byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq).array();
            bb.put(1, numSequences, 3, numSequencesBytes);

            // dois bytes para o tamanho dos metadados
            byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
            bb.put(1 + numSequencesBytes, metaDataSize, 2, 2);

            // adiciona os bytes com os metadados à mensagem
            bb.put(1 + numSequencesBytes + dataByteSize, metaDataBytes);
        } catch (IOException e) {
            System.out.println("Erro ao ler o ficheiro");
            e.printStackTrace();
        }

        return bb.array();
    }

    // Verificar se está correto

    // Tipo 4
    // -------------------------
    // | Tipo | Nº Seq | Dados |
    // -------------------------
    // 1B 5B
    public static List<byte[]> createFileDataMessages(String path) {
        Integer numSequencesBytes = 5;
        Integer dataMaxSize = messageSize - 1 - numSequencesBytes;
        Integer nSeq = 0;

        List<byte[]> res = new ArrayList<>();
        ByteBuffer bb;

        try {
            byte[] data = FilesHandler.fromFileToByteArray(path);
            Integer dataSize = data.length;

            for (int i = 0; dataSize - i * dataMaxSize > dataMaxSize; i++) {
                byte[] partData = ByteBuffer.allocate(dataMaxSize).array();
                System.arraycopy(data, 0, partData, 0, dataMaxSize);
                data = cutByteArray(data, dataMaxSize, data.length - dataMaxSize);

                bb = ByteBuffer.allocate(messageSize);

                // primeiro byte que define o tipo
                bb.put(0, (byte) 4);

                // bytes com o nº de sequência da mensagem
                byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq++).array();
                bb.put(1, numSequences, 3, numSequencesBytes);

                // adiciona os bytes com os metadados à mensagem
                bb.put(1 + numSequencesBytes, partData);

                res.add(bb.array());
            }

            bb = ByteBuffer.allocate(messageSize);

            // primeiro byte que define o tipo
            bb.put(0, (byte) 4);

            // bytes com o nº de sequência da mensagem
            byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq).array();
            bb.put(1, numSequences, 3, numSequencesBytes);

            // adiciona os bytes com os metadados à mensagem
            bb.put(1 + numSequencesBytes, data);

            res.add(bb.array());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return res;
    }

    // Tipo 20
    // -----------------
    // | Tipo | Nº Seq |
    // -----------------
    public static byte[] createAckMessage(Integer seqNumber) {
        Integer numSequencesBytes = 5;
        ByteBuffer bb = ByteBuffer.allocate(messageSize);
        // primeiro byte que define o tipo
        bb.put(0, (byte) 20);

        // bytes com o nº de sequências que esta transferência terá
        byte[] numSequences = ByteBuffer.allocate(8).putLong(seqNumber).array();
        bb.put(1, numSequences, 3, numSequencesBytes);

        return bb.array();
    }

    public static void main(String[] args) {
        // List<byte[]> info = createInfoMessage("src/main/java/com/cc/");

        // try {
        // readInfoMessage(info);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // createTransferInfoMessage("src/main/java/com/cc/Protocol.java");

        int size = createFileDataMessages("src/main/java/com/cc/Protocol.java").size();

        System.out.println(size);
    }
}
