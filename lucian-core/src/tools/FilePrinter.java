package tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FilePrinter {

    public static final String ACCOUNT_STUCK = "accountStuck.txt", EXCEPTION_CAUGHT = "exceptionCaught.txt", CLIENT_START = "clientStartError.txt", ADD_PLAYER = "addPlayer.txt", MAPLE_MAP = "mapleMap.txt", ERROR38 = "error38.txt", PACKET_LOG = "log.txt", EXCEPTION = "exceptions.txt", PACKET_HANDLER = "PacketHandler/", PORTAL = "portals/", NPC = "npcs/", INVOCABLE = "invocable/", REACTOR = "reactors/", QUEST = "quests/", ITEM = "items/", MOB_MOVEMENT = "mobmovement.txt", MAP_SCRIPT = "mapscript/", DIRECTION = "directions/", SAVE_CHAR = "saveToDB.txt", INSERT_CHAR = "insertCharacter.txt", LOAD_CHAR = "loadCharFromDB.txt", UNHANDLED_EVENT = "doesNotExist.txt", SESSION = "sessions.txt", EXPLOITS = "exploits/", REPORTS = "reports/", STORAGE = "storage/", PACKET_LOGS = "packetlogs/", DELETED_CHARACTERS = "deletedchars/", FREDRICK = "fredrick/", NPC_UNCODED = "uncodedNPCs.txt", QUEST_UNCODED = "uncodedQuests.txt", SAVING_CHARACTER = "saveChar.txt";//more to come (maps)
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    private static final String FILE_PATH = "logs/" + sdf.format(Calendar.getInstance().getTime()) + "/";// + sdf.format(Calendar.getInstance().getTime()) + "/"
    private static final String ERROR = "error/";

    public static void printError(final String name, final Throwable t) {
        System.out.println("Caught throwable " + t.getMessage() + " into " + name);
        final String file = FILE_PATH + ERROR + name;
        File outputFile = new File(file);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write((t.toString() + "\r\n").getBytes());
            for (StackTraceElement traceElement : t.getStackTrace()) {
                out.write(("\tat " + traceElement + "\r\n").getBytes());
            }
            out.write("\n---------------------------------\r\n".getBytes());
        } catch (IOException ignore) {
        }
    }

    public static void printError(final String name, final Throwable t, final String info) {
        System.out.println("Caught throwable " + t.getMessage() + " into " + name);
        final String file = FILE_PATH + ERROR + name;
        File outputFile = new File(file);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write((info + "\r\n").getBytes());

            out.write((t.toString() + "\r\n").getBytes());
            for (StackTraceElement traceElement : t.getStackTrace()) {
                out.write(("\tat " + traceElement + "\r\n").getBytes());
            }

            out.write("\n---------------------------------\r\n".getBytes());
        } catch (IOException ignore) {
        }
    }

    public static void printError(final String name, final String s) {
        System.out.println("Error " + s + " into " + name);
        final String file = FILE_PATH + ERROR + name;
        File outputFile = new File(file);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write(s.getBytes());
            //            out.write("\n---------------------------------\n".getBytes());
        } catch (IOException ignore) {
        }
    }

    public static void print(final String name, final String s) {
        print(name, s, true);
    }

    public static void print(final String name, final String s, boolean line) {
        String file = FILE_PATH + name;
        File outputFile = new File(file);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write(s.getBytes());
            out.write("\r\n".getBytes());
            if (line) {
                out.write("---------------------------------\r\n".getBytes());
            }
        } catch (IOException ignore) {
        }
    }
}