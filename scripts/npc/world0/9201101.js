const Occupation = Java.type("com.lucianms.client.meta.Occupation");
const ServerConstants = Java.type("com.lucianms.constants.ServerConstants");
const ChangeCost = 500;

/* izarooni */
/* modified by kerrigan */

var status = 0;
var career_track = 0;
var counter = 0;
var pocc = player.getOccupation();
var requestChange = false;
if (player.isDebug()) {
    pocc = null;
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    else {
        status++;
    }
    if (status == 1) {
        if (pocc == null) {
            let content = "Heya. I'm the Chirithy #dspecialization manager#k. I get paid the big bucks to make minor, unhelpful changes to your gameplay. What would you like from me today?\r\n";
            content += "#d#L0#Give me an overview of all current specializations.\r\n" + "#L1#I'm ready to choose my \"specialization\".";
            cm.sendSimple(content);
          }
        else {
            let content = "You are currently specializing as a #d" + pocc.getType().name() + "\r\n";
            content += "#d#L0#Give me an overview of all current specializations.\r\n" + "#L1#Let me change my \"specialization\"";
            cm.sendSimple(content);
        }
    }
    else if (status == 2) {
        let content = ""
        let types = Occupation.Type.values();
          if (selection == 0) {
            content += "Alright, sure. Which specialization would you like to learn about? (Side note: these specializations are in no way a projection of the developer's personal insecurities.)\r\n#d";
        }
        else if (selection == 1) {
          requestChange = true;
          content += "Sure, here's the list. Take your pick:\r\n#d";
        }
        for (let i = 0; i < types.length; i++) {
            content += "\r\n#d#L" + i + "#" + types[i].name();
        }
        cm.sendSimple(content);
    }
    else if (status == 3) {
        this.career = TypeFromValue(selection);
        if (!requestChange) {
          if (this.career == Occupation.Type.Grinder) {
              cm.sendOk("#d#eSpecialization Overview:#k#n\r\n"
                  + "A solitary creature, a grinder believes that by making larger and larger numbers appear on screen, their life suddenly gains more meaning. Unfortunately, they are wrong. Also, weapon attack is capped at 1999."
                  + "\r\n\r\n#d#eSpecialization \"benefits\"#k#n\r\n"
                  + "Significantly increased EXP rate\r\nSlightly decreased drop rate\r\nSlightly decreased meso rate"
                  + "\r\n\r\n#d#eYou should pick this specialization if:#k#n\r\nThe idea of extended social interactions with others has you running straight back to Ulu city."
              );
          }
          else if (this.career == Occupation.Type.FM_WHORE) {
            cm.sendOk("#d#eSpecialization Overview:#k#n\r\n"
                + "As self-proclaimed \"social butterflies\" that peaked socially in middle school, FM whores enjoy populating the main hub with pointless chatter that eventually spills over into server Discord, much to the dismay of the Discord moderators."
                + "\r\n\r\n#d#eSpecialization \"benefits\"#k#n\r\n"
                + "Very slightly decreased EXP rate\r\nVery slightly decreased drop rate\r\nVery slightly decreased meso rate\r\nDoubled event point payout from GM events\r\nA grab bag of useless commands (@warp, @stun, @bomb, @reverse, @seduce)"
                + "\r\n\r\n#d#eYou should pick this specialization if:#k#n\r\nYou enjoy stream sniping Dafran.\r\nYour name starts with a \"K\" and ends with an \"ayleigh\"."
            );
          }
          else if (this.career == Occupation.Type.Miser) {
            cm.sendOk("#d#eSpecialization Overview:#k#n\r\n"
                + "Because having an abundance of worthless online currency is the same as having material wealth in real life, right? Right?"
                + "\r\n\r\n#d#eSpecialization \"benefits\"#k#n\r\n"
                + "Slightly decreased EXP rate\r\nSlightly decreased drop rate\r\nSignificantly increased meso rate\r\nAccess to @autocoin"
                + "\r\n\r\n#d#eYou should pick this specialization if:#k#n\r\nYour coping mechanism for your inability to pay rent on time is to become rich in an online game."
            );
          }
          else if (this.career == Occupation.Type.Hoarder) {
            cm.sendOk("#d#eSpecialization Overview:#k#n\r\n"
                + "From Wikipedia: \r\nHoarding appears to be more common in people with psychological disorders such as depression, anxiety and attention deficit hyperactivity disorder (ADHD). Other factors often associated with hoarding include alcohol dependence and paranoid, schizotypal and avoidance traits."
                + "\r\n\r\n#d#eSpecialization \"benefits\"#k#n\r\n"
                + "Slightly decreased EXP rate\r\nSignificantly increased drop rate\r\nDecreased meso rate\r\nAccess to petvac (active with a pet)"
                + "\r\n\r\n#d#eYou shouldn't pick this specialization if:#k#n\r\nYou went to look up the above Wikipedia article because it hit a little bit too close to home."
            );
          }
        else if (this.career == Occupation.Type.Mediocrity) {
          cm.sendOk("#d#eSpecialization Overview:#k#n\r\n"
              + "It seems like you're already at the maximum allowed level for this specialization! You must have a lot of prior experience."
              + "\r\n\r\n#d#eSpecialization \"benefits\"#k#n\r\n"
              + "#k#eAbsolutely nothing!#n"
              + "\r\n\r\n#d#eYou should pick this specialization if:#k#n\r\nYou enjoy playing realistic simulation games."
          );
        }
      }
      else if (requestChange) {
        let content = "So you want your specialization to be #d" + this.career.name() + "#k, huh?";
        if (pocc != null) {
            if (player.getOccupation().getType() == Occupation.Type.Grinder) {
            content += "\r\nIt will cost 150 #drebirth points#k to change your specialization. Are you sure about this? They are all equally terrible, don't worry."
            career_track = 1;
            }
            else if (player.getOccupation().getType() == Occupation.Type.FM_WHORE) {
            content += "\r\nIt will cost 2 #devent points#k to change your specialization. Are you sure about this? They are all equally terrible, don't worry."
            career_track = 2;
            }
            else if (player.getOccupation().getType() == Occupation.Type.Miser) {
            content += "\r\nIt will cost 25 #bChirithy coin#k to change your specialization. Are you sure about this? They are all equally terrible, don't worry."
            career_track = 3;
            }
            else if (player.getOccupation().getType() == Occupation.Type.Hoarder) {
            content += "\r\nIt will cost 3 #v2049100# #dchaos scrolls #k to change your specialization. Are you sure about this? They are all equally terrible, don't worry."
            career_track = 4;
            }
        }
        content += "\r\n#d#L0#Yes\r\n#L1#No";
        cm.sendSimple(content);
      }
    }
    else if (status == 4) {
        if (!requestChange || selection == 1) {
            cm.dispose();
            return;
        }
        if (requestChange && selection == 0 && pocc != null) {
            if (player.getOccupation() != null && player.getOccupation().getType() == this.career) {
                cm.sendOk("You already specialize in this, buddy.");
                return cm.dispose();
            }
            else if ((career_track == 1) && (player.getRebirthPoints() >= 150)) {
                player.setRebirthPoints(player.getRebirthPoints() - 150);
                player.sendMessage(`You now have ${player.getRebirthPoints()} rebirth points.`);
            }
            else if ((career_track == 2) && (player.getEventPoints() >= 2)) {
                gainPoints("event points", -1);
                player.sendMessage(`You now have ${player.getEventPoints()} event points.`);
            }
            else if ((career_track == 3) && (player.getItemQuantity(4260002, false) >= 25)) {
                gainPoints(4260002, -25);
                player.sendMessage(`You now have ${player.getItemQuantity(4260002, false)} Chirithy coin.`);
            }
            else if ((career_track == 4) && (player.getItemQuantity(2049100, false) >= 3)) {
                gainPoints(2049100, -3);
                player.sendMessage(`You now have ${player.getItemQuantity(2049100, false)} chaos scrolls.`);
            }
            else if (pocc.getType() == Occupation.Type.Mediocrity) {
            }
            else {
                cm.sendOk("You don't have enough of the required currency to make this change.");
                return cm.dispose();
            }
        }
        player.setOccupation(new Occupation(this.career));
        player.getOccupation().setLevel(1);
        cm.sendOk("Your speciality is now: #d" + this.career + "#k.");
        cm.dispose();
    }
}

function TypeFromValue(n) {
    try {
        return Occupation.Type.values()[n];
    } catch (ignore) {
        return undefined;
    }
}

function gainPoints(s, amt) {
    if (typeof s == 'number') {
        cm.gainItem(s, amt, true);
        return true;
    }
    switch (s) {
        default: return false;
        case "PQ points":
            player.addPoints("pq", amt);
            return true;
        case "event points":
            player.addPoints("ep", amt);
            return true;
        case "donor points":
            player.addPoints("dp", amt);
            return true;
        case "fishing points":
            player.addPoints("fp", amt);
            return true;
        case "vote points":
            player.addPoints("vp", amt);
            return true;
    }
}
