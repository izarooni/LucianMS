package discord.commands;

/**
 * Represents the command itself
 *
 * @author izarooni
 */
public class Command {

    private final String name; // name of command
    public final String[] args; // array of args

    Command(String name, String[] args) {
        this.name = name;
        this.args = args;
    }

    public String getCommand() {
        return name;
    }

    public boolean equals(String message) {
        return getCommand().equalsIgnoreCase(message);
    }

    /**
     * Compare the command name with other names
     *
     * @param messages command(s) to compare
     * @return true if any of the specified command name(s) match with the class defined command name
     */
    public boolean equals(String... messages) {
        for (String s : messages) {
            if (s != null && s.equalsIgnoreCase(getCommand())) {
                return true;
            }
        }
        return false;
    }

}
