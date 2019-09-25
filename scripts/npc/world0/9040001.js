load('scripts/util_gpq.js');
/* 
* @Author Lerk
* 
* Nuris, Sharenian: Returning Path (990001100)
* 
* Exit of Guild Quest
*/
const aToRemove = [
    nItemEarrings,
    nItemRubian, nItemLonginusSpear,
    nItemValorMedal, nItemWisdomScroll, nItemSpoiledFood, nItemNeckiDrink,
    nItemSharenianPants, nItemSharenianShoes, nItemSharenianTop, nItemsharienCrown, 
    nItemEvilMark, nItemRustyKey, nItemKey];

function start() {
    for (let i = 0; i < aToRemove.length; i++)
        cm.removeAll(aToRemove[i]);
    cm.warp(nFieldConstructionSite);
    cm.dispose();
}