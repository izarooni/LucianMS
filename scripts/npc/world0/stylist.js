/* izarooni */
var status = 0;
// [0, 1, 2, 3, 4, 5, 9, 10, 11];
var broken = null;
var skins = [10, 3, 0, 11, 1, 2, 5, 4, 9];
var hairs = {
    male: [],
    female: [],
    colors: []
};
var faces = {
    male: [],
    female: [],
    colors: []
};

try {
    var file = new java.io.File("resources/data-styler.json");
    var content = Packages.org.apache.commons.io.FileUtils.readFileToString(file);
    var json = JSON.parse(content);

    hairs.male = json.mHairs.split(", ");
    hairs.female = json.fHairs.split(", ");
    faces.male = json.mFaces.split(", ");
    faces.female = json.fFaces.split(", ");

    hairs.male = split(hairs.male, ((1 / 120) * hairs.male.length));
    hairs.female = split(hairs.female, ((1 / 120) * hairs.female.length));
    faces.male = split(faces.male, ((1 / 120) * faces.male.length));
    faces.female = split(faces.female, ((1 / 120) * faces.female.length));
} catch (e) {
    broken = e;
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 0) {
        status--;
        if (status == 0) {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status == 1) {
        this.sect = null;
        if (broken != null) {
            cm.sendOk(broken);
            cm.dispose();
            return;
        }
        cm.sendSimple(
                "Hey #b#h ##k would you like to change your look?\r\n#b"
                + "\r\n#L0#Skin#l"
                + "\r\n#L1#Hair#l \t #L2#Hair Color#l"
                + "\r\n#L3#Eyes#l \t#L4#Eye Color#l"
                );
    } else if (status == 2) {
        if (this.sect == null)
            this.sect = selection;
        if (this.sect == 0) {
            cm.sendStyle("", skins);
        } else if (this.sect == 1 || this.sect == 3) {
            var text = "";

            var mlength = (this.sect == 1 ? hairs.male.length : faces.male.length);
            var flength = (this.sect == 1 ? hairs.female.length : faces.female.length);
            if (cm.getPlayer().getGender() == 1)
                mlength = 0;
            if (cm.getPlayer().getGender() == 0)
                flength = 0;
            var length = (mlength + flength);

            for (var i = 0; i < length; i++) {
                if (flength > 0) {
                    text += "#r#L" + (100 + i) + "#Female " + (this.sect == 1 ? "hairs" : "faces") + "#l#k";
                    if (mlength > 0)
                        text += "\t";
                    else
                        text += "\r\n";
                }
                if (mlength > 0) {
                    // formatting only for trans; where both sections need to be shown
                    if (cm.getPlayer().getGender() == 2 && flength == 0)
                        text += "\t\t\t\t\t\t\t\t  ";
                    text += "#b#L" + (200 + i) + "#Male " + (this.sect == 1 ? "hairs" : "faces") + "#l#k\r\n";
                }
                if (flength > 0)
                    flength--;
                if (mlength > 0)
                    mlength--;
            }

            cm.sendSimple(text);
        } else if (this.sect == 2 || this.sect == 4) {
            /*
             Hair colors defined by (base + n)
             base being the black (or first color) of the hair
             'n' being the incrementing value from iteration
             
             Face colors are defined by (base + (n * 100))
             base being the black (or first color) of the face
             'n' being the incrementing value from iteration
             */
            var baseId = (this.sect == 2 ?
                    Math.floor(cm.getPlayer().getHair() / 10) * 10 :
                    cm.getPlayer().getFace() - (Math.floor(cm.getPlayer().getFace() / 100 % 10) * 100)
                    );
            for (var i = 0; i < 9; i++) {

                var nId = baseId; // new Id (getting colors)
                if (this.sect == 4)
                    nId += (i * 100); // faces
                else
                    nId += i; // hairs

                var file = new java.io.File("wz/Character.wz/" + (this.sect == 2 ? "Hair" : "Face") + "/000" + (nId) + ".img.xml");
                if (file.exists()) { // some faces and hairs have colors that other's dont
                    (this.sect == 2 ? hairs.colors : faces.colors).push(nId);
                }
                file = null;
            }
            cm.sendStyle("", (this.sect == 2 ? hairs.colors : faces.colors));
        }
    } else if (status == 3) {
        if (this.sect == 0) {
            if (selection >= 0 && selection < skins.length) {
                cm.setSkin(skins[selection]);
            }
            cm.dispose();
        } else if (this.sect == 1 || this.sect == 3) {
            this.gender = (selection >= 200 ? (this.sect == 1 ? hairs.male : faces.male) : (this.sect == 1 ? hairs.female : faces.female));
            this.index = selection >= 200 ? (selection % 200) : (selection % 100);

            cm.sendStyle("", this.gender[selection % (Math.floor(selection / 100) * 100)]);
        } else if (this.sect == 2 || this.sect == 4) {
            if (this.sect == 2)
                cm.setHair(hairs.colors[selection]);
            else if (this.sect == 4)
                cm.setFace(faces.colors[selection]);
            cm.dispose();
        }
    } else if (status == 4) {
        if (this.sect == 1)
            cm.setHair(this.gender[this.index][selection]);
        else if (this.sect == 3)
            cm.setFace(this.gender[this.index][selection]);
        cm.dispose();
    }
}

function split(array, splits) {
    var len = array.length;
    var out = new Array();
    var i = 0;
    while (i < array.length) {
        var size = Math.ceil((len - i) / splits--);
        out.push(array.slice(i, i += size));
    }
    return out;
}
