package com.lucianms.io.scripting;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.meta.Achievement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

/**
 * Could be a a bad implementation of this feature
 * <p>Consider optimizations in the future</p>
 *
 * @author izarooni
 */
public class Achievements {

    private static final String ROOT_DIRECTORY = "achievements/";
    private static final Logger LOGGER = LoggerFactory.getLogger(Achievements.class);
    private static ArrayList<Pair<String, CompiledScript>> invocables = null;
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
        final long start = System.currentTimeMillis();
        File dir = new File("scripts/achievements");
        if (dir.mkdirs()) {
            LOGGER.info("Achievements script directory created");
        }
        File[] files = dir.listFiles();
        if (files != null) {
            invocables = new ArrayList<>(files.length);
            for (File file : files) {
                try {
                    CompiledScript compile = ScriptUtil.compile(ROOT_DIRECTORY + file.getName(), Collections.emptyList());
                    Invocable engine = (Invocable) compile.getEngine();
                    String name = (String) engine.invokeFunction("getName");
                    ArrayList<String> rr = new ArrayList<>();
                    engine.invokeFunction("readableRewards", rr);
                    invocables.add(new Pair<>(name, compile));
                    rewards.put(name, rr);
                } catch (Exception e) {
                    LOGGER.error("Unable to compile achievement '{}': {}", file.getName(), e.getMessage());
                }
            }
            LOGGER.info("{} achievement scripts loaded in {}s", invocables.size(), ((System.currentTimeMillis() - start) / 1000d));
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
     * Iterate through achievement scripts and test for player and kill requirements against the player and player
     * achievement data
     *
     * @param player    playerr to pass as a parameter
     * @param monsterId a monster id to pass as a parameter for kill requirements
     */
    public static void testFor(MapleCharacter player, int monsterId) {
        if (invocables != null) {
            boolean failed = false;
            for (Pair<String, CompiledScript> pair : invocables) {
                String name = pair.getLeft();
                Achievement achievement = player.getAchievement(name);
                if (achievement.getStatus() == Achievement.Status.Incomplete) {
                    try {
                        Invocable iv = (Invocable) pair.getRight().getEngine();
                        if (testForKill(iv, player, monsterId) && testForPlayer(iv, player)) {
                            achievement.setStatus(Achievement.Status.Complete);
                            player.announce(MaplePacketCreator.showEffect("quest/party/clear4"));
                            player.announce(MaplePacketCreator.mapSound("customJQ/quest"));
                            try {
                                if (reward(iv, player)) {
                                    achievement.setStatus(Achievement.Status.RewardGiven);
                                    player.sendMessage("You completed the '{}' achievement!", name);
                                } else {
                                    failed = true;
                                }
                            } catch (NoSuchMethodException e) {
                                LOGGER.warn("Achievement script {} contains no reward function", name);
                            }
                        }
                    } catch (ScriptException e) {
                        LOGGER.error("Achievement script error {}", name, e);
                    }
                }
            }
            if (failed) {
                player.sendMessage("You are unable to receive achievement rewards");
                player.sendMessage("Use the @achievements command to claim your rewards");
            }
        }
    }

    /**
     * @param invocable the achievement script invocable
     * @param player    player to pass as a parameter
     * @return true if the reward was given to the player, false otherwise
     */
    private static boolean reward(Invocable invocable, MapleCharacter player) throws ScriptException, NoSuchMethodException {
        return (boolean) invocable.invokeFunction("reward", player);
    }

    public static boolean reward(String achievement, MapleCharacter player) throws ScriptException, NoSuchMethodException {
        Optional<Pair<String, CompiledScript>> first = invocables.stream().filter(p -> p.getLeft().equals(achievement)).findFirst();
        if (first.isPresent()) {
            return reward((Invocable) first.get().getRight().getEngine(), player);
        }
        throw new NullPointerException(achievement);
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
