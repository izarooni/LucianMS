//Ains Crossover event item trader 
//FeinT
 
function start() {
    status = -1;
    action(1, 0, 0);
}
 
function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendSimple ("WELCOME! weakling. Do you by any chance carry any #i4011040# ?\r\n\If so, I'd like to receive them for my own collection in exchange of some very sweet items! \r\n\ #L0# Trade 4 #i4011040# for 1 #i1012701# \r\n #L1# Trade 8 #i4011040# for 1 #i1053479# \r\n\ #L2# Trade 8 #i4011040# for 1 #i1053480# \r\n #L3# Trade 8 #i4011040# for 1 #i1053481# \r\n #L4# Trade 8 #i4011040# for 1 #i1053482# \r\n #L5# Trade 8 #i4011040# for 1 #i1053483# \r\n #L6# Trade 8 #i4011040# for 1 #i1053484# \r\n #L7# Trade 8 #i4011040# for 1 #i1053485# \r\n #L8# Trade 6 #i4011040# for 1 #i1005406# \r\n #L9# Trade 6 #i4011040# for 1 #i1005407# \r\n #L10# Trade 6 #i4011040# for 1 #i1005408# \r\n #L11# Trade 4 #i4011040# for 1 #i1073382# \r\n #L12# Trade 4 #i4011040# for 1 #i1073383# \r\n #L13# Trade 4 #i4011040# for 1 #i1073384# \r\n #L14# Trade 4 #i4011040# for 1 #i1073385# \r\n #L15# Trade 4 #i4011040# for 1 #i1073386# \r\n #L16# Trade 10 #i4011040# for 1 #i1702949# \r\n #L17# Trade 1 #i4011040# for 1 #i2002031#");
                  } else if (selection == 0) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1012701, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053479, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                  } else if (selection == 2) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053480, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
                  }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                } else if (selection == 3) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053481, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                } else if (selection == 4) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053482, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                } else if (selection == 5) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053483, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                } else if (selection == 6) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053484, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
        }
 
                } else if (selection == 7) {
                                  if(cm.haveItem(4011040, 8)) {
                                  cm.gainItem(1053485, 1);
                  cm.gainItem(4011040, -8);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 8) {
                                  if(cm.haveItem(4011040, 6)) {
                                  cm.gainItem(1005406, 1);
                  cm.gainItem(4011040, -6);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 9) {
                                  if(cm.haveItem(4011040, 6)) {
                                  cm.gainItem(1005407, 1);
                  cm.gainItem(4011040, -6);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 10) {
                                  if(cm.haveItem(4011040, 6)) {
                                  cm.gainItem(1005408, 1);
                  cm.gainItem(4011040, -6);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 11) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1073382, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 12) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1073383, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 13) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1073384, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 14) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1073385, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 15) {
                                  if(cm.haveItem(4011040, 4)) {
                                  cm.gainItem(1073386, 1);
                  cm.gainItem(4011040, -4);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 16) {
                                  if(cm.haveItem(4011040, 10)) {
                                  cm.gainItem(1702949, 1);
                  cm.gainItem(4011040, -10);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
		}
 
                } else if (selection == 17) {
                                  if(cm.haveItem(4011040, 1)) {
                                  cm.gainItem(2002031, 1);
                  cm.gainItem(4011040, -1);
                                  cm.sendOk("Here's your EXP ticket!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have #i4011040#. Come back when you have.");
        cm.dispose();
 }
 }
 }
 }