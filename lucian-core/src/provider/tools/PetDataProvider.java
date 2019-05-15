package provider.tools;

import com.lucianms.client.inventory.PetCommand;
import provider.MapleData;
import provider.MapleDataTool;

import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class PetDataProvider {

    private static Map<Integer, Map<Integer, PetCommand>> CMD_CACHE = new HashMap<>();
    private static Map<Integer, Integer> HUNGER = new HashMap<>();

    private PetDataProvider() {
    }

    public static PetCommand getPetCommand(int petID, int interaction) {
        Map<Integer, PetCommand> cmds = CMD_CACHE.computeIfAbsent(petID, p -> new HashMap<>());
        PetCommand cmd = cmds.get(interaction);
        if (cmd != null) {
            return cmd;
        }
        MapleData petData = ItemProvider.getPet(petID);
        if (petData == null) {
            return null;
        }
        MapleData interact = petData.getChildByPath("interact");
        if (interact == null) {
            return null;
        }
        MapleData interactCmd = interact.getChildByPath(Integer.toString(interaction));
        if (interactCmd == null) {
            return null;
        }
        int inc = MapleDataTool.getInt("inc", interactCmd, 0);
        int levelLower = MapleDataTool.getInt("l0", interactCmd, 1);
        int levelUpper = MapleDataTool.getInt("l1", interactCmd, 30);
        int probability = MapleDataTool.getInt("prob", interactCmd, 30);
        cmd = new PetCommand(interaction, inc, levelLower, levelUpper, probability);
        cmds.put(interaction, cmd);
        return cmd;
    }

    public static int getHunger(int petID) {
        Integer hunger = HUNGER.get(petID);
        if (hunger != null) {
            return hunger;
        }
        MapleData petData = ItemProvider.getPet(petID);
        if (petData == null) {
            return 4;
        }
        MapleData infoData = petData.getChildByPath("info");
        if (infoData == null) {
            return 4;
        }
        int hungry = MapleDataTool.getInt("hungry", infoData, 4);
        HUNGER.put(petID, hungry);
        return hungry;
    }
}
