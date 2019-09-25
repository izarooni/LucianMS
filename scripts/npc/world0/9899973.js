/* original by izarooni */
/* remade by kerrigan */
/* jshint esversion: 6 */

let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.openNpc(9899973, "f_poly");
    }
}
