package discord.user;

import org.json.JSONArray;
import org.json.JSONObject;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author izarooni
 */
public class DUser {

    private final IUser user;
    private LinkedList<String> permissions = new LinkedList<>();

    public DUser(IUser user) {
        this.user = user;
    }

    public IUser getUser() {
        return user;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray jArray = new JSONArray();
        permissions.forEach(jArray::put);
        json.put("permissions", jArray);

        return json;
    }

    public ArrayList<String> getPermissions() {
        return new ArrayList<>(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.stream().anyMatch(s -> s.equals(permission));
    }

    public boolean givePermission(String permission) {
        ArrayList<String> temp = new ArrayList<>(permissions);
        try {
            if (temp.stream().noneMatch(s -> s.equals(permission))) {
                permissions.add(permission);
                return true;
            }
            return false;
        } finally {
            temp.clear();
        }
    }

    public boolean removePermission(String permission) {
        ArrayList<String> temp = new ArrayList<>(permissions);
        try {
            if (temp.stream().anyMatch(s -> s.equals(permission))) {
                permissions.remove(permission);
                return true;
            }
            return false;
        } finally {
            temp.clear();
        }
    }
}
