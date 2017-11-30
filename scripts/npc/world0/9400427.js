//Cameow
//Golden Leaves Trader 
//Golden maple leaf id 4000313

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
            cm.sendSimple ("Hello. I am Cameow! The cutest kitty around. I have some sweet prizes in exchange for some Golden Maple Leaves. \r\n\Are you interested in any of the prizes? \r\n\Remember to come back from time to time to check for new items! \r\n\#L0# Trade 50 #i4000313# for 1 #i1002761# \r\n\#L1# Trade 50 #i4000313# for 1 #i1302062# \r\n\#L2# Trade 150 #i4000313# for 1 #i1003027# \r\n\#L3# Trade 999 #i4000313# for 1 #i1003068# \r\n\#L4# Trade 600 #i4000313# for 1 #i1082399#"); 
                  } else if (selection == 0) { 
                                  if(cm.haveItem(4000313, 50)) { 
                                  cm.gainItem(1002761, 1); 
                  cm.gainItem(4000313, -50); 
                                  cm.sendOk("Here's your Blue Eye Scanner!"); 
                  cm.dispose(); 
        } 
        else { 
        cm.sendOk("You don't have enough Golden Maple Leaves! Come back when you have enough."); 
        cm.dispose(); 
        } 
                  } else if (selection == 1) { 
                                  if(cm.haveItem(4000313, 50)) { 
                                  cm.gainItem(1302062, 1); 
                  cm.gainItem(4000313, -50); 
                                  cm.sendOk("Here's your Green Eye Scanner!"); 
                  cm.dispose(); 
        } 
        else { 
        cm.sendOk("You don't have enough Golden Maple Leaves! Come back when you have enough."); 
        cm.dispose();  
        } 
         
                  } else if (selection == 2) { 
                                  if(cm.haveItem(4000313, 150)) { 
                                  cm.gainItem(1003027, 1); 
                  cm.gainItem(4000313, -150); 
                                  cm.sendOk("Here's your Pink Eye Scanner!"); 
                  cm.dispose(); 
        } 
        else { 
        cm.sendOk("You don't have enough Golden Maple Leaves! Come back when you have enough."); 
        cm.dispose();  
        }  } else if (selection == 3) { 
                                  if(cm.haveItem(4000313, 999)) { 
                                  cm.gainItem(1003068, 1); 
                  cm.gainItem(4000313, -999); 
                                  cm.sendOk("Here's your  Red Eye Scanner!"); 
                  cm.dispose(); 
        }         
        else { 
        cm.sendOk("You don't have enough Golden Maple Leaves! Come back when you have enough."); 
        cm.dispose();  
      }  } else if (selection == 4) { 
                                  if(cm.haveItem(4000313, 600)) { 
                                  cm.gainItem(1082399, 1); 
                  cm.gainItem(4000313, -600); 
                                  cm.sendOk("Here's your  Red Eye Scanner!"); 
                  cm.dispose(); 
        }         
        else { 
        cm.sendOk("You don't have enough Golden Maple Leaves! Come back when you have enough."); 
        cm.dispose();  
        
}  
 }  
 }  
 }   