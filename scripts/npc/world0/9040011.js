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
    str += "#d#e#L0#Getting Started#l#n\r\n";
    str += "#b#L1#Frequently Asked Questions (FAQ)#l\r\n";
    str += "#k#L2#Receive your #dDaily Reward#l\r\n";
    str += "\r\n";
    str += "#L3##kFeatures#l\r\n";
    str += "#L4##kPlayer Commands#l\r\n";
    str += "#L5##kStaff List#l\r\n";
    str += "#L6##kHow to #dDonate#l\r\n";
    str += "#L7##kHow to #dVote#l\r\n";
    str += "#L8##kChirithy Terms of Service#l\r\n";
    cm.sendSimple(str);
}

function action(m, t, s) {
    if (m == -1) {
        cm.dispose();
        return;
    }
    if (s == 0) {
        cm.sendOk("Getting Started:\r\n"
        + "\r\nChirithy is a high rate rebirth server. A good place to get starting gear is #dGlimmerman#k, who sells level 1 weapons and starting armor for #bChirithy coin#k (Be sure to collect your daily reward for some easy coins). \r\n\r\nTo get started with training, check out the multipurpose NPC tab at the bottom right corner. The \"Warper\" option allows you to select training maps, or you can go with Ienzo's suggested training map. Once you have maxed your stats, consider talking to the Chirithy NPC to make an MSI.\r\n\r\nBe sure to take advantage of the server's other features including Specializations, BossPQ, and the Arcade, which can give you special bonuses and weapon attack gear.");
        cm.dispose();
    }
    else if (s == 1) {
        cm.sendOk("Frequently Asked Questions:\r\n"
        + "\r\n#eWhat are the rates for Chirithy?#n\r\nEXP: 500x // Mesos: 250x // Drop: 3x\r\n\r\n#eWhat is the server's currency?#n\r\n#i4260002#\r\n\r\n#eHow do I make an MSI?#n\r\nIn order to make a MSI, you will need to speak to Chirithy which is located in the Free Market. As well, you will need to acquire 32k in all stats and fulfill specific conditions depending on what MSI option you select");
        cm.dispose();
    } else if (s == 2) {
        cm.openNpc(9000037, "f_daily_login");
        cm.dispose();
    } else if (s == 3) {
        cm.sendOk("#eThese are the features of Chirithy v83:#n\r\n\r\n#d#eCustom bosses: #k#nSpace Slime, Black Mage commander, Black Mage, Mysterious Adversary, Kaneki, Ultimate Mushroom, The Wall.\r\n\r\n#d#eCustom maps: #k#nMario, Wario, JQ, The Wall, Smash Bros, Wario, Outer Space, Realm of Gods, Concert, Home & FM.\r\n\r\n#d#eCustom items: #k#nWeapons, Mounts, Pets, Chairs, Accesories, Gloves, Hats.\r\n\r\n#d#eCustom Skills and jobs: #k#nUchiha, Valkyrie, Dragoon, Dancer, Ark, Pathfinder, Cadena, Rashoumon, Luminous, Mechanic.\r\n\r\n#d#eCustom events: #k#nDragon Ball Z and Planet Aura.\r\n\r\n#d#eArcade: #k#nExperience the arcade with tons of fun arcade games!\r\n\r\n#d#eStoryline: #k#nStart your journey with Chirithy and fight the darkness against Master Xehanort!\r\n\r\n#d#eAuto-events: #k#nParticipate in the events that starts automatically!\r\n\r\n#d#eCustom PVP: #k#nPush your friends off the map with our Smash Bros PVP system!\r\n\r\n#d#eWeddings: #k#nMarry your significant other with our marriage system!");
        cm.dispose();
    } else if (s == 4) {
        cm.sendOk("You may use commands by entering them in the game chat box.\r\nPlayer commands can be listed via < #d@help#k > command and is always ordered alphabetically.\r\nYou may also view the list of commands through an NPC via < #d@help npc#k > command");
        cm.dispose();
    } else if (s == 5) {
        cm.sendOk("#r[Notice]: This staff list may not always be up to date.\r\n"
        + "\r\n\t\t\t\t\t\t\t\t#r#eOwners :#n#k\r\n\t\t\t\t\t\t\tFeinT UTC+2"
        + "\r\n\t\t\t\t\t\t\t\t#b#eDevelopers :#n#k\r\n\t\t\t\t\t\t\tizarooni\r\n\t\t\t\t\t\t\t69/Jeemie \r\n\t\t\t\t\t\t\t Kerrigan \r\n\t\t\t\t\t\t\t yeehaw\r\n"
        + "\r\n\t\t\t\t\t\t\t\t#k#eAdministrators :#n#k\r\n\t\t\t\t\t\t\tRai\r\n\t\t\t\t\t\t\t\r\n"
        + "\r\n\t\t\t\t\t\t\t\t#d#eGame Masters#e :#n#k\r\n\t\t\t\t\t\t\tDesco\r\n\t\t\t\t\t\t\tBrazzers\r\n\t\t\t\t\t\t\tImp\r\n\t\t\t\t\t\t\t\r\n"
        + "\r\n\t\t\t\t\t\t\t\t#g#eGFX :#n#k\r\n\t\t\t\t\t\t\tnoona");
        cm.dispose();
    } else if (s == 6) {
        cm.sendOk("#eTo donate, go to our Website #b(www.maplechirithy.com)#k, Click on the donate tab, Enter your username and the amount of money you would like to donate, then you will be redirected to PayPal to complete your donation,  ");
        cm.dispose();
    } else if (s == 7) {
        cm.sendOk("#eVoting is quite easy, basically just go to our Website #b(www.maplechirithy.com)#k, Click on the vote tab, Enter your account username and press vote, you will then be redirected to a page, enter the captcha shown and press vote");
        cm.dispose();
    } else if (s == 8) {
        cm.sendOk("#eChirithy's Term of Service#n: \r\n\r\n#eHacking#n: \r\nThe term 'hacking' will be used to cover a variety of things. If you have to open a third-party program to do ANYTHING on the server, or to it's files, you are breaking this rule. This includes, but not limited to, client editing, wz editing, botting, duping, and map crashing.\r\n\r\n#eBugs and Exploits#n: \r\nBugs/Exploits are problems within the game that shouldn't belong. For example, you know an item is very rare to obtain in our server. The item can only be retrieved by killing a hard boss. But, one day while searching with your friends, you find a map with mobs. All the mobs drop that etc. You continue to farm them instead of reporting it.\r\n\r\n#eScamming#n: \r\nIf you propose the idea to trade an item for mesos, nx, or gear you must abide by it. If you choose to scam that person (you decide to take the item without paying for it) you will be punished. If any of you plan on trading and you'd like the safety of not being scammed, please message a GM (using @callgm) and a GM will spectate the trade. If no GM is available, please record the transaction to prevent anything happening, or wait for a GM to log on.\r\n\r\n#eMulticlienting#n: Multiclient is NOT allowed on this server. If you are caught multiclienting, you will be jailed, and, on subsequent attempts, banned.");
        cm.dispose();
    }
}
