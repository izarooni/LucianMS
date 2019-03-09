/*
Warp to Sharen III's Grave - Guild Quest
Give guild points if holding appropriate item and not gained already
Save location to return.

@Author Lerk
*/

function enter(pi) {
    let player = pi.getPlayer();
    if (player.isGM() || player.getLevel() <= 30) {
        pi.warp(990000640, 1);
        return true;
    } else {
        pi.getPlayer().dropMessage(5, "You cannot proceed past this point.");
        return false;
    }
}