package com.cc.ftrapid.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.util.List;

import com.cc.ftrapid.FTRapid;
import com.cc.ftrapid.utils.FilesHandler;

public class HttpHandler implements Runnable {
    private Socket socket;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    private String pageNotFoundMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 404 Not Found\r\n\r\n");
        sb.append("<p><span style=\"font-size: 30px;\">P&aacute;gina inexistente.</span></p>");
        sb.append("<p><br></p>");
        sb.append(
                "<p><span style=\"font-size: 12px;\">Consulte a p&aacute;gina <em><strong>/</strong></em> para saber quais as p&aacute;ginas possiveis.</span></p>");

        return sb.toString();
    }

    private String serverLogMessage() {
        StringBuilder sb = new StringBuilder();
        String log;

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            log = FilesHandler.readFileText(FTRapid.LOG_FOLDER + "/Server" + "/ServerLog.txt");

            sb.append(log);
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro de logs.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String allServerRequestsLogMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            List<String> files = FilesHandler.readAllFilesName(FTRapid.LOG_FOLDER + "/Server");
            sb.append("<p><span style=\"font-size: 18px;\">Ficheiros com log:</span></p><p></p>");

            for (String file : files) {
                if (!file.equals("ServerLog.txt")) {
                    sb.append("<p>").append(file).append("</p>");
                }
            }
            sb.append(
                    "<p><span style=\"font-size: 18px;\">Para verificar o log de um ficheiro, <em>.../srequestslogs/exfile</em></span></p>");
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro de logs.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String serverRequestsLogMessage(String file) {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            String log = FilesHandler.readFileText(FTRapid.LOG_FOLDER + "/Server/" + file);
            sb.append(log);
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro.");
            e.printStackTrace();
        } catch (InvalidPathException e) {
            sb.append("Ficheiro inexistente.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String clientAllFileTransfLogMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            List<String> files = FilesHandler.readAllFilesName(FTRapid.LOG_FOLDER + "/Client");
            sb.append("<p><span style=\"font-size: 18px;\">Ficheiros com log:</span></p><p></p>");

            for (String file : files) {
                sb.append("<p>").append(file).append("</p>");
            }
            sb.append(
                    "<p><span style=\"font-size: 18px;\">Para verificar o log de um ficheiro, <em>.../clientftlog/exfile</em></span></p>");
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro de logs.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String clientFileTransfLogMessage(String file) {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            String log = FilesHandler.readFileText(FTRapid.LOG_FOLDER + "/Client/" + file);
            sb.append(log);
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro.");
            e.printStackTrace();
        } catch (InvalidPathException e) {
            sb.append("Ficheiro inexistente.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String syncStatusMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            List<String> allFiles = FilesHandler.readAllFilesName(FTRapid.SYNC_FOLDER);
            List<String> filesToSync = FTRapid.LW.getAll();

            sb.append("<p><span style=\"font-size: 20px;\">Todos os ficheiros da pasta selecionada:</span></p>");

            for (String file : allFiles) {
                sb.append("<p>").append(file).append("</p>");
            }

            sb.append("\n<p><span style=\"font-size: 20px;\">Ficheiros por sincronizar:</span></p>");

            for (String file : filesToSync) {
                sb.append("<p>").append(file).append("</p>");
            }

        } catch (IOException e) {
            sb.append("Erro ao ler os ficheiros");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String rootMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 200 OK\r\n\r\n");
        sb.append(
                "<p style=\"text-align: left;\"><span style=\"font-size: 30px;\"><strong>FFSync</strong></span></p>");
        sb.append("<p style=\"text-align: center;\"><br></p>");
        sb.append(
                "<p style=\"text-align: left;\"><span style=\"font-size: 18px;\">Aplica&ccedil;&atilde;o de sincroniza&ccedil;&atilde;o de pastas entre clientes.</span></p>");
        sb.append("<p><span style=\"font-size: 18px;\">P&aacute;ginas dispon&iacute;veis:</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<strong><em>/syncstatus</em></strong> - <span style=\"font-size: 14px;\">Estado de sincroniza&ccedil;&atilde;o dos ficheiros</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<em><strong>/clientftlog</strong></em> - <span style=\"font-size: 14px;\">Log da transfer&ecirc;ncia dos ficheiros do cliente do FFSync</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<em><strong>/serverlog</strong></em> - <span style=\"font-size: 14px;\">Log do servidor do FFSync</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<em><strong>/srequestslogs</strong></em> - <span style=\"font-size: 14px;\">Log do pedidos que o servidor recebeu do FFSync</span></p>");

        return sb.toString();
    }

    @Override
    public void run() {

        try {
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            String[] words = line.split(" ", 3);
            String type = words[1];

            String httpResponse = "";

            if (type.equals("/")) {
                httpResponse = rootMessage();
            } else if (type.equals("/clientftlog")) {
                httpResponse = clientAllFileTransfLogMessage();
            } else if (type.startsWith("/clientftlog")) {
                String file = type.substring("/clientftlog".length());
                httpResponse = clientFileTransfLogMessage(file);
            } else if (type.equals("/serverlog")) {
                httpResponse = serverLogMessage();
            } else if (type.equals("/srequestslogs")) {
                httpResponse = allServerRequestsLogMessage();
            } else if (type.startsWith("/srequestslogs")) {
                String file = type.substring("/srequestslogs".length());
                httpResponse = serverRequestsLogMessage(file);
            } else if (type.equals("/syncstatus")) {
                httpResponse = syncStatusMessage();
            } else {
                httpResponse = pageNotFoundMessage();
            }

            socket.getOutputStream()
                    .write(httpResponse.getBytes("UTF-8"));
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
