/* izarooni */
var seeds = [4001095, 4001097, 4001096, 4001098, 4001099, 4001100];

function act() {
    if (Math.random() < 0.6) {
        rm.dropItem(seeds[Math.floor(Math.random() * seeds.length)], 1);
    }
}
