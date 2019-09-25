const NPCScriptManager = Java.type("scripting.npc.NPCScriptManager");

function enter(pi) {
    NPCScriptManager.start(pi.getClient(), 1300010, null, null);
    return true;
}
