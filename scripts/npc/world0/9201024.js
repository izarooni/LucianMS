load('scripts/util_wedding.js');
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let quest = player.getCustomQuest(POL[2]);
    if (quest == null) {
        cm.sendOk("Nice to meet you! I am Nana the Fairy from Amoria. I am waiting for you to prove your devotion to your loved one by obtaining a Proof of Love! To start, you'll have to venture to Amoria to find my good friend, Moony the Ringmaker. Even if you are not interested in marriage yet, Amoria is open for everyone! Go visit Thomas Swift at Henesys to head to Amoria if you are interested in weddings, be sure to speak with Ames the Wise once you get there!");
        cm.dispose();
    } else if (!quest.checkRequirements()) {
        let content = "Hello, I am Nana the Love Fairy of Ellinia. Isn't it a magnificient place? I'll give you a #bProof of Love#k if you invite me to your wedding...Haha I'm just kidding.\r\nPlease collect these items for me to obtain your Proof of Love. Good luck~\r\n\r\n";
        let items = quest.getToCollect().getItems();
        content += CQuestCollect(items);
        if (player.isDebug()) {
            items.values().forEach(item => {
                cm.gainItem(item.getItemId(), item.getRequirement());
            });
        }
        cm.sendOk(content);
    } else if (!quest.isCompleted()) {
        if (quest.complete(player)) {
            cm.sendOk("Here is your Proof of Love! Have a wonderful wedding~");
        } else {
            cm.sendOk("Please make sure you have enough space in your inventory to receive my Proof of Love");
        }
    } else {
        cm.sendOk("I have already given you my #bProof of Love#k. If you have four different colored Proof of Love travel back to Amoria and talk to my good friend Moony the Ringmaker to begin your engagement.");
    }
    cm.dispose();
}