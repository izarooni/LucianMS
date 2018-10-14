load('scripts/util_imports.js');
var TimeUnit = Java.type("java.util.concurrent.TimeUnit");
var Calendar = Java.type("java.util.Calendar");
/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let con = Database.getConnection();
        try {
            var ps = con.prepareStatement("select daily_login, login_streak from accounts where id = ?");
            ps.setInt(1, client.getAccID());
            var rs = ps.executeQuery();
            if (rs.next()) {
                var timestamp = rs.getTimestamp("daily_login");
                this.streak = rs.getInt("login_streak");
                if (timestamp != null) {
                    var calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timestamp.getTime());
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
                    var ttd = calendar.getTime().getTime();
                    var now = Date.now();
                    ttd = parseInt(ttd); // js trust issues -3-
                    if (now >= ttd) { // it's been at least 24 hours since last login
                        if (now - ttd >= TimeUnit.DAYS.toMillis(1)) { // been 2+ days since login
                            cm.sendNext("Aw man, your " + this.streak + " daily login streak has been crushed!\r\nSadly, it has been over #b" + TimeUnit.MILLISECONDS.toDays(now - ttd) + " days#k since your last login.\r\nYou'll have to start over now...");
                        } else {
                            cm.sendNext("#eLogin Streak: #b" + this.streak + "#k#n\r\nThanks for playing, remember to vote for us~\r\nHave a nice day!");
                        }
                    } else {
                        cm.sendOk("We know you're excited to play, but you've already claimed your daily login prize within the last 24 hours!\r\nCome back in #b" + StringUtil.getTimeElapse(ttd - now));
                        cm.dispose();
                    }
                } else {
                    cm.sendNext("This is your first daily login reward ever!\r\nBe sure to come back in 24 hours to claim tomorrow's prize~");
                }
            }
        } finally { con.close(); }
    } else if (status == 2) {
        if (giveReward(this.streak)) {
            cm.sendOk("Here is your reward for your #b" + this.streak + aaa(this.streak) + "#k daily login reward!");
            this.streak += 1;
            recordStreak(this.streak);
        } else {
            cm.sendOk("Please make sure you have room in your inventory before claiming your reward");
        }
        cm.dispose();
    }
}

function recordStreak(streak) {
    let con = Database.getConnection();
    try {
        var ps = con.prepareStatement("update accounts set daily_login = ?, login_streak = ? where id = ?");
        ps.setTimestamp(1, new java.sql.Timestamp(java.lang.System.currentTimeMillis()));
        ps.setInt(2, streak);
        ps.setInt(3, client.getAccID());
        ps.executeUpdate();
    } finally { con.close(); }
}

function aaa(streak) {
    var sw = (streak % 10);
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
