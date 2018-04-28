/*
 * @Author:     Venem
 * Map:     Plaza final boss battle
*/
var status = 0;

function start(ms) { 
    ms.openNpc(9899953);
    ms.getPlayer().dropMessage(5, "Venem is using his remaining powers! Defeat him and escape the Plaza!");
    ms.getPlayer().getMap(551030394).addMapTimer(180);
}