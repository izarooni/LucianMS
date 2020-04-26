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
        if (union.equalsIgnoreCase("none") || union == null) {
          cm.sendSimple("Hello. I am #dAced#k, the leader of the Ursus union.\r\n#L0#Union Benefits\r\n#L1#I want to join the Ursus");
        }
        else if (union.equalsIgnoreCase("Ursus")) {
          cm.sendOk("Hello, disciple.");
        }
        else {
          cm.sendOk("Hello. I am #dAced#k, the leader of the Ursus union.");
        }
    }
    if (status == 2) {
        if (selection == 0) {
          cm.sendOk("Union Benefits:\r\n\r\n+5 additional AP per 10 levels, stacking infinitely.");
        }
        if (selection == 1) {
          if (player.getUnion().getName().equals("none") || union == null) {
            union = new Union("Ursus");
            player.setUnion(union);
            cm.sendOk("You are now a member of the Ursus union.");
          } else {
            cm.sendOk("You appear to already be a member of another union.");
          }
        }
    }
  }
