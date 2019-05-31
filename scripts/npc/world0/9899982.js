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
            cm.sendOk("You are beginning to feel different..You are beginning to remember your past. You are beginning to feel who you used to be.\r\n\r\nIt all makes sense now..I....\r\nI.......\r\n#eI...\r\n\r\n#eI AM \r\n#e...#r#h #");
        }
        else if (status == 1) {
            cm.warp(90000011,0);
            cm.dispose();
            }
        }
    }  