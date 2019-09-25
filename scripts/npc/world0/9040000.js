load('scripts/util_gpq.js');
/* izarooni */
let status = 0;
let eimName = `GuildQuest-${ch.getId()}`;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("The path to Sharenian starts here. What would you like to do?#b"
        + "\r\n#L0#Start a Guild Quest#l"
        + "\r\n#L1#Join your guild's Guild Quest#l");
    } else if (status == 2) {
        if (selection == 0) return StartGPQ();
        else if (selection == 1) return JoinGPQ();
    }
}

function JoinGPQ() {
    let lStatus = (status - 1);

    let guild = player.getGuild();
    if (guild == null) {
        cm.sendOk("You must be in a guild to join the Guild Quest.");
        return cm.dispose();
    }

    let em = ch.getEventScriptManager().getManager("GuildQuest");
    if (em == null) {
        cm.sendOk("This trial is currently under construction.");
        return cm.dispose();
    }
    let eim = em.getInstance(eimName);
    if (eim == null) {
        cm.sendOk("There is no guild entered into the Guild Quest.");
        cm.dispose();
    }

    if (lStatus == 1) {
        eim.registerPlayer(player);
        cm.sendOk(`The guild has been entered into the Guild Quest. Please report to Shuang at the Excavation Camp on channel ${ch.getId()}`);
        cm.dispose();
    }
}

function StartGPQ() {
    let lStatus = (status - 1);

    if (!cm.isLeader()) {
        cm.sendOk("Only the party leader may start the Guild Quest.");
        return cm.dispose();
    }

    let guild = player.getGuild();
    if (guild == null || player.getGuildRank() >= 3) {
        cm.sendOk("Only a Guild Master or Jr. Master may start the Guild Quest");
        return cm.dispose();
    }

    let em = ch.getEventScriptManager().getManager("GuildQuest");
    if (em == null) {
        cm.sendOk("This trial is currently under construction.");
        return cm.dispose();
    }
    let eim = em.getInstance(eimName);
    if (eim != null) {
        if (eim.vars.guildID != guild.getId()) {
            cm.sendOk("Another guild has already registered for the quest. Please try again later.");
        } else {
            cm.sendOk("Your guild has already been entered for the Guild Quest.")
        }
        return cm.dispose();
    }

    if (lStatus == 1) {
        eim = em.startInstance(cm.getParty(), player.getMap());
        eim.vars.guildID = guild.getId();
        // if (player.isDebug()) {
            eim.vars.debug = true;
            eim.vars.playerID = player.getId();
            player.sendMessage("Guild Quest has started in debug mode");
        // }
        cm.dispose();
    }
}