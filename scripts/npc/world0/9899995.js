// Casino NPC
// credits ragezone
var status = 0;
var price = 200000;
var scammed = 200000;
var prize1 = 500000;
var prize2 = 1000000;
var prize3 = 3000000;
var prize4 = 7000000;
                             

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        } else {
        if (cm.getMeso() <= price) {
            cm.sendOk("You don't have enough mesos to gamble!");
            cm.dispose();
            return;
        }
        if (mode == 0) {
            cm.sendOk("Come back later!");
            cm.dispose();
            return;
        }
        status++;
        if (status == 0) {
            cm.sendNext("Hello #d#h ##k!\r\nWould you like to gamble some of your mesos? There is a big chance to win big money here!");
        } else if (status == 1) {
            cm.sendSimple("Alright, here we go! \r\n#L0##dGamble Mesos ("+ price +" mesos)#k#l\r\n#L1##dNevermind...#k#l")
        } else if (status == 2) {
                     if (selection == 0) {
                                cm.gainMeso(-price);
                                var playernumber = Math.floor(Math.random()*1000+1); //Random number from 1 to 1000
                if (playernumber >= 0 && playernumber <= 100) {
                                    
                                    cm.sendOk("Congrats, #d#h ##k! You won "+ scammed +" mesos!\r\n\r\nOh, wait. Nevermind, I just scammed you for "+ scammed +" mesos, haha!~");
                                    cm.gainMeso(-scammed);
                                    winningplayer = cm.getChar().getName();
                                    cm.mapMessage(5, "[CASINO] "+ winningplayer +" just got scammed "+ scammed +" mesos! See, gambling is bad for you!~");
                                    cm.dispose();
                } else if(playernumber >= 200 && playernumber <= 210) {
                                    
                                    cm.sendOk("Congrats, #d#h ##k! You won "+ prize1 +" mesos!");
                                    cm.gainMeso(prize1);
                                    winningplayer = cm.getChar().getName();
                                    cm.mapMessage(5, "[CASINO] "+ winningplayer +" hit the jackpot ("+ prize1 +" mesos)! Don't spend it all in one place!~");
                                    cm.dispose();
                } else if(playernumber >= 400 && playernumber <= 410) {
                                    
                                    cm.sendOk("Congrats, #d#h ##k! You won "+ prize2 +" mesos!");
                                    cm.gainMeso(prize2);
                                    winningplayer = cm.getChar().getName();
                                    cm.mapMessage(5, "[CASINO] "+ winningplayer +" hit the jackpot ("+ prize2 +" mesos)! Don't spend it all in one place!~");
                                    cm.dispose();
                } else if(playernumber >= 600 && playernumber <= 610) {
                                    
                                    cm.sendOk("Congrats, #d#h ##k! You won "+ prize3 +" mesos!");
                                    cm.gainMeso(prize3);
                                    winningplayer = cm.getChar().getName();
                                    cm.mapMessage(5, "[CASINO] "+ winningplayer +" hit the jackpot ("+ prize3 +" mesos)! Don't spend it all in one place!~");
                                    cm.dispose();
                } else if(playernumber >= 800 && playernumber <= 810) {
                                    
                                    cm.sendOk("Congrats, #d#h ##k! You won "+ prize4 +" mesos!");
                                    cm.gainMeso(prize4);
                                    winningplayer = cm.getChar().getName();
                                    cm.mapMessage(5, "[CASINO] "+ winningplayer +" hit the jackpot ("+ prize4 +" mesos)! Don't spend it all in one place!~");
                                    cm.dispose();
                } else {
                                    cm.sendOk("Sorry, you didn't win anything this time. Please try again.")
                                    cm.dispose();
                }
                        } else if (selection == 1) {
                                cm.sendOk("I guess you have better things to spend your mesos on, hope to see you around these parts again!!");
                cm.dispose();
                        }
                }
        }
}