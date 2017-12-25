/* izarooni */
var status = 0;
var features = [LocateReactors];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (this.feature == null) {
        if (status == 1) {
            var text = "What can I help you with?\r\n#b";
            for (var i = 0; i < features.length; i++) {
                text += "\r\n#L" + i + "#" + features[i].name + "#l";
            }
            cm.sendSimple(text);
        } else if (status == 2) {
            this.feature = features[selection];
            status = 0;
            action(1, 0, 0);
        }
    } else {
        this.feature(selection);
    }
}

function LocateReactors(selection) {
    var Locate = function(reactor) {
        var pair = reactor.getReactItem(0);
        if (pair != null) {
            var item = new Packages.client.inventory.Item(pair.getLeft(), 0, pair.getRight());
            player.getMap().spawnItemDrop(player, player, item, reactor.getPosition(), false, true);
        }
    };
    if (status == 1) {
        var text ="\r\n#b";
        text += "\r\n#L" + (player.getMap().getAllReactor().size() + 1) + "# Activate all #l\r\n";
        player.getMap().getAllReactor().stream().forEach(function(r) {
            text += "\r\n#L" + r.getId() + "#" + r.getName() + " - " + r.getId() + "#l";
        });
        cm.sendSimple(text);
    } else if (status == 2) {
        if (selection == player.getMap().getAllReactor().size() + 1) {
            var delay = 500;
            player.getMap().getAllReactor().stream().forEach(function(r) {
                cm.delayCall(function() {
                    Locate(r);
                }, (delay += 300));
            });
        } else {
            var reactor = player.getMap().getReactorById(selection);
            if (reactor != null) {

                Locate(reactor);

                var information = "Located!\r\n";
                information += "\r\nState: " + reactor.getState();
                information += "\r\nAlive: " + reactor.isAlive();
                information += "\r\nReactItem (Left): " + reactor.getReactItem(0).getLeft();
                information += "\r\nReactItem (Right): " + reactor.getReactItem(0).getRight();
                cm.sendOk(information);
            } else {
                cm.sendOk("No such reactor");
            }
        }
        cm.dispose();
    }
}
