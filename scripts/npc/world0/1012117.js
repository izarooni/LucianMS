/* izarooni */
var status = 0;

var skins = [0, 1, 2, 3, 4, 5, 9, 10];
var hairs = {
    male: [30000, 30010, 30020, 30030, 30040, 30050, 30060, 30070, 30080, 30090, 30100, 30110, 30120, 30130, 30140, 30150, 30160, 30170, 30180, 30190, 30200, 30210, 30220, 30230, 30240, 30250, 30260, 30270, 30280, 30290, 30300, 30310, 30320, 30330, 30340, 30350, 30360, 30370, 30380, 30400, 30410, 30420, 30430, 30440, 30450, 30460, 30470, 30480, 30490, 30510, 30520, 30530, 30540, 30550, 30560, 30570, 30580, 30590, 30600, 30610, 30620, 30630, 30640, 30650, 30660, 30670, 30680, 30690, 30700, 30710, 30720, 30730, 30740, 30750, 30760, 30770, 30780, 30790, 30800, 30810, 30820, 30830, 30840, 30850, 30860, 30870, 30880, 30890, 30900, 30910, 30920, 30930, 30940, 30950, 32370, 32380, 32390, 32400, 32410, 32420, 32430, 32440, 32450, 32460, 32470, 32480, 32490, 32500, 32520, 32640, 33000, 33040, 33050, 33060, 33070, 33080, 33090, 33100, 33110, 33120, 33130, 33140, 33150, 33170, 33180, 33190, 33200, 33210, 33220, 33240, 33250, 33260, 33270, 33280, 33290, 33300, 33310, 33320, 33330, 33340, 33350, 33360, 33370, 33380, 33390, 33400, 33410, 33420, 33430, 33440, 33450, 33460, 33470, 33480, 33500, 33510, 33520, 33530, 33540, 33550, 33580, 33590, 33600, 33610, 33620, 33630, 33640, 33660, 33670, 33690, 33700, 33710, 33720, 33730, 33740, 33750, 33760, 33770, 33780, 33790, 33800, 33810, 33820, 33830, 33930, 33940, 33950, 33960, 33990, 34000, 35130, 35140, 35150, 35160, 35170, 35180, 35190, 35200, 35210, 35220, 35240, 35250, 35260, 35270, 35280, 35290, 35300, 35310, 35320, 35330, 35340, 35350, 35360, 35420, 35430, 35440, 35450, 35460, 35470, 35480, 35490, 35500, 35510, 35520, 35530, 35540, 35550, 35560, 35570, 35580, 35590, 35600, 35620, 35630, 35640, 35650, 35660, 35670, 35680, 35690, 35700, 35710, 35720, 35740, 35760, 35770, 35780, 35790, 35820, 35830, 35950, 35960, 36000, 36010, 36020, 36030, 36040, 36050, 36060, 36070, 36080, 36090, 36100, 36110, 36130, 36140, 36150, 36160, 36170, 36180, 36190, 36200, 36210, 36220, 36230, 36240, 36250, 36260, 36270, 36280, 36300, 36310, 36320, 36330, 36340, 36350, 36380, 36390, 36400, 36410, 36420, 36430, 36440, 36450, 36460, 36470, 36480, 36490, 36500, 36510, 36520, 36530, 36560, 36570, 36580, 36590, 36600, 36610, 36620, 36630, 36640, 36650, 36670, 36680, 36690, 36700, 36710, 36720, 36730, 36740, 36750, 36760, 36770, 36780, 36790, 36800, 36810, 36820, 36830, 36840, 36850, 36860, 36880, 36900, 36910, 36920, 36930, 36940, 36950, 37120, 37160, 37170, 39000, 39040, 39050, 39060, 39070, 39080, 39090, 39170, 39190, 39200, 39210, 39220, 39230, 39240, 39260, 39310, 39330, 39370, 39380, 39430, 40000, 40010, 40020, 40030, 40040, 40050, 40060, 40070, 40080, 40090, 40100, 40110, 40120, 40250, 40260, 40270, 40280, 40290, 40300, 40310, 40320, 40330, 40350, 40360, 40370, 40390, 40400, 40410, 40420, 40440, 40450, 40460, 40470, 40480, 40490, 40500, 40510, 40520, 40530, 40540, 40550, 40560, 40570, 40580, 40590, 40600, 40610, 40620, 40630, 40640, 40650, 40660, 40670, 40680, 40690, 40700, 40710, 40720, 40730, 40740, 40750, 40760, 40770, 40780, 40790, 40800, 40810, 40820, 40830, 40840, 42060, 42080, 42090, 42100, 42160, 43000, 43010, 43020, 43120, 43130, 43140, 43150, 43160, 43170, 43180, 43190, 43200, 43210, 43220, 43230, 43240, 43250, 43260, 43280, 43290, 43300, 43310, 43320, 43330, 43340, 43350, 43410, 43420, 43430, 43440, 43450, 43570, 43580, 43590, 43600, 43610, 43620, 43630, 43640, 43650, 43660, 43670, 43680, 43690, 43700, 43730, 43740, 43750, 43760, 43770, 43780, 43790, 43800, 43810, 43820, 43830, 43840, 43850, 43890, 43900, 44100, 44130, 44140, 44150, 44160, 44170, 44340, 44350, 45000, 45010, 45020, 45030, 45040, 45050, 45060, 45070, 45080, 45090, 45100, 45110, 45120, 45130, 45140, 45150, 45160, 45220, 45230, 45240, 46000, 46010, 46020, 46030, 46040, 46050, 46060, 46070, 46080, 46090, 46100, 46110, 46140, 46150, 46160, 46170, 46180, 46190, 46200, 46210, 46220, 46230, 46240, 46310, 46320, 46330, 46340, 46350, 46360, 46370, 46380, 46390, 46400, 46410, 46420, 46430, 46440, 46450, 46460, 46470, 46480, 46490, 46500, 46510, 46520, 46530, 46560, 46570, 46590, 46600, 46610, 46620, 46630, 46640, 46670, 46680, 47170, 47260, 48820, 48830],
    female: [30960, 30970, 30980, 30990, 31000, 31010, 31020, 31030, 31040, 31050, 31060, 31070, 31080, 31090, 31100, 31110, 31120, 31130, 31140, 31150, 31160, 31170, 31180, 31190, 31200, 31210, 31220, 31230, 31240, 31250, 31260, 31270, 31280, 31290, 31300, 31310, 31320, 31330, 31340, 31350, 31360, 31380, 31400, 31410, 31420, 31430, 31440, 31450, 31460, 31470, 31480, 31490, 31510, 31520, 31530, 31540, 31550, 31560, 31570, 31580, 31590, 31600, 31610, 31620, 31630, 31640, 31650, 31660, 31670, 31680, 31690, 31700, 31710, 31720, 31730, 31740, 31750, 31760, 31770, 31780, 31790, 31800, 31810, 31820, 31830, 31840, 31850, 31860, 31870, 31880, 31890, 31910, 31920, 31930, 31940, 31950, 31960, 31970, 31980, 31990, 32050, 32150, 32160, 32310, 32320, 32330, 32340, 32350, 32360, 32530, 32540, 32550, 32560, 32650, 32660, 32720, 32730, 32740, 32750, 32760, 33010, 33020, 33030, 33160, 33680, 34010, 34020, 34030, 34040, 34050, 34060, 34070, 34080, 34090, 34100, 34110, 34120, 34130, 34140, 34150, 34160, 34170, 34180, 34190, 34200, 34210, 34220, 34230, 34240, 34250, 34260, 34270, 34280, 34290, 34300, 34310, 34320, 34330, 34340, 34350, 34360, 34370, 34380, 34390, 34400, 34410, 34420, 34430, 34440, 34450, 34470, 34480, 34490, 34510, 34540, 34560, 34580, 34590, 34600, 34610, 34620, 34630, 34640, 34650, 34660, 34670, 34680, 34690, 34700, 34710, 34720, 34730, 34740, 34750, 34760, 34770, 34780, 34790, 34800, 34810, 34820, 34830, 34840, 34850, 34860, 34870, 34880, 34890, 34900, 34910, 34940, 34950, 34960, 34970, 34980, 35000, 35010, 35020, 35030, 35040, 35050, 35060, 35070, 35080, 35090, 35100, 35110, 35120, 36980, 36990, 37000, 37010, 37020, 37030, 37040, 37050, 37060, 37070, 37080, 37090, 37100, 37110, 37130, 37140, 37150, 37190, 37200, 37210, 37220, 37230, 37240, 37250, 37260, 37270, 37280, 37290, 37300, 37310, 37320, 37330, 37340, 37350, 37360, 37370, 37380, 37400, 37420, 37440, 37450, 37460, 37470, 37490, 37500, 37510, 37520, 37530, 37560, 37570, 37580, 37590, 37600, 37610, 37620, 37630, 37640, 37650, 37660, 37670, 37680, 37690, 37700, 37710, 37720, 37730, 37740, 37750, 37760, 37770, 37780, 37790, 37800, 37810, 37820, 37830, 37840, 37850, 37860, 37880, 37900, 37910, 37920, 37930, 37940, 37950, 37960, 37970, 37980, 37990, 38000, 38010, 38020, 38030, 38040, 38050, 38060, 38070, 38080, 38090, 38100, 38110, 38120, 38130, 38140, 38150, 38160, 38180, 38240, 38250, 38260, 38270, 38280, 38290, 38300, 38310, 38320, 38330, 38350, 38380, 38390, 38400, 38410, 38420, 38430, 38440, 38450, 38460, 38470, 38480, 38490, 38500, 38510, 38520, 38540, 38550, 38560, 38570, 38580, 38590, 38600, 38610, 38620, 38630, 38640, 38650, 38660, 38670, 38680, 38690, 38700, 38710, 38730, 38740, 38750, 38760, 38770, 38780, 38790, 38800, 38810, 38820, 38830, 38840, 38860, 38880, 38890, 38900, 38910, 38920, 38930, 38940, 39250, 39320, 39340, 39350, 39360, 39390, 39400, 39410, 39420, 39440, 40850, 40860, 40870, 40880, 40890, 40900, 40910, 40920, 40930, 40940, 40950, 40960, 40970, 40980, 41060, 41070, 41080, 41090, 41100, 41110, 41120, 41140, 41150, 41160, 41170, 41180, 41190, 41200, 41220, 41340, 41350, 41360, 41370, 41380, 41390, 41400, 41410, 41420, 41440, 41460, 41470, 41480, 41490, 41510, 41520, 41530, 41560, 41570, 41580, 41590, 41600, 41610, 41620, 41630, 41640, 41650, 41660, 41670, 41680, 41690, 41700, 41710, 41720, 41730, 41740, 41750, 41760, 41770, 41780, 41790, 41800, 41810, 41820, 41830, 41840, 41850, 41860, 41870, 41880, 41890, 41900, 41910, 41920, 41930, 41940, 41950, 41960, 41970, 41980, 41990, 42070, 42110, 42120, 42150, 43270, 43860, 43870, 43880, 43910, 43980, 44000, 44010, 44020, 44030, 44040, 44050, 44060, 44070, 44080, 44090, 44110, 44120, 44180, 44190, 44200, 44290, 44300, 44310, 44320, 44330, 44360, 44370, 44380, 44390, 44400, 44410, 44420, 44430, 44440, 44450, 44460, 44470, 44480, 44490, 44500, 44510, 44520, 44530, 44590, 44600, 44610, 44620, 44630, 44640, 44650, 44770, 44780, 44790, 44800, 44810, 44820, 44830, 44840, 44850, 44870, 44880, 44890, 44900, 44910, 44920, 44930, 44940, 44950, 44980, 44990, 47000, 47010, 47020, 47030, 47040, 47050, 47060, 47070, 47080, 47090, 47100, 47110, 47120, 47130, 47140, 47150, 47160, 47270, 47280, 47290, 47300, 47310, 47320, 47330, 47340, 47350, 47360, 47370, 47380, 47390, 47400, 47410, 47420, 47430, 47440, 47450, 47460, 47520, 47530, 47540, 48000, 48010, 48020, 48030, 48040, 48050, 48060, 48070, 48080, 48090, 48100, 48130, 48140, 48150, 48160, 48170, 48180, 48190, 48200, 48210, 48220, 48230, 48320, 48330, 48340, 48350, 48360, 48370, 48380, 48390, 48400, 48410, 48430, 48440, 48450, 48460, 48470, 48480, 48490, 48500, 48510, 48520, 48530, 48540, 48550, 48560, 48570, 48580, 48590, 48600, 48610, 48620, 48630, 48640, 48650, 48660, 48700, 48710, 48730, 48740, 48750, 48760, 48770, 48790, 48800],
    colors: []
};
var faces = {
    male: [20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20009, 20010, 20011, 20012, 20013, 20014, 20015, 20016, 20017, 20018, 20019, 20020, 20021, 20022, 20023, 20024, 20025, 20026, 20027, 20028, 20029, 20030, 20031, 20032, 20033, 20035, 20036, 20037, 20038, 20039, 20040, 20042, 20043, 20044, 20045, 20046, 20047, 20048, 20049, 20050, 20051, 20052, 20053, 20054, 20055, 20056, 20057, 20058, 20059, 20060, 20061, 20062, 20063, 20064, 20065, 20066, 20067, 20068, 20069, 20070, 20071, 20072, 20073, 20074, 20075, 20076, 20077, 20078, 20079, 20080, 20081, 20082, 20083, 20084, 20085, 20086, 20087, 20088, 20089, 20090, 20091, 20092, 20093, 20094, 20095, 20096, 20097, 20098, 20099],
    female: [21000, 21001, 21002, 21003, 21004, 21005, 21006, 21007, 21008, 21009, 21010, 21011, 21012, 21013, 21014, 21015, 21016, 21017, 21018, 21019, 21020, 21021, 21022, 21023, 21024, 21025, 21026, 21027, 21028, 21029, 21030, 21031, 21033, 21034, 21035, 21036, 21037, 21038, 21041, 21042, 21043, 21044, 21045, 21046, 21047, 21048, 21049, 21050, 21051, 21052, 21053, 21054, 21055, 21056, 21057, 21058, 21059, 21060, 21061, 21062, 21063, 21064, 21065, 21066, 21067, 21068, 21069, 21070, 21071, 21072, 21073, 21074, 21075, 21076, 21077, 21078, 21079, 21080, 21081, 21082, 21083, 21084, 21085, 21086, 21087, 21088, 21089, 21090, 21091, 21092, 21093, 21094, 21095, 21096, 21097, 21098],
    colors: []
};

