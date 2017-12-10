function start(pi) {
    Packages.scripting.npc.NPCScriptManager.start(pi.getClient(), 9071000, "monster_park_enter", null);
    Packages.scripting.npc.NPCScriptManager.action(pi.getClient(), 1, 0, pi.getPortal().getId());
    return false;
}
