package com.cc;

public class ConnectionFailureException extends Exception {
    public ConnectionFailureException() {
        super();
    }

    public ConnectionFailureException(String msg) {
        super(msg);
    }
}
