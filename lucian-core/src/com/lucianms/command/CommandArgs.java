package com.lucianms.command;

/**
 * Helper class that manages and eases functionality of command arguments
 */
public class CommandArgs {

    private String[] args;
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
     *
     * @return String stored in the args array at the specified index
     */
    public String get(int index) {
        return args[index];
    }

    public void set(int index, String value) {
        args[index] = value;
    }

    /**
     * @return length of the args array
     */
    public int length() {
        return args.length;
    }

    public void setLength(int length) {
        if (length == args.length) return;
        String[] temp = new String[length];
        System.arraycopy(args, 0, temp, 0, args.length);
        args = temp;
    }

    /**
     * Compare a String stored at the specified index with multiple other Strings
     *
     * @param index    Index of String to compare
     * @param compares Array of String to compare the source String with
     *
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
     *
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
     *
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
