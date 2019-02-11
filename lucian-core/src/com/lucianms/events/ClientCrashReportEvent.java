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
        while (m.find()) {
            getLogger().info(m.group());
        }
        return null;
    }
}
