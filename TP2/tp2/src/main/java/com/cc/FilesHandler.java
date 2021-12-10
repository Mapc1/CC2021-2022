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
    public static String createMetaDataString(File file) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        StringBuilder metaDataSB = new StringBuilder();
        metaDataSB.append("name:").append(file.getName()).append(";");
        metaDataSB.append("isDirectory:").append(attr.isDirectory()).append(";");
        metaDataSB.append("size:").append(attr.size()).append(";");
        metaDataSB.append("creationTime:").append(attr.creationTime()).append(";");
        metaDataSB.append("lastModifiedTime:").append(attr.lastModifiedTime()).append("//");

        return metaDataSB.toString();
    }

    public static byte[] fromFileToByteArray(String path) throws IOException {
        File file = new File(path);
        byte[] res = Files.readAllBytes(file.toPath());

        for (byte b : res) {
            System.out.format("0x%02X ", b);
        }
        System.out.println(res.length);

        return res;

    }

}
