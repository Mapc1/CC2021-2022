package com.cc.ffsync.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

import com.cc.ffsync.FFSync;
import com.cc.ffsync.utils.FilesHandler;

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
                "<p><span style=\"font-size: 12px;\">Consulte a p&aacute;gina <em><strong>localhost:8080/</strong></em> para saber quais as p&aacute;ginas possiveis.</span></p>");

        return sb.toString();
    }

    private String serverLogMessage() {
        StringBuilder sb = new StringBuilder();
        String log;

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            log = FilesHandler.readFileText(FFSync.LOG_FOLDER + "/ServerLog.txt");

            sb.append(log);
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro de logs.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String clientLogMessage() {
        StringBuilder sb = new StringBuilder();
        String log;

        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            log = FilesHandler.readFileText(FFSync.LOG_FOLDER + "/ClientLog.txt");

            sb.append(log);
        } catch (IOException e) {
            sb.append("Erro na leitura do ficheiro de logs.");
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String syncStatusMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n\r\n");

        try {
            List<String> allFiles = FilesHandler.readAllFilesName(FFSync.SYNC_FOLDER);
            List<String> filesToSync = FFSync.LW.getAll();

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
        sb.append("<p style=\"text-align: left;\"><span style=\"font-size: 30px;\"><strong>FTRapid</strong></span></p>");
        sb.append("<p style=\"text-align: center;\"><br></p>");
        sb.append(
                "<p style=\"text-align: left;\"><span style=\"font-size: 18px;\">Aplica&ccedil;&atilde;o de sincroniza&ccedil;&atilde;o de pastas entre clientes.</span></p>");
        sb.append("<p><span style=\"font-size: 18px;\">P&aacute;ginas dispon&iacute;veis:</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<strong><em>/syncstatus</em></strong> - <span style=\"font-size: 14px;\">Estado de sincroniza&ccedil;&atilde;o dos ficheiros</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<em><strong>/clientlog</strong></em> - <span style=\"font-size: 14px;\">Log do cliente do FFSync</span></p>");
        sb.append(
                "<p>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp;<em><strong>/serverlog</strong></em> - <span style=\"font-size: 14px;\">Log do servidor do FFSync</span></p>");

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
            } else if (type.equals("/clientlog")) {
                httpResponse = clientLogMessage();
            } else if (type.equals("/serverlog")) {
                httpResponse = serverLogMessage();
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
