package com.lucianms.scripting;

import client.MapleCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Could be a a bad implementation of this feature
 * <p>Consider optimizations in the future</p>
 *
 * @author izarooni
 */
public class Achievements {

    private static final Logger LOGGER = LoggerFactory.getLogger(Achievements.class);
    private static ArrayList<Pair<String, Invocable>> invocables = null;
    private static HashMap<String, ArrayList<String>> rewards = new HashMap<>();

    private Achievements() {
    }

    public static ArrayList<String> getNames() {
        ArrayList<String> ret = new ArrayList<>(invocables.size());
        invocables.forEach(p -> ret.add(p.getLeft()));
        return ret;
    }

    /**
     * List all achievement scripts, execute them and store their invocable objects for later use
     */
    public static int loadAchievements() {
        if (invocables != null) {
            invocables = new ArrayList<>(invocables.size());
            rewards = new HashMap<>(rewards.size());
            System.gc();
        }
        try {
            final long start = System.currentTimeMillis();
            File dir = new File("scripts/achievements");
            if (dir.mkdirs()) {
                LOGGER.info("Achievements script directory created");
            }
            File[] files = dir.listFiles();
            if (files != null) {
                invocables = new ArrayList<>(files.length);
                for (File file : files) {
                    Invocable iv = ScriptUtil.eval(null, "achievements/" + file.getName(), Collections.emptyList());
                    try {
                        String name = (String) iv.invokeFunction("getName");
                        ArrayList<String> rr = new ArrayList<>();
                        iv.invokeFunction("readableRewards", rr);
                        invocables.add(new Pair<>(name, iv));
                        rewards.put(name, rr);
                    } catch (Exception e) {
                        LOGGER.error("Unable to cache achievement '{}'", file.getName(), e);
                    }
                }
                LOGGER.info("{} achievement scripts loaded in {}s", invocables.size(), ((System.currentTimeMillis() - start) / 1000d));
            }
        } catch (IOException | ScriptException e) {
            e.printStackTrace();
        }
        return invocables.size();
    }

    public static ArrayList<String> getRewards(String achievement) {
        if (invocables != null) {
            return rewards.get(achievement);
        }
        return null;
    }

    /**
     * Iterate through achievement scripts and test for player and kill requirements against the player and player achievement data
     *
     * @param player    playerr to pass as a parameter
     * @param monsterId a monster id to pass as a parameter for kill requirements
     */
    public static void testFor(MapleCharacter player, int monsterId) {
        if (invocables != null) {
            for (Pair<String, Invocable> pair : invocables) {
                if (!player.getAchievement(pair.getLeft()).isCompleted()) {
                    try {
                        Invocable iv = pair.getRight();
                        if (testForKill(iv, player, monsterId) && testForPlayer(iv, player)) {
                            player.announce(MaplePacketCreator.showEffect("PSO2/stuff/2"));
                            try {
                                if (reward(iv, player)) {
                                    player.sendMessage("You completed the '{}' achievement!", pair.getLeft());
                                }
                            } catch (NoSuchMethodException e) {
                                LOGGER.warn("Achievement script {} contains no reward function", pair.getLeft());
                            }
                        }
                    } catch (ScriptException e) {
                        LOGGER.error("Achievement script error {}", pair.getLeft(), e);
                    }
                }
            }
        }
    }

    /**
     * @param invocable the achievement script invocable
     * @param player    player to pass as a parameter
     * @return true if the reward was given to the player, false otherwise
     * @throws ScriptException
     * @throws NoSuchMethodException
     */
    private static boolean reward(Invocable invocable, MapleCharacter player) throws ScriptException, NoSuchMethodException {
        return (boolean) invocable.invokeFunction("reward", player);
    }

    /**
     * Confirm if the player kill requirement for a specified achievement has reached threshold
     *
     * @param invocable the achievement script invocable
     * @param player    player to pass as a parameter
     * @param monsterId a monster id to pass a parameter
     * @return true if the test for kill requirement passes, false otherwise
     */
    private static boolean testForKill(Invocable invocable, MapleCharacter player, int monsterId) throws ScriptException {
        try {
            return (boolean) invocable.invokeFunction("testForKill", player, monsterId);
        } catch (NoSuchMethodException ignore) {
            // assume no kill requirements
            return true;
        }
    }

    /**
     * Confirm if any data accessible via player object meets specified achievement conditions
     *
     * @param invocable the achievement script invocable
     * @param player    player to pass as a parameter to the script function
     * @return true if the test for kill requirement passes, false otherwise
     */
    private static boolean testForPlayer(Invocable invocable, MapleCharacter player) throws ScriptException {
        try {
            return (boolean) invocable.invokeFunction("testForPlayer", player);
        } catch (NoSuchMethodException ignore) {
            // assume no player requirements
            return true;
        }
    }
}
