package com.lucianms.events;

import com.lucianms.client.SkillMacro;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;

public class PlayerSkillMacroUpdateEvent extends PacketEvent {

    private class Macro {
        private String name;
        private byte shout; // bool
        private int skill1, skill2, skill3;
    }

    private Macro[] macros;

    @Override
    public void clean() {
        macros = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        int count = reader.readByte();
        macros = new Macro[count];
        for (int i = 0; i < count; i++) {
            Macro macro = new Macro();
            macro.name = reader.readMapleAsciiString();
            macro.shout = reader.readByte();
            macro.skill1 = reader.readInt();
            macro.skill2 = reader.readInt();
            macro.skill3 = reader.readInt();
            macros[i] = macro;
        }
    }

    @Override
    public Object onPacket() {
        for (int i = 0; i < macros.length; i++) {
            Macro m = macros[i];
            SkillMacro macro = new SkillMacro(m.skill1, m.skill2, m.skill3, m.name, m.shout, i);
            getClient().getPlayer().updateMacros(i, macro);
        }
        return null;
    }
}
