package com.lucianms.command;

import java.io.*;
import java.util.*;
public class CommandLog {

    BufferedWriter w = null;
    BufferedReader b = null;
    private static CommandLog instance = null;
    final List<String> chat = new ArrayList<String>();

    private CommandLog() {
        try {
            w = new BufferedWriter(new FileWriter("commandlog.txt", true));
            b = new BufferedReader(new FileReader("commandlog.txt"));
        } catch (IOException i) { i.printStackTrace(System.out); }
    }

    public static CommandLog getInstance() { //works together with disable()
        if (instance == null)
            instance = new CommandLog();
        return instance;
    }

    public List<String> getChat() {
        return chat;
    }

    public String generateTime() {
        return new Date().toString(); //deprecated class ftw
    }

    public void disable() {
        try {
            if (w != null) w.close();
            if (b != null) b.close();
            instance = null;
        } catch (IOException io) {
            io.printStackTrace(System.out);
        }
    }

    public void makeLog() {
        synchronized (w) {
            try {
                for (int i = 0; i < chat.size(); i++) {
                    w.newLine();
                    w.append(chat.get(i));
                }
            } catch (IOException io) {
                io.printStackTrace(System.out);
            }
            disable();
        }
        chat.clear();
    }

    public synchronized void add(String a) { //constantly adding
        chat.add(a);
    }

}
