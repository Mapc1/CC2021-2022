package com.cc.ftrapid.logs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
    private File f;
    private FileWriter fileW;

    public Log(String fileName) throws IOException {
        this.f = new File(fileName);
        this.f.getParentFile().mkdirs();
        this.f.createNewFile();
        this.fileW = new FileWriter(f);
    }
    
    public void newLine() throws IOException {
        fileW.write("\n");
    }

    public void write(String msg, LogType type) throws IOException {
        fileW.write(type.getLabel() + msg + "\n");
        fileW.flush();
    }
}
