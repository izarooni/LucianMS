load('scripts/util_imports.js');
const TimeUnit = Java.type("java.util.concurrent.TimeUnit");
const Calendar = Java.type("java.util.Calendar");
const now = Date.now();
/* izarooni */
let status = 0;
let streak = null;
let ttd = null;
let show = true;

{
    let con = cm.getDatabaseConnection();
    try {
        let ps = con.prepareStatement("select daily_login, daily_showable, login_streak from accounts where id = ?");
        ps.setInt(1, client.getAccID());
        let rs = ps.executeQuery();
        if (rs.next()) {
            show = rs.getBoolean("daily_showable");
            let timestamp = rs.getTimestamp("daily_login");
            streak = rs.getInt("login_streak");
            if (timestamp != null) {
                let calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timestamp.getTime());
                calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
                ttd = calendar.getTime().getTime();
                ttd = parseInt(ttd);
            }
        }
        ps.close();
        rs.close();
    } finally { con.close(); }
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status === 1) {
        if (ttd != null) {
            if (now >= ttd) { // it's been at least 24 hours since last login
                setShowable(true);
                if (now - ttd >= TimeUnit.DAYS.toMillis(1)) { // been 2+ days since login
                    cm.sendNext("Aw man, your " + streak + " daily login streak has been crushed!\r\nSadly, it has been over #b" + TimeUnit.MILLISECONDS.toDays(now - ttd) + " days#k since your last login.\r\nYou'll have to start over now...", 1);
                    recordStreak(1);
                    giveReward(1);
                    cm.dispose();
                } else {
                    cm.sendNext("#eLogin Streak: #b" + (streak + 1) + "#k#n\r\nThanks for playing, remember to vote for us~\r\nHave a nice day!", 1);
                }
            } else {
                if (show) {
                    cm.sendSimple("You're currently on a #b" + streak + "#k day login streak!\r\nYour next attendance is in #b" + StringUtil.getTimeElapse(ttd - now) + "\r\n\r\n#L0##bDon't show again for today#l");
                } else {
                    cm.dispose();
                }
            }
        } else {
            cm.sendNext("This is your first daily login reward ever!\r\nBe sure to come back in 24 hours to claim tomorrow's prize~", 1);
        }
    } else if (status === 2) {
        if (now < ttd) {
            setShowable(false);
            cm.sendOk("Don't forget to login tomorrow~");
            cm.dispose();
        } else {
            if (giveReward(streak)) {
                cm.sendSimple("Here is your reward for your #b" + streak + aaa(streak) + "#k daily login reward!\r\n\r\n#L0##bDon't show again for today#l");
                streak += 1;
                recordStreak(streak);
            } else {
                cm.sendOk("Please make sure you have room in your inventory before claiming your reward");
                cm.dispose();
            }
        }
    } else if (status === 3 && selection === 0) {
        status = 1;
        ttd = Date.now() + 1; // doesn't matter just make it later than execution time
        action(1, 0 , 0);
    }
}

function setShowable(b) {
    let con = cm.getDatabaseConnection();
    try {
        let ps = con.prepareStatement("update accounts set daily_showable = ? where id = ?");
        ps.setBoolean(1, b);
        ps.setInt(2, player.getAccountID());
        ps.executeUpdate();
        ps.close();
    } finally { con.close(); }
}

function recordStreak(streak) {
    let con = cm.getDatabaseConnection();
    try {
        let record = con.prepareStatement("update accounts set daily_login = ?, login_streak = ? where id = ?");
        record.setTimestamp(1, new java.sql.Timestamp(java.lang.System.currentTimeMillis()));
        record.setInt(2, streak);
        record.setInt(3, client.getAccID());
        record.executeUpdate();
        record.close();
    } finally { con.close(); }
}

function aaa(streak) {
    let sw = (streak % 10);
    switch (sw) {
        default: return "th";
        case 1: return "st";
        case 2: return "nd";
        case 3: return "rd";
    }
}

function giveReward(streak) {
    if (InventoryModifier.checkSpace(client, 2002031, 1, "")) {
        cm.gainItem(2002031, 1, true); // EXP Ticket
        player.gainMeso(25000, true);
        return true;
    }
    return false;
}
