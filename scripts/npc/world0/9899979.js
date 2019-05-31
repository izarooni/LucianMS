var status = 0;  
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");

function start() {  
    status = -1;  
    action(1, 0, 0);  
}  

function action(mode, type, selection) {  
       
    if (mode == -1) {  
        cm.dispose();  
    }  
    else {   
        if (status >= 2 && mode == 0) {   
            cm.sendOk("Goodbye");   
            cm.dispose();   
            return;   
        }   
          
        if (mode == 1) {  
            status++;  
        }      
        else {  
            status--;  
        }  
            if (status == 0) { 
            cm.sendOk("#eNeed some help?");
        }
        else if (status == 1) {
            cm.warp(90000012,0);
            cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear4"));
            cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound("customJQ/quest"));
            cm.player.dropMessage("You just received a blessing from the Light");
            cm.dispose();
            }
        }
    }  