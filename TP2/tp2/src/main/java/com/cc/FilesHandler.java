package com.cc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FilesHandler {

    /**
     * Devolve uma lista com o nome de todos os ficheiros/diretorias na diretoria
     * dada
     * 
     * @param path Caminho base a procurar
     * @param dir  Diretoria no caminho aonde queremos procurar
     * @return Lista com o nome dos Ficheiros/diretorias
     */
    public static List<String> readAllFilesName(String path, String dir) {
        List<String> res = new ArrayList<>();

        File directoryPath = new File(path + dir);

        // List of all files and directories
        String contents[] = directoryPath.list();
        if (contents == null)
            return res;

        for (int i = 0; i < contents.length; i++) {
            res.add(dir + contents[i]);
            File f = new File(path + dir + contents[i]);
            if (f.isDirectory()) {
                res.addAll(readAllFilesName(path, contents[i] + "/"));
            }
        }
        return res;
    }

    // adicionar o nome da diretoria
    public static String createMetaDataString(File file, String directory) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        System.out.println(file.getPath());
        StringBuilder metaDataSB = new StringBuilder();
        metaDataSB.append("name:").append(file.getName()).append(";");
        metaDataSB.append("directory:").append(directory).append(";");
        metaDataSB.append("isDirectory:").append(attr.isDirectory()).append(";");
        metaDataSB.append("size:").append(attr.size()).append(";");
        metaDataSB.append("creationTime:").append(attr.creationTime()).append(";");
        metaDataSB.append("lastModifiedTime:").append(attr.lastModifiedTime());

        return metaDataSB.toString();
    }

    /**
     * Método que devolve o tamanho do ficheiro a que os metadados pertencem
     * @param metadata Metadados do ficheiro
     * @return Tamanho do ficheiro
     */
    public static Long getSizeFromMetaData(String metadata) {
        String[] parts = metadata.split(";");
        String[] sizeParts = parts[3].split(":");
        return Long.parseLong(sizeParts[1]);
    }

    /**
     * Devolve os bytes do ficheiro pedido.
     * 
     * @param path Caminho para o ficheiro.
     * @return Lista de bytes do ficheiro.
     * @throws IOException Quando dá erro ao ler o ficheiro.
     */
    public static byte[] fromFileToByteArray(String path) throws IOException {
        File file = new File(path);
        byte[] res = Files.readAllBytes(file.toPath());

        // for (byte b : res) {
        // System.out.format("0x%02X ", b);
        // }
        System.out.println(file.length());

        return res;

    }

    // public static Boolean createFile(byte[] data, String metadata) {

    // }

}
