package com.lucianms.command.executors;

import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;

import java.util.Scanner;

/**
 * @author izarooni
 */
public abstract class ConsoleCommands {

    private static ConsoleCommands instance;

    private volatile boolean reading;
    private Scanner scanner;

    public final static ConsoleCommands getInstance() {
        return instance;
    }

    public final static void setInstance(ConsoleCommands instance) {
        ConsoleCommands.instance = instance;
    }

    public final boolean isReading() {
        return reading;
    }

    protected final void setReading(boolean reading) {
        this.reading = reading;
    }

    public final void beginReading() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setReading(true);
                scanner = new Scanner(System.in);
                String line;
                while (reading && (line = scanner.nextLine()) != null) {
                    // from CommandWorker
                    int cn = line.indexOf(" "); // command name split index
                    String name; // command name
                    String[] sp = new String[0]; // args of command
                    if (cn > -1) { // a space exists in the message (this assumes there are arguments)
                        // there are command arguments
                        name = line.substring(0, cn); // substring command name
                        if (line.length() > name.length()) { // separate command name from args
                            sp = line.substring(cn + 1, line.length()).split(" ");
                        }
                    } else {
                        // no command arguments
                        name = line;
                    }

                    Command command = new Command(name);
                    CommandArgs args = new CommandArgs(sp);

                    try {
                        execute(command, args);
                    } catch (Throwable t) {
                        // don't break the loop
                        t.printStackTrace();
                    }
                }
            }
        }, "ConsoleReader").start();
    }

    public final void stopReading() {
        if (scanner != null) {
            scanner.close();
        }
        setReading(false);
    }

    public abstract void execute(Command command, CommandArgs args);
}
