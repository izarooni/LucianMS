const UGiveaway = Java.type("tools.UniqueGiveaway");

const Calendar  = Java.type("java.util.Calendar");
const TimeZone  = Java.type("java.util.TimeZone");
const ZoneId    = Java.type("java.time.ZoneId");

const GiveawayType  = "BETA_REWARD";
const GiveawayMedal = 1142505;
/* izarooni */
let status = 0;
let firstDay = false;

// what the fuck is going on
TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

let start = Calendar.getInstance(TimeZone.getDefault());
start.set(2018, Calendar.OCTOBER, 13, 20, 30); // Oct 13, 2018 @ 10:30 PM
start.add(Calendar.HOUR_OF_DAY, 2); // utc+2 copenhagen time

let end = Calendar.getInstance(TimeZone.getDefault());
end.set(2018, Calendar.OCTOBER, 13, 20, 30); // Oct 13, 2018 @ 10:30 PM
end.add(Calendar.HOUR_OF_DAY, 38); // First (36+2(utc)) hours to receive the medal

let current = Calendar.getInstance(TimeZone.getDefault());
current.add(Calendar.HOUR_OF_DAY + 2); // utc
if ((current.getTimeInMillis() >= start.getTimeInMillis() && current.getTimeInMillis() <= end.getTimeInMillis())) {
    firstDay = true;
}
TimeZone.setDefault(null);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Welcome! We hope you'll enjoy our server #bLucianMS#k. "
            + "Make sure to join our forum and Discord server to keep up with the latest updates!");
    } else if (status == 2) {
        if (UGiveaway.checkWithBoth(client.getRemoteAddress(), client.getHWID(), GiveawayType)) {
            cm.sendNext("According to our records... You're amazing! Thanks for playing #bLucianMS#k. "
                + "We only give the starter pack to new players, but we hope you're having fun~");
        } else {
            cm.sendNext("Here are a few things to help get you started", 1);
            cm.gainItem(2000002, 200);  // white potions
            cm.gainItem(2000006, 200); // mana elixir
            cm.gainMeso(100000);
            UGiveaway.createData(client.getRemoteAddress(), client.getHWID(), GiveawayType);
        }
    } else if (status == 3) {
        if (firstDay) {
            cm.sendNext("It's launch day!! Thanks for taking time to try out #bLucianMS#k!\r\nFor playing on the first day, we would like to reward you with this very exclusive medal");
            cm.gainItem(GiveawayMedal);
        }
        cm.dispose();
    }
}
