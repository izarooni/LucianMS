package com.lucianms.command;

import client.MapleCharacter;
import client.MapleClient;
import com.lucianms.command.executors.*;

import java.util.regex.Pattern;

/**
 * @author izarooni
 */
public class CommandWorker {

    /**
     * Coming up with new ways to handle commands...
     */
    private CommandWorker() {
    }

    public static boolean isCommand(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        char h = message.charAt(0);
        return h == '!' || h == '@';
    }

    public static boolean process(MapleClient client, String message, boolean noCheck) {
        MapleCharacter player = client.getPlayer();

        char h = message.charAt(0);
        message = message.substring(1);
        int cn = message.indexOf(" "); // command name split index
        String name; // command name
        String[] sp = new String[0]; // args of command
        if (cn > -1) { // a space exists in the message (this assumes there are arguments)
            // there are command arguments
            name = message.substring(0, cn); // substring command name
            if (message.length() > name.length()) { // separate command name from args
                sp = message.substring(cn + 1, message.length()).split(" ");
            }
        } else {
            // no command arguments
            name = message;
        }

        Command command = new Command(name);
        CommandArgs args = new CommandArgs(sp);

        if (h == '!' && (player.isGM() || noCheck)) {
            try {
                if (!EventCommands.execute(client, command, args)) {
                    if (noCheck || player.gmLevel() >= 6) {
                        AdministratorCommands.execute(client, command, args);
                    }
                    if (noCheck || player.gmLevel() >= 3) {
                        HGMCommands.execute(client, command, args);
                    }
                    if (noCheck || player.gmLevel() >= 2) {
                        GameMasterCommands.execute(client, command, args);
                    }
                    if (noCheck || player.gmLevel() >= 1) {
                        EventCommands.execute(client, command, args);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(5, "An error occurred in this command.");
            }
            return true;
        } else if (h == '@') {
             if (!player.isGM() && !noCheck) {
                 if (!command.equals("dispose", "quests")) {
                     if (player.getMapId() >= 90000000 && player.getMapId() <= 90000004) {
                         player.dropMessage("Commands are disabled in this area.");
                         return true;
                     } else if (player.getMapId() == 80 || player.getMapId() == 81) {
                         player.dropMessage("You may not use commands here");
                         return true;
                     }
                 }
            }
            PlayerCommands.execute(client, command, args);
            return true;
        }
        return false;
    }

    /**
     * Helper class that manages uses with the command name
     */
    public static class Command {

        private final String name;

        public Command(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Compare a String with the command name
         *
         * @param compare String to compare with the command name
         * @return true if the specified String matches the command name
         */
        public boolean equals(String compare) {
            return name.equalsIgnoreCase(compare);
        }

        /**
         * Compare multiple String with the command name
         *
         * @param compares An array of Strings to compare with the command name
         * @return true if any of the specified Strings match with the command name
         */
        public boolean equals(String... compares) {
            if (compares == null) {
                return false;
            }
            for (String compare : compares) {
                if (compare.equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matches(String regex) {
            return name.matches(Pattern.compile(regex).pattern());
        }
    }

    /**
     * Helper class that manages and eases functionality of command arguments
     */
    public static class CommandArgs {

        private final String[] args;
        private final String[] errors;

        public CommandArgs(String[] args) {
            this.args = args;
            errors = new String[args.length];
        }

        public String getFirstError() {
            for (String error : errors) {
                if (error != null) {
                    return error;
                }
            }
            return null;
        }

        /**
         * @param index Index of String in the args array
         * @return String stored in the args array at the specified index
         */
        public String get(int index) {
            return args[index];
        }

        /**
         * @return length of the args array
         */
        public int length() {
            return args.length;
        }

        /**
         * Compare a String stored at the specified index with multiple other Strings
         *
         * @param index    Index of String to compare
         * @param compares Array of String to compare the source String with
         * @return true if any specified String matches with the source String
         */
        public boolean argEquals(int index, String... compares) {
            String arg = args[index];
            for (String compare : compares) {
                if (compare.equalsIgnoreCase(arg)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Join args from specified index to end of array as one String
         *
         * @param index index to begin concatenation
         * @return
         */
        public String concatFrom(int index) {
            StringBuilder sb = new StringBuilder();
            for (int i = index; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            return sb.toString();
        }

        public <T extends Number> T parseNumber(int index, Class<? extends Number> t) {
            if (index < 0 || index >= args.length) {
                return null;
            }
            try {
                String arg = args[index];
                Object ret = null;
                if (t == byte.class) {
                    ret = Byte.parseByte(arg);
                } else if (t == short.class) {
                    ret = Short.parseShort(arg);
                } else if (t == int.class) {
                    ret = Integer.parseInt(arg);
                } else if (t == long.class) {
                    ret = Long.parseLong(arg);
                } else if (t == float.class) {
                    ret = Float.parseFloat(arg);
                } else if (t == double.class) {
                    ret = Double.parseDouble(arg);
                }
                return (T) ret;
            } catch (NumberFormatException e) {
                errors[index] = String.format("'%s' is not a number", args[index]);
                return null;
            } catch (IndexOutOfBoundsException e) {
                errors[index] = "Insufficient arguments";
                return null;
            }
        }

        /**
         * Parse the String argument at the specified index as the specified Number type
         * <p>
         * Should parsing fail, an error message will be created and stored in the errors array at the specified index
         * </p>
         *
         * @param index index to parse and the index the error message will be put
         * @param def   the default value to return
         * @return Long object of the parsed String, null if failure
         */
        public <T extends Number> T parseNumber(int index, T def, Class<? extends Number> t) {
            try {
                Number number = parseNumber(index, t);
                return number == null ? def : (T) number;
            } catch (Exception e) {
                return def;
            }
        }

        /**
         * Find an argument in the array of args and return the index of the argument + 1
         *
         * @param arg the argument to search for
         * @return the index above the found argument
         */
        public int findArg(String arg) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) {
                        return i + 1;
                    }
                }
            }
            return -1;
        }
    }
}
