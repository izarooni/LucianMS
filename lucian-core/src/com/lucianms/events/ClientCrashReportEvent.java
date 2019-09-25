package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author izarooni
 */
public class ClientCrashReportEvent extends PacketEvent {

    private String error;

    @Override
    public void processInput(MaplePacketReader reader) {
        error = reader.readMapleAsciiString();
        if (reader.available() > 0) {
            getLogger().info(reader.readAsciiString(reader.available()));
        }
    }

    @Override
    public Object onPacket() {
        Matcher m = Pattern.compile(Pattern.compile(".+?\\(.*?\\)\\)?").pattern()).matcher(error.replaceAll(", ", ""));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group()).append("\r\n");
        }
        /*
        ver(%d)
        CharacterName(%s)
        WorldID(%d)
        ChID(%d)
        FieldID(%d)
        com_error(%s)
        ZException (%s)
        source(?)
         */
        if (sb.length() > 0) {
            getLogger().info(sb.toString());
        }
        return null;
    }
}
