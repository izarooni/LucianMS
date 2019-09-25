/*
 * @Author:     Venem
 * Map:     Plaza final boss battle
*/
var status = 0;

function start(ms) { 
    ms.openNpc(9899955);
    ms.getPlayer().dropMessage(5, "Defeat Venem and escape the Plaza!");
    ms.getPlayer().getMap(551030393).addMapTimer(180);
}