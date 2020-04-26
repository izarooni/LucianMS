//kerrigan

const Union = Java.type("com.lucianms.client.meta.Union");

var union = player.getUnion().getName();
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    else {
        status++;
    }
    if (status == 1) {
        if (union.equalsIgnoreCase("none")) {
          cm.sendSimple("Hello. I am #dIra#k, the leader of the Unicorn union.\r\n#L0#Union Benefits\r\n#L1#I want to become a member of the Unicorn");
        }
        else if (union.equalsIgnoreCase("Kirin")) {
          cm.sendOk("Hello, disciple.");
        }
        else {
          cm.sendOk("Hello. I am #dIra#k, the leader of the Unicorn union.");
        }
    }
    if (status == 2) {
        if (selection == 0) {
          cm.sendOk("Union Benefits:\r\n\r\n+5% exp for mob kills when in a party with 2 or more members, stacking per Unicorn member.");
        }
        if (selection == 1) {
          if (player.getUnion().getName().equals("none") || union == null) {
            union = new Union("Unicorn");
            player.setUnion(union);
            cm.sendOk("You are now a member of the Unicorn.");
          } else {
            cm.sendOk("You appear to already be a member of another union.");
          }
        }
    }
  }
