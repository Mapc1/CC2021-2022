package com.cc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    public static Integer messageSize = 1420;
    public static Integer dataByteSize = 2;
    
    public static final byte INFO_TYPE = 1;
    public static final byte ACK_TYPE = 20;
    public static final byte SYN_TYPE = 21;
    public static final byte KEY_TYPE = 22;

    // Tipo 1
    // -----------------------------------
    // | Tipo | Nº Seq | Size | Metadados|
    // -----------------------------------
    // 1B 2B 2B
    public static List<byte[]> createInfoMessage(String path) {
        Integer seqNByteSize = 2;
        Short sequenceNumber = 0;
        Integer metadataMaxSize = messageSize - 1 - seqNByteSize - dataByteSize;

        List<byte[]> res = new ArrayList<>();
        ByteBuffer bb;

        List<String> pathFiles = FilesHandler.readAllFilesName(path, "/");

        try {
            StringBuilder allMetaDataSB = new StringBuilder();
            for (String fileName : pathFiles) {
                File file = new File(path + fileName);
                StringBuilder directorySB = new StringBuilder();
                String[] parts = fileName.split("//");

                for (int i = 0; i < parts.length - 1; i++) {
                    directorySB.append(parts[i]).append("/");
                }

                allMetaDataSB.append(FilesHandler.createMetaDataString(file, directorySB.toString())).append("//");
            }

            String metaDataString = allMetaDataSB.toString();

            byte[] metaDataBytes = metaDataString.getBytes(StandardCharsets.UTF_8);
            Integer metaDataL = metaDataBytes.length;

            for (int i = 0; metaDataL - i * metadataMaxSize > metadataMaxSize; i++) {
                bb = ByteBuffer.allocate(messageSize);
                // primeiro byte que define o tipo
                bb.put((byte) 1);

                byte[] partMetaData = ByteBuffer.allocate(metadataMaxSize).array();
                System.arraycopy(metaDataBytes, 0, partMetaData, 0, metadataMaxSize);
                metaDataBytes = cutByteArray(metaDataBytes, metadataMaxSize, metaDataBytes.length - metadataMaxSize);
                // bytes para o nº da sequencia
                //byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber++).array();
                
                bb.putShort(sequenceNumber);

                // dois bytes para o tamanho dos dados
                //byte[] metaDataSize = ByteBuffer.allocate(4).putInt(partMetaData.length).array();
                bb.putShort((short) partMetaData.length);

                // adiciona os bytes com os metadados à mensagem
                bb.put(partMetaData);

                res.add(bb.array());
            }
            bb = ByteBuffer.allocate(messageSize);

            // primeiro byte que define o tipo
            bb.put((byte) 1);

            // bytes para o nº da sequencia
            //byte[] seqBytes = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
            bb.putShort(sequenceNumber);

            // dois bytes para o tamanho dos dados
            //byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
            bb.putShort((short) metaDataBytes.length);

            // adiciona os bytes com os metadados à mensagem
            bb.put(metaDataBytes);

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
        bb.put((byte) 2);

        // dois bytes para o tamanho dos dados
        //byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
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

    /**
     * Cria uma mensagem que envia a informação do ficheiro que irá enviar em
     * seguida.
     * <p>
     * Protocolo da mensagem (tipo 3):
     * <p>
     * | Tipo (1B) | Nº Sequências (5B) | Size (2B) | Metadata |
     * 
     * @param metadata Metadados do ficheiro que irá enviar
     * @return Mensagem com a informação
     */
    public static byte[] createTransferInfoMessage(String metadata) {
        Integer numSequencesBytes = 5;
        ByteBuffer bb = ByteBuffer.allocate(messageSize);

        byte[] metaDataBytes = metadata.getBytes(StandardCharsets.UTF_8);

        // primeiro byte que define o tipo
        bb.put(0, (byte) 3);

        // bytes com o nº de sequências que esta transferência terá
        Long size = FilesHandler.getSizeFromMetaData(metadata);

        Long nSeq = 1 + size / (messageSize - 1 - numSequencesBytes);

        byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq).array();
        bb.put(1, numSequences, 3, numSequencesBytes);

        // dois bytes para o tamanho dos metadados
        byte[] metaDataSize = ByteBuffer.allocate(4).putInt(metaDataBytes.length).array();
        bb.put(1 + numSequencesBytes, metaDataSize, 2, 2);

        // adiciona os bytes com os metadados à mensagem
        bb.put(1 + numSequencesBytes + dataByteSize, metaDataBytes);

        return bb.array();
    }

    /**
     * Método que lê uma mensagem com a informação da transferência de ficheiro que
     * será feito.
     * 
     * @param message Array de bytes da mensagem
     * @return Par com o número de pacotes que serão enviados e a metadata do
     *         ficheiro
     */
    public static Pair<Long, String> readTransferInfoMessage(byte[] message) {
        Integer numSequencesBytes = 5;

        byte[] sequenceBytes = ByteBuffer.allocate(8).array();
        System.arraycopy(message, 1, sequenceBytes, 8 - numSequencesBytes, numSequencesBytes);
        ByteBuffer sequenceBB = ByteBuffer.wrap(sequenceBytes);
        Long nSeq = sequenceBB.getLong();

        byte[] sizeBytes = { message[1 + numSequencesBytes], message[2 + numSequencesBytes] };
        ByteBuffer sizeBB = ByteBuffer.wrap(sizeBytes);
        int sizeMetaData = (int) sizeBB.getShort();

        byte[] metaDataBytes = ByteBuffer.allocate(sizeMetaData).array();
        System.arraycopy(message, 1 + numSequencesBytes + dataByteSize, metaDataBytes, 0, sizeMetaData);

        return new Pair<Long, String>(nSeq, new String(metaDataBytes));
    }

    /**
     * Cria uma lista de mensagem a enviar com toda a informação do ficheiro
     * pretendido.
     * <p>
     * Protocolo da mensagem (tipo 4):
     * <p>
     * 
     * | Tipo (1B) | Nº Seq (5B) | Dados |
     * <p>
     * 
     * @param path Caminho para o ficheiro
     * @return Lista com as mensagens com a informação
     */
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
                bb.put((byte) 4);

                // bytes com o nº de sequência da mensagem
                //byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq++).array();
                bb.putLong(nSeq);

                // adiciona os bytes com os metadados à mensagem
                bb.put(partData);

                res.add(bb.array());
            }

            bb = ByteBuffer.allocate(messageSize);

            // primeiro byte que define o tipo
            bb.put((byte) 4);

            // bytes com o nº de sequência da mensagem
            //byte[] numSequences = ByteBuffer.allocate(8).putLong(nSeq).array();
            bb.putLong(nSeq);

            // adiciona os bytes com os metadados à mensagem
            bb.put(data);

            res.add(bb.array());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return res;
    }

    /**
     * Devolve o array de bytes do ficheiro recebido a partir da lista de todas as
     * suas mensagens
     * 
     * @param messages   Lista com as mensagens recebidas
     * @param nSequences Número de mensagens recebidas
     * @param fileSize   Tamanho do ficheiro recebido
     * @return Array de bytes do ficheiro
     */
    public static byte[] readFileBytesFromMessages(List<byte[]> messages, Long nSequences, Long fileSize) {
        Integer numSequencesBytes = 5;
        Integer maxDataSize = messageSize - 1 - numSequencesBytes;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte[] message : messages) {
            byte[] dataBytes;

            byte[] sequenceBytes = ByteBuffer.allocate(8).array();
            System.arraycopy(message, 1, sequenceBytes, 8 - numSequencesBytes, numSequencesBytes);
            ByteBuffer sequenceBB = ByteBuffer.wrap(sequenceBytes);
            Long nSeq = sequenceBB.getLong();

            if (nSeq == nSequences - 1) {
                Integer lastDataNumBytes = Math.toIntExact(fileSize - maxDataSize * nSequences - 1);

                dataBytes = ByteBuffer.allocate(lastDataNumBytes).array();
                System.arraycopy(message, 1 + numSequencesBytes, dataBytes, 0, lastDataNumBytes);
            } else {
                dataBytes = ByteBuffer.allocate(maxDataSize).array();
                System.arraycopy(message, 1 + numSequencesBytes, dataBytes, 0, maxDataSize);
            }
            try {
                outputStream.write(dataBytes);
            } catch (IOException e) {
                System.out.println("I/O error");
                e.printStackTrace();
            }
        }

        return outputStream.toByteArray();
    }

    // Tipo 20
    // ---------------------------------
    // | Tipo | Tipo Mensagem | Nº Seq |
    // ---------------------------------
    // 1B  1B  5B
    public static byte[] createAckMessage(Integer messageType ,Long seqNumber) {
        Integer numSequencesBytes = 5;
        ByteBuffer bb = ByteBuffer.allocate(messageSize);
        // primeiro byte que define o tipo
        bb.put(0, (byte) 20);
        bb.put(1, (byte) (int) messageType);

        // bytes com o nº de sequências que esta transferência terá
        //byte[] numSequences = ByteBuffer.allocate(8).putLong(seqNumber).array();
        bb.putLong(1, seqNumber);

        return bb.array();
    }

    /**
     * Devolve um par com o tipo a que a messagem dá ack, e o número de sequência (se necessário).
     * @param message Array de bytes com a mensagem
     * @return Par com o tipo da mensagem e o número de sequência
     */
    public static Pair<Integer, Long> readAckMessage(byte[] message) {
        Integer numSequencesBytes = 5;

        byte[] sequenceBytes = ByteBuffer.allocate(8).array();
            System.arraycopy(message, 2, sequenceBytes, 8 - numSequencesBytes, numSequencesBytes);
            ByteBuffer sequenceBB = ByteBuffer.wrap(sequenceBytes);
            Long nSeq = sequenceBB.getLong();
        
        return new Pair<Integer,Long>((int) message[1], nSeq);
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

    public static void main(String[] args) {
        // List<byte[]> info = createInfoMessage("src/main/java/com/cc/");

        // try {
        // readInfoMessage(info);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // createTransferInfoMessage("src/main/java/com/cc/Protocol.java");
        try {
            String meta = FilesHandler.createMetaDataString(new File("src/main/java/com/cc/Protocol.java"), "");
            System.out.println(meta);

            FilesHandler.getSizeFromMetaData(meta);

            byte[] message = createTransferInfoMessage(meta);

            System.out.println(getMessageType(message));

            Pair<Long, String> m = readTransferInfoMessage(message);
            System.out.println(m.fst());
            System.out.println(m.snd());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
