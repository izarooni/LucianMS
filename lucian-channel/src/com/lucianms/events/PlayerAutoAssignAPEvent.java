package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Generic
 * @author izarooni
 */
public class PlayerAutoAssignAPEvent extends PacketEvent {

    private int primary, primaryTemp;
    private int secondary, secondaryTemp;

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        reader.skip(8);
        if (reader.available() < 16) {
            setCanceled(true);
        } else {
            primary = reader.readInt();
            primaryTemp = reader.readInt();

            secondary = reader.readInt();
            secondaryTemp = reader.readInt();

            if (primaryTemp < 0 || secondaryTemp < 0) {
                setCanceled(true);
            } else if (primaryTemp > player.getRemainingAp() || secondaryTemp > player.getRemainingAp()) {
                setCanceled(true);
            }
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter chr = getClient().getPlayer();
        if (chr.getRemainingAp() < 1) {
            return null;
        }
        int total = 0;
        int extras = 0;

        total += primaryTemp;
        extras += gainStatByType(chr, MapleStat.getBy5ByteEncoding(primary), primaryTemp);

        total += secondaryTemp;
        extras += gainStatByType(chr, MapleStat.getBy5ByteEncoding(secondary), secondaryTemp);

        int remainingAp = (chr.getRemainingAp() - total) + extras;
        chr.setRemainingAp(remainingAp);

        chr.updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }

    private int gainStatByType(MapleCharacter chr, MapleStat type, int gain) {
        int newVal = 0;
        if (type.equals(MapleStat.STR)) {
            newVal = chr.getStr() + gain;
            chr.setStr(newVal);
        } else if (type.equals(MapleStat.INT)) {
            newVal = chr.getInt() + gain;
            chr.setInt(newVal);
        } else if (type.equals(MapleStat.LUK)) {
            newVal = chr.getLuk() + gain;
            chr.setLuk(newVal);
        } else if (type.equals(MapleStat.DEX)) {
            newVal = chr.getDex() + gain;
            chr.setDex(newVal);
        }
        chr.updateSingleStat(type, newVal);
        return 0;
    }
}