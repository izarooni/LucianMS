package com.lucianms.command;

import java.util.regex.Pattern;

/**
 * Helper class that manages uses with the command name
 */
public class Command {

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
     *
     * @return true if the specified String matches the command name
     */
    public boolean equals(String compare) {
        return name.equalsIgnoreCase(compare);
    }

    /**
     * Compare multiple String with the command name
     *
     * @param compares An array of Strings to compare with the command name
     *
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
