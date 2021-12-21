package com.cc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ListWrapper {
    private List<String> syncingFiles = new ArrayList<>();
    private Lock lock = new ReentrantLock();

    public void add(String fileName) {
        lock.lock();
        try {
            syncingFiles.add(fileName);
        } finally {
            lock.unlock();
        }
    }

    public void remove(String fileName) {
        lock.lock();
        try {
            syncingFiles.remove(fileName);
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(String fileName) {
        lock.lock();
        try {
            return syncingFiles.contains(fileName);
        } finally {
            lock.unlock();
        }
    }

    public List<String> getAll() {
        lock.lock();
        try {
            return new ArrayList<>(syncingFiles);
        } finally {
            lock.unlock();
        }
    }

    public String get(int i) {
        lock.lock();
        try {
            return syncingFiles.get(i);
        } finally {
            lock.unlock();
        }
    }
}
