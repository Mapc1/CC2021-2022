package com.cc.ffsync.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class FilesHandler {

    /**
     * Devolve uma lista com o nome de todos os ficheiros/diretorias na diretoria
     * dada
     * 
     * @param path Caminho base a procurar
     * @param dir  Diretoria no caminho aonde queremos procurar
     * @return Lista com os metadados de todos of ficheiros/pastas
     * @throws IOException
     */
    public static List<String> readAllFilesName(String filePath) throws IOException {
        List<String> files = Files.walk(Paths.get(filePath)).map(path -> {
            String metadata = null;
            try {
                metadata = createMetaDataString(path.toFile());
                metadata = metadata.substring(filePath.length());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return metadata;
        }).collect(Collectors.toList());

        files.remove(0);
        return files;
    }

    // adicionar o nome da diretoria
    public static String createMetaDataString(File file) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        StringBuilder metaDataSB = new StringBuilder();
        metaDataSB.append(file.getPath()).append(";");
        metaDataSB.append(attr.isDirectory()).append(";");
        metaDataSB.append(attr.size()).append(";");
        metaDataSB.append(attr.lastModifiedTime().toMillis()).append(";");
        metaDataSB.append(attr.lastAccessTime().toMillis()).append(";");
        metaDataSB.append(attr.creationTime().toMillis());

        return metaDataSB.toString();
    }

    /**
     * Método que devolve o tamanho do ficheiro a que os metadados pertencem
     * 
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

        return res;

    }

    public static String readFileText(String path) throws IOException {
        Path fileName = Path.of(path);

        String text = Files.readString(fileName);

        return text;
    }
}
