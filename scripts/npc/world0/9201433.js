
var status = 0;
var jobName;
var job;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    var jobId=cm.getPlayer().getJob().getId();
    if (mode == -1) {
        cm.sendOk("#eWell okay then. Come back if you change your mind.\r\n\r\nGood luck on your training.");
        cm.dispose();
    } else {
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendNext("#eHello #r#h ##k, I'm in charge of Job Advancing.");
        } else if (status == 1) {
            if (cm.getLevel() < 200 && cm.getJobId()==0) {
                if (cm.getLevel() < 8) {
                    cm.sendNext("#eSorry, but you have to be at least level 8 to use my services.");
                    status = 98;
                } else if (cm.getLevel() < 10) {
                    cm.sendYesNo("#eCongratulations on reaching such a high level. Would you like to make the #rFirst Job Advancement#k as a #rMagician#k?");
                    status = 150;
                } else {
                    cm.sendYesNo("#eCongratulations on reaching such a high level. Would you like to make the #rFirst Job Advancement#k?");
                    status = 153;
                }
            } else if (cm.getLevel() < 30) {
                cm.sendNext("#eSorry, but you have to be at least level 30 to make the #rSecond Job Advancement#k.");
                status = 98;
            } else if (cm.getJobId()==400) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Which would you like to be? #b\r\n#L0#Assassin#l\r\n#L1#Bandit#l#k");
            } else if (cm.getJobId()==100) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Which would you like to be? #b\r\n#L2#Fighter#l\r\n#L3#Page#l\r\n#L4#Spearman#l#k");
            } else if (cm.getJobId()==200) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Which would you like to be? #b\r\n#L5#Ice Lightning Wizard#l\r\n#L6#Fire Poison Wizard#l\r\n#L7#Cleric#l#k");
            } else if (cm.getJobId()==300) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Which would you like to be? #b\r\n#L8#Hunter#l\r\n#L9#Crossbowman#l#k");
            } else if (cm.getJobId()==500) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Which would you like to be? #b\r\n#L10#Brawler#l\r\n#L11#Gunslinger#l#k");
            } else if (cm.getJobId()==1200) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L12#Yes#l\r\n#L13#No#l#k");
            } else if (cm.getJobId()==1100) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L14#Yes#l\r\n#L15#No#l#k");
            } else if (cm.getJobId()==1400) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L16#Yes#l\r\n#L17#No#l#k");
            } else if (cm.getJobId()==1300) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L18#Yes#l\r\n#L19#No#l#k");
            } else if (cm.getJobId()==1500) {
                cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L20#Yes#l\r\n#L21#No#l#k");
            } else if (cm.getJobId()==2100) {
			    cm.sendSimple("#eCongratulations on reaching such a high level. Do you want to job advance? #b\r\n#L22#Yes#l\r\n#L23#No#l#k");
			} else if (cm.getLevel() < 70) {
                cm.sendNext("#eSorry, but you have to be at least level 70 to make the #rThird Job Advancement#k.");
                status = 98;
            } else if (cm.getJobId()==410) {
                status = 63;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==420) {
                status = 66;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==310) {
                status = 69;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==320) {
                status = 72;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==210) {
                status = 75;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==220) {
                status = 78;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==230) {
                status = 81;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==110) {
                status = 84;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==120) {
                status = 87;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==130) {
                status = 90;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==520) {
                status = 95;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==510) {
                status = 92;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1210) {
                status = 169;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1110) {
                status = 172;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1410) {
                status = 175;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1310) {
                status = 178;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1510) {
                status = 181;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getLevel() < 120) {
                cm.sendNext("#eSorry, but you have to be at least level 120 to make the #rForth Job Advancement#k.");
                status = 98;
            } else if (cm.getJobId()==411) {
                status = 105;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==421) {
                status = 108;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==311) {
                status = 111;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==321) {
                status = 114;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==211) {
                status = 117;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==221) {
                status = 120;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==231) {
                status = 123;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==111) {
                status = 126;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==121) {
                status = 129;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==131) {
                status = 132;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==511) {
                status = 133;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==521) {
                status = 134;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getLevel() < 200) {
                cm.sendNext("#eSorry, but you have already attained the highest level of your job's mastery. \r\n\r\nHowever, you can #rrebirth#k when you are level 200.");
                status = 98;
            } else {
                cm.dispose();
            }
        } else if (status == 2) {
            if (selection == 0) {
                jobName = "Assassin";
                job = 410;
            }
            if (selection == 1) {
                jobName = "Bandit";
                job = 420;
            }
            if (selection == 2) {
                jobName = "Fighter";
                job = 110;
            }
            if (selection == 3) {
                jobName = "Page";
                job = 120;
            }
            if (selection == 4) {
                jobName = "Spearman";
                job = 130;
            }
            if (selection == 5) {
                jobName = "Ice Lightning Wizard";
                job = 220;
            }
            if (selection == 6) {
                jobName = "Fire Poison Wizard";
                job = 210;
            }
            if (selection == 7) {
                jobName = "Cleric";
                job = 230;
            }
            if (selection == 8) {
                jobName = "Hunter";
                job = 310;
            }
            if (selection == 9) {
                jobName = "Crossbowman";
                job = 320;
            }
            if (selection == 10) {
                jobName = "Brawler";
                job = 510;
            }
            if (selection == 11) {
                jobName = "Gunslinger";
                job = 520;
            }
            if (selection == 12) {
                jobName = "Level 2 Blaze Wizard";
                job = 1210;
            }
            if (selection == 13) {
                cm.sendOk("Come back to me when you are ready.");
                cm.dispose();
            }
            if (selection == 14) {
                jobName = "Level 2 Dawn Warrior";
                job = 1110;
            }
            if (selection == 15) {
                cm.sendOk("Come back to me when you are ready.");
                cm.dispose();
            }
            if (selection == 16) {
                jobName = "Level 2 Night Walker";
                job = 1410;
            }
            if (selection == 17) {
                cm.sendOk("#eCome back to me when you are ready.");
                cm.dispose();
            }
            if (selection == 18) {
                jobName = "Level 2 Wind Archer";
                job = 1310;
            }
            if (selection == 19) {
                cm.sendOk("#eCome back to me when you are ready.");
                cm.dispose();
            }
            if (selection == 20) {
                jobName = "Level 2 Thunder Breaker";
                job = 1510;
            }
			if (selection == 21) {
			    jobName = "Level 2 Aran";
				job = 2110;
			}
            if (selection == 22) {
                cm.sendOk("#eCome back to me when you are ready.");
                cm.dispose();
            }
            cm.sendYesNo("#eDo you want to become a #r" + jobName + "#k?");
        } else if (status == 3) {
            cm.changeJobById(job);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 61) {
            if (cm.getJobId()==410) {
                status = 63;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==420) {
                status = 66;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==310) {
                status = 69;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==320) {
                status = 72;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==210) {
                status = 75;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==220) {
                status = 78;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==230) {
                status = 81;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==110) {
                status = 84;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==120) {
                status = 87;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==130) {
                status = 90;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==520) {
                status = 98;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==510) {
                status = 93;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1210) {
                status = 170;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1110) {
                status = 173;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1410) {
                status = 176;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1310) {
                status = 179;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==1510) {
                status = 182;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==2110) {
			    status = 185;
				cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
			} else {
                cm.dispose();
            }
        } else if (status == 64) {
            cm.changeJobById(411);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 67) {
            cm.changeJobById(421);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 70) {
            cm.changeJobById(311);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 73) {
            cm.changeJobById(321);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 76) {
            cm.changeJobById(211);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 79) {
            cm.changeJobById(221);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 82) {
            cm.changeJobById(231);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 85) {
            cm.changeJobById(111);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 88) {
            cm.changeJobById(121);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 91) {
            cm.changeJobById(131);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 93) {
            cm.changeJobById(511);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 96) {
            cm.changeJobById(521);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 170) {
            cm.changeJobById(1211);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 173) {
            cm.changeJobById(1111);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 176) {
            cm.changeJobById(1411);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 179) {
            cm.changeJobById(1311);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
        } else if (status == 182) {
            cm.changeJobById(1511);
            cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
            cm.dispose();
		} else if (status == 185) {
		    cm.changeJobById(2111);
			cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe :)");
			cm.dispose();
        } else if (status == 99) {
            cm.sendOk("#eGood luck on your training.");
            cm.dispose();
        } else if (status == 102) {
            if (cm.getJobId()==411) {
                status = 105;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==421) {
                status = 108;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==311) {
                status = 111;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==321) {
                status = 114;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==211) {
                status = 117;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==221) {
                status = 120;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==231) {
                status = 123;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==111) {
                status = 126;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==121) {
                status = 129;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==131) {
                status = 132;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==511) {
                status = 134;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else if (cm.getJobId()==521) {
                status = 136;
                cm.sendYesNo("#eCongratulations on reaching such a high level. Do you want to Job Advance now?");
            } else {
                cm.dispose();
            }
        } else if (status == 106) {
            cm.changeJobById(412);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 109) {
            cm.changeJobById(422);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 112) {
            cm.changeJobById(312);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 115) {
            cm.changeJobById(322);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 118) {
            cm.changeJobById(212);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 121) {
            cm.changeJobById(222);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 124) {
            cm.changeJobById(232);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 127) {
            cm.changeJobById(112);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 130) {
            cm.changeJobById(122);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 133) {
            cm.changeJobById(132);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 134) {
            cm.changeJobById(512);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 135) {
            cm.changeJobById(522);
            cm.sendOk("#eThere you go. Hope you enjoy it.");
            cm.dispose();
        } else if (status == 154) {
            cm.sendSimple("#eWhich would you like to be? #b\r\n#L0#Warrior#l\r\n#L1#Magician#l\r\n#L2#Bowman#l\r\n#L3#Thief#l\r\n#L4#Pirate#l#k\r\n\r\n#eOr Do you prefer Knights Of Cygnus?#k\r\n#L5##bDawn Warrior#l\r\n#L6#Night Walker#l\r\n#L7#Blaze Wizard#l\r\n#L8#Wind Archer#l\r\n#L9#Thunder Breaker#l#k\r\n\r\n#eOr do you prefer Aran?#k\r\n#L10#Aran#l");
        } else if (status == 155) {
            if (selection == 0) {
                jobName = "Warrior";
                job = 100;
            }
            if (selection == 1) {
                jobName = "Magician";
                job = 200;
            }
            if (selection == 2) {
                jobName = "Bowman";
                job = 300;
            }
            if (selection == 3) {
                jobName = "Thief";
                job = 400;
            }
            if (selection == 4) {
                jobName = "Pirate";
                job = 500;
            }
            if (selection == 5) {
                jobName = "Dawn Warrior";
                job = 1100;
            }
            if (selection == 6) {
                jobName = "Night Walker";
                job = 1400;
            }
            if (selection == 7) {
                jobName = "Blaze Wizard";
                job = 1200;
            }
            if (selection == 8) {
                jobName = "Wind Archer";
                job = 1300;
            }
            if (selection == 9) {
                jobName = "Thunder Breaker";
                job = 1500;
            }
			if (selection == 10) {
			    jobName = "Aran";
				job = 2100;
			}
            cm.sendYesNo("#eDo you want to become a #r" + jobName + "#k?");
        } else if (status == 156) {
                cm.changeJobById(job);
                cm.sendOk("#eThere you go. Hope you enjoy it. See you around in the future maybe.");
                cm.dispose();
            }
        }
    }