/**
 * @function: Bulletin Board NPC

 * @author: instincts
 */


var FAQ = "Frequently Asked Questions (FAQ)\r\n=================================";

function start() {
    var str = 'Welcome to Chirithy!, #h #.\r\n';
    str += "Server Uptime: <todo>\r\n";
    str += "Date: " + new Date().toString() + "\r\n\r\n";
    str += "#b#e#L0#Frequently Asked Questions (FAQ)#l\r\n";
    str += "#r#e#L1#Receive your Daily Reward#l\r\n";
    str += "\r\n";
    str += "#L2#Features#l\r\n";
    str += "#L3#Player Commands#l\r\n";
    str += "#L4#Staff List#l\r\n";
    str += "#L5#How to Donate#l\r\n";
    str += "#L6#How to Vote#l\r\n";
    str += "#L7#Chirithy Terms of Service#l\r\n";

    cm.sendSimple(str);
}

function action(m, t, s) {
    if (m == -1) {
        cm.dispose();
        return;
    }

    if (s == 0) {
        cm.sendOk("Frequently Asked Questions : \r\n\r\nWhat are the rates for Chirithy?\r\n250x/100x/3x");
        cm.dispose();
    } else if (s == 1) {
        cm.openNPC(9000037, "daily_login");
        cm.dispose();
    } else if (s == 2) {
        cm.sendOK("Features here");
        cm.dispose();
    } else if (s == 3) {
        cm.sendOk("Player Commands npc here");
        cm.dispose();
    } else if (s == 4) {
        cm.sendOk("#eThe current staff is listed below:\r\n#rOwners:\r\nXehanort\r\nSef\r\nIzarooni#k\r\n\r\n#bDevelopers:\r\nIzarooni\r\nLcas2k19#k\r\n\r\nAdministrators:\r\nKill\r\nInstincts\r\nSauce\r\n\r\nGame Masters:\r\nSiut\r\nConfirmed\r\nDicezu\r\nUh\r\n\r\nGFX:\r\nnoona\r\n\Muneo\r\nBirb\r\n\r\n#r[Notice]: The staff list will contantly be updated.");
        cm.dispose();
    } else if (s == 5) {
        cm.sendOk("#eTo donate, go to our Website #b(www.maplechirithy.com)#k, Click on the donate tab, Enter your username and the amount of money you would like to donate, then you will be redirected to PayPal to complete your donation,  ");
        cm.dispose();
    } else if (s == 6) {
        cm.sendOk("#eVoting is quite easy, basically just go to our Website #b(www.maplechirithy.com)#k, Click on the vote tab, Enter your account username and press vote, you will then be redirected to a page, enter the captcha shown and press vote\r\n#r[NOTICE]: Make sure you're logged off before voting");
        cm.dispose();
    } else if (s == 7) {
        cm.sendOk("Terms of Service");
        cm.dispose();
    }
}