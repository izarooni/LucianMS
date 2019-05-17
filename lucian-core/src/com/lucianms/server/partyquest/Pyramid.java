/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lucianms.server.partyquest;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.world.MapleParty;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 * @author kevintjuh93
 */
public class Pyramid extends PartyQuest {

    public enum PyramidMode {
        EASY(0), NORMAL(1), HARD(2), HELL(3);
        int mode;

        PyramidMode(int mode) {
            this.mode = mode;
        }

        public int getMode() {
            return mode;
        }
    }

    private int kill = 0, miss = 0, cool = 0, exp = 0, map, count;
    private byte coolAdd = 5, missSub = 4, decrease = 1;//hmmm
    private short gauge;
    private byte rank, skill = 0, stage = 0, buffcount = 0;//buffcount includes buffs + skills
    private PyramidMode mode;

    private Task timer = null;
    private Task gaugeTask = null;

    public Pyramid(MapleParty party, PyramidMode mode, int mapid) {
        super(party);
        this.mode = mode;
        this.map = mapid;

        byte plus = (byte) mode.getMode();
        coolAdd += plus;
        missSub += plus;
        switch (plus) {
            case 0:
                decrease = 1;
            case 1:
            case 2:
                decrease = 2;
            case 3:
                decrease = 3;
        }
    }

    private void startGaugeSchedule() {
        if (gaugeTask == null) {
            gauge = 100;
            count = 0;
            gaugeTask = TaskExecutor.createRepeatingTask(new Runnable() {
                @Override
                public void run() {
                    gauge -= decrease;
                    if (gauge <= 0) {
                        warp(926010001);
                    }

                }
            }, 1000);
        }
    }

    public void kill() {
        kill++;
        if (gauge < 100) {
            count++;
        }
        gauge++;
        broadcastInfo("hit", kill);
        if (gauge >= 100) {
            gauge = 100;
        }
        checkBuffs();
    }

    public void cool() {
        cool++;
        int plus = coolAdd;
        if ((gauge + coolAdd) > 100) {
            plus -= ((gauge + coolAdd) - 100);
        }
        gauge += plus;
        count += plus;
        if (gauge >= 100) {
            gauge = 100;
        }
        broadcastInfo("cool", cool);
        checkBuffs();

    }

    public void miss() {
        miss++;
        count -= missSub;
        gauge -= missSub;
        broadcastInfo("miss", miss);
    }

    public int timer() {
        int value;
        if (stage > 0) {
            value = 180;
        } else {
            value = 120;
        }

        timer = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                stage++;
                warp(map + (stage * 100));//Should work :D
            }
        }, value * 1000);//, 4000
        broadcastInfo("party", getParticipants().size() > 1 ? 1 : 0);
        broadcastInfo("hit", kill);
        broadcastInfo("miss", miss);
        broadcastInfo("cool", cool);
        broadcastInfo("skill", skill);
        broadcastInfo("laststage", stage);
        startGaugeSchedule();
        return value;
    }

    public void warp(int mapid) {
        for (MapleCharacter chr : getParticipants()) {
            chr.changeMap(mapid);
        }
        if (stage > -1) {
            gaugeTask.cancel();
            gaugeTask = null;

            timer.cancel();
            timer = null;
        } else {
            stage = 0;
        }
    }

    private void broadcastInfo(String info, int amount) {
        for (MapleCharacter chr : getParticipants()) {
            chr.announce(MaplePacketCreator.setSessionValue("massacre_" + info, amount));
            chr.announce(MaplePacketCreator.increaseMassacreGauge(count));
        }
    }

    public boolean useSkill() {
        if (skill < 1) {
            return false;
        }

        skill--;
        broadcastInfo("skill", skill);
        return true;
    }

    private void checkBuffs() {
        int total = (kill + cool);
        if (buffcount == 0 && total >= 250) {
            buffcount++;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (MapleCharacter chr : getParticipants()) {
                ii.getItemEffect(2022585).applyTo(chr);
            }

        } else if (buffcount == 1 && total >= 500) {
            buffcount++;
            skill++;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (MapleCharacter chr : getParticipants()) {
                chr.announce(MaplePacketCreator.setSessionValue("massacre_skill", skill));
                ii.getItemEffect(2022586).applyTo(chr);
            }
        } else if (buffcount == 2 && total >= 1000) {
            buffcount++;
            skill++;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (MapleCharacter chr : getParticipants()) {
                chr.announce(MaplePacketCreator.setSessionValue("massacre_skill", skill));
                ii.getItemEffect(2022587).applyTo(chr);
            }
        } else if (buffcount == 3 && total >= 1500) {
            skill++;
            broadcastInfo("skill", skill);
        } else if (buffcount == 4 && total >= 2000) {
            buffcount++;
            skill++;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (MapleCharacter chr : getParticipants()) {
                chr.announce(MaplePacketCreator.setSessionValue("massacre_skill", skill));
                ii.getItemEffect(2022588).applyTo(chr);
            }
        } else if (buffcount == 5 && total >= 2500) {
            skill++;
            broadcastInfo("skill", skill);
        } else if (buffcount == 6 && total >= 3000) {
            skill++;
            broadcastInfo("skill", skill);
        }
    }

    public void sendScore(MapleCharacter chr) {
        if (exp == 0) {
            int totalkills = (kill + cool);
            if (stage == 5) {
                if (totalkills >= 3000) {
                    rank = 0;
                } else if (totalkills >= 2000) {
                    rank = 1;
                } else if (totalkills >= 1500) {
                    rank = 2;
                } else if (totalkills >= 500) {
                    rank = 3;
                } else {
                    rank = 4;
                }
            } else {
                if (totalkills >= 2000) {
                    rank = 3;
                } else {
                    rank = 4;
                }
            }

            if (rank == 0) {
                exp = (60500 + (5500 * mode.getMode()));
            } else if (rank == 1) {
                exp = (55000 + (5000 * mode.getMode()));
            } else if (rank == 2) {
                exp = (46750 + (4250 * mode.getMode()));
            } else if (rank == 3) {
                exp = (22000 + (2000 * mode.getMode()));
            }

            exp += ((kill * 2) + (cool * 10));
        }
        chr.announce(MaplePacketCreator.getMassacreResult(rank, exp));
        chr.gainExp(exp, true, true);
    }

    public void hit(long damage, Pair<Integer, Integer> cool) {
        if (damage > 0) {
            if (damage >= cool.getLeft()) {
                if ((Math.random() * 100) < cool.getRight()) {
                    cool();
                } else {
                    kill();
                }
            } else {
                kill();
            }
        } else {
            miss();
        }
    }
}


