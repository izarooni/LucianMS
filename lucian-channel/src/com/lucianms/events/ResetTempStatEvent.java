package com.lucianms.events;

import com.lucianms.client.DiseaseValueHolder;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleDisease;
import com.lucianms.nio.receive.MaplePacketReader;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author izarooni
 */
public class ResetTempStatEvent extends PacketEvent {
    @Override
    public void processInput(MaplePacketReader reader) {

    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        EnumMap<MapleDisease, DiseaseValueHolder> diseases = player.getDiseases();
        if (!diseases.isEmpty()) {
            Set<MapleDisease> toRemove = new HashSet<>();
            for (Map.Entry<MapleDisease, DiseaseValueHolder> entry : diseases.entrySet()) {
                MapleDisease disease = entry.getKey();
                DiseaseValueHolder holder = entry.getValue();
                long expire = holder.startTime + holder.length;
                if (expire - System.currentTimeMillis() <= 0) {
                    toRemove.add(disease);
                }
            }
            player.cancelDebuffs(toRemove);
            toRemove.clear();
        }
        return null;
    }
}
