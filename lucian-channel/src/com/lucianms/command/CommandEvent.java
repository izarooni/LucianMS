package com.lucianms.command;

import com.lucianms.client.MapleCharacter;

public interface CommandEvent {

    void execute(MapleCharacter player, CommandWorker.Command cmd, CommandWorker.CommandArgs args);
}
