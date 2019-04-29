const StringUtil = Java.type('tools.StringUtil');
const Server = Java.type('com.lucianms.server.Server');
/**
 * @function: Bulletin Board NPC
 * @author: instincts
 */

function start() {
    let str = 'Welcome to Chirithy!, #h #.\r\n';
    str += "Server Uptime: " + StringUtil.getTimeElapse(Date.now() - Server.Uptime)+ "\r\n";
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
        cm.sendOk("Frequently Asked Questions :\r\n"
        + "\r\n#eWhat are the rates for Chirithy?#n\r\nEXP: 250x // Mesos: 100x // Drop: 3x");
        cm.dispose();
    } else if (s == 1) {
        cm.openNPC(9000037, "daily_login");
        cm.dispose();
    } else if (s == 2) {
        cm.sendOk("Features here");
        cm.dispose();
    } else if (s == 3) {
        cm.sendOk("You may use commands by entering them in the game chat box.\r\nPlayer commands can be listed via < #d@help#k > command and is always ordered alphabetically.\r\nYou may also view the list of commands through an NPC via < #d@help npc#k > command");
        cm.dispose();
    } else if (s == 4) {
        cm.sendOk("#r[Notice]: This staff list may not always be up to date."
        + "\r\n#rOwners:\r\nXehanort\r\nSef\r\nizarooni#k\r\n"
        + "\r\n#bDevelopers:\r\nizarooni\r\nLcas2k19#k\r\n"
        + "\r\nAdministrators:\r\nKill\r\nInstincts\r\nSauce\r\n"
        + "\r\nGame Masters:\r\nSiut\r\nConfirmed\r\nDicezu\r\nUh\r\n"
        + "\r\nGFX:\r\nnoona\r\n\Muneo\r\nBirb\r\n");
        cm.dispose();
    } else if (s == 5) {
        cm.sendOk("#eTo donate, go to our Website #b(www.maplechirithy.com)#k, Click on the donate tab, Enter your username and the amount of money you would like to donate, then you will be redirected to PayPal to complete your donation,  ");
        cm.dispose();
    } else if (s == 6) {
        cm.sendOk("#eVoting is quite easy, basically just go to our Website #b(www.maplechirithy.com)#k, Click on the vote tab, Enter your account username and press vote, you will then be redirected to a page, enter the captcha shown and press vote");
        cm.dispose();
    } else if (s == 7) {
        cm.sendOk("Terms of Service");
        cm.dispose();
    }
}