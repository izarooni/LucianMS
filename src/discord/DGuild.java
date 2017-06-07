package discord;

import org.json.JSONArray;
import org.json.JSONObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author izarooni
 */
public class DGuild {

    private final IGuild guild;
    private LinkedHashMap<String, LinkedList<String>> permissions = new LinkedHashMap<>();

    public DGuild(IGuild guild) {
        this.guild = guild;

        for (IRole role : guild.getRoles()) {
            permissions.put(role.getName(), new LinkedList<>());
        }
        Discord.println(String.format("Loaded DGuild('%s') - %d roles available", guild.getName(), permissions.size()));
    }

    public IGuild getGuild() {
        return guild;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        /*
        {
            'permissions': {
                'role1': ['perm1', 'perm2'],
                'role2': ['perm1']
            }
        }
        */
        JSONObject j = new JSONObject();
        for (Map.Entry<String, LinkedList<String>> entry : permissions.entrySet()) {
            JSONArray jArray = new JSONArray();
            for (String s : entry.getValue()) {
                jArray.put(s);
            }
            j.put(entry.getKey(), jArray);
        }
        json.put("permissions", j);

        return json;
    }

    public LinkedHashMap<String, LinkedList<String>> getPermissions() {
        return new LinkedHashMap<>(permissions);
    }

    public boolean hasPermission(String role, String permission) {
        if (!permissions.containsKey(role)) {
            return false;
        }
        return permissions.get(role).stream().anyMatch(s -> s.equals(permission));
    }

    public boolean givePermission(String role, String permission) {
        if (!permissions.containsKey(role)) {
            return false;
        }
        LinkedHashMap<String, LinkedList<String>> temp = new LinkedHashMap<>(permissions);
        try {
            if (temp.get(role).stream().noneMatch(s -> s.equals(permission))) {
                permissions.get(role).add(permission);
                return true;
            }
            return false;
        } finally {
            temp.clear();
        }
    }

    public boolean removePermission(String role, String permission) {
        if (!permissions.containsKey(role)) {
            return false;
        }
        LinkedHashMap<String, LinkedList<String>> temp = new LinkedHashMap<>(permissions);
        try {
            if (temp.get(role).stream().anyMatch(s -> s.equals(permission))) {
                permissions.get(role).remove(permission);
                return true;
            }
            return false;
        } finally {
            temp.clear();
        }
    }
}