hairs.male = split(hairs.male);
hairs.female = split(hairs.female);
faces.male = split(faces.male);
faces.female = split(faces.female);

function start() {
    status = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    selection = selection & 0xFF;

    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 0) {
        if (status == 4) {
            // post hair selection, declined to hair color
            cm.dispose();
            return;
        }
        status--;
        if (--status == 0) {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status == 1) {
        // reset section selection
        this.sect = null;
        this.takeCoupon = true;
        cm.sendSimple("Hello, #b#h ##k! I'm the #bDonator Stylist NPC#k.\r\n"
            + "\r\nIf you have a #b#i5150044# #t5150044##k, I can change your style however you wish! Here's what I have currently available#b"
            + "\r\n#L0#Skin#l"
            + "\r\n#L1#Haircuts#l \t #L2#Hair Color#l"
            + "\r\n#L3#Eyes#l \t\t\t#L4#Eye Color#l"
        );
    } else if (status == 2) {
        if (this.takeCoupon && !cm.haveItem(5150044)) {
            cm.sendOk("Sorry, you don't have the #i5150044##t5150044##l.\r\nPlease purchase this from the #bCash Shop#k.");
            return cm.dispose();
        }
        if (this.sect == null)
            this.sect = selection;
        if (this.sect == 0) { // skin section
            if (skins.length == 0) {
                cm.sendOk("Sorry but there are no skins available to pick from right now!");
                cm.dispose();
            } else {
                cm.sendStyle("", skins);
            }
        } else if (this.sect == 1 || this.sect == 3) { // hair or eyes section
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
                    text += "#r#L" + (100 + i) + "#Female " + (this.sect == 1 ? "Hairs" : "Eyes") + "#l#k";
                    if (mlength > 0)
                        text += "\t";
                    else
                        text += "\r\n";
                }
                if (mlength > 0) {
                    // formatting only for trans; where both sections need to be shown
                    if (cm.getPlayer().getGender() == 2 && flength == 0)
                        text += "\t\t\t\t\t\t\t\t  ";
                    text += "#b#L" + (200 + i) + "#Male " + (this.sect == 1 ? "Hairs" : "Eyes") + " page " + (i + 1) + "#l#k\r\n";
                }
                if (flength > 0)
                    flength--;
                if (mlength > 0)
                    mlength--;
            }

            cm.sendSimple(text);
        } else if (this.sect == 2 || this.sect == 4) { // hair color or eye color section
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
            var arr = (this.sect == 2 ? hairs.colors : faces.colors);
            cm.sendStyle(java.lang.String.format("%d colors to select from!\r\nPick your favorite", arr.length), arr);
        }
    } else if (status == 3) {
        if (this.sect == 0) { // skin selection
            if (selection >= 0 && selection < skins.length) {
                cm.setSkin(skins[selection]);
                cm.gainItem(5150044, -1, true);
            }
            cm.dispose();
        } else if (this.sect == 1 || this.sect == 3) { // hair or eyes selection
            this.gender = (selection >= 200 ? (this.sect == 1 ? hairs.male : faces.male) : (this.sect == 1 ? hairs.female : faces.female));
            this.index = selection >= 200 ? (selection % 200) : (selection % 100);

            var arr = this.gender[selection % (Math.floor(selection / 100) * 100)];
            cm.sendStyle(java.lang.String.format("%d styles to select from!\r\nPick your favorite", arr.length), arr);
        } else if (this.sect == 2 || this.sect == 4) { // hair color or eye color selection
            if (this.sect == 2) {
                cm.setHair(hairs.colors[selection]);
                if (this.takeCoupon) cm.gainItem(5150044, -1, true);
            } else if (this.sect == 4) {
                cm.setFace(faces.colors[selection]);
                if (this.takeCoupon) cm.gainItem(5150044, -1, true);
            }
            cm.dispose();
        }
    } else if (status == 4) { // update player style
        if (this.sect == 1) {
            cm.setHair(this.gender[this.index][selection]);
            cm.sendYesNo("Would you like to change your hair color?");
            cm.gainItem(5150044, -1, true);
            this.takeCoupon = false;
        } else if (this.sect == 3) {
            cm.setFace(this.gender[this.index][selection]);
            cm.sendYesNo("Would you like to change your eye color?");
            cm.gainItem(5150044, -1, true);
            this.takeCoupon = false;
        }
    } else if (status == 5) {
        if (this.sect == 1 || this.sect == 3) {
            this.sect += 1; // change to color selection
            status = 1;
            action(1, 0, 0);
        } else cm.dispose();
    }
}

function split(array) {
    var out = new Array();
    while (array.length > 0) {
        var capacity = Math.min(255, array.length);
        out.push(array.splice(0, capacity));
    }
    return out;
}