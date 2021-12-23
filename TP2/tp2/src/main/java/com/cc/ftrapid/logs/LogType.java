package com.cc.ftrapid.logs;

public enum LogType {
    GOOD("[Good] "),
    TIMEOUT("[Timeout] "),
    ERROR("[Error] "),
    INFO("[INFO] "),
    EMPTY("");

    private final String label;

    private LogType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
