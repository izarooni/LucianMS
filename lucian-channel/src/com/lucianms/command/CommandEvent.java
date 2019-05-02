package com.lucianms.command;

import com.lucianms.client.MapleCharacter;

public interface CommandEvent {

    void execute(MapleCharacter player, Command cmd, CommandArgs args);
}
