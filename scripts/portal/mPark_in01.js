const NPCScriptManager = Java.type('com.lucianms.io.scripting.npc.NPCScriptManager');

function enter(pi) {
    NPCScriptManager.start(pi.getClient(), 9071006, "f_monster_park_enter");
    NPCScriptManager.action(pi.getClient(), 1, 0, pi.getPortal().getId());
    //NPCScriptManager.dispose(pi.getClient());
    return false;
}
