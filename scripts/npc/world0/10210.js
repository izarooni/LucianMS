var status = 0;  

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
            cm.sendOk("Hello, #h #. I see that you have returned. Well..as you know you were striked down by #rXehanort#k to then be resurrected by #bKill#k.\r\n\r\nArghh..It saddens me too see that two of my disciple went their seperate ways. One to restore light to the world and the other to have a world full of #edarkness#k#n\r\n\r\nYou stood no chance and it is all my fault..if I just had been more aware of #eXehanorts#k#n sudden new attitude and interest in research of the dark, I could have prevented all this from happening.\r\nNo more sad talk. It is time for you to regain your strength and finish off #eXehanort#k#n once and for all! Please, be safe on your new adventure.");
        }
        else if (status == 1) {
            cm.warp(910000000,0);
            cm.dispose();
            }
        }
    }  