load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
const Constants = Java.type('com.lucianms.constants.ServerConstants');
const Relationship = Java.type('com.lucianms.client.Relationship');
// Proof Of Love QuestIDs
const PARENTS_BLESSING = 4031373;
const PRIESTS_PERMISSION = 4031374;

const POL_HENE    = 214;
const POL_KERNING = 215;
const POL_ELLINIA = 216;
const POL_ORBIS   = 217;
const POL_LUDI    = 218;
const POL_PERION  = 219;
const POL_PARENTS = 220;

const POL_VICTORIA = [POL_HENE, POL_KERNING, POL_ELLINIA, POL_PERION];
const POL_Items = [4031367, 4031368, 4031369, 4031372, 4031370, 4031371];
const WED_RINGS = [1112803, 1112806, 1112807, 1112809];
const ENG_BOXES = [2240000, 2240001, 2240002, 2240003];
const ENG_ETC   = [4031357, 4031358, 4031359, 4031360, 4031361, 4031362, 4031363, 4031364, 4031806];
const WED_EFFECT = [
    "#FEffect/ItemEff.img/1112803/1/1#",
    "#FEffect/ItemEff.img/1112806/1/1#",
    "#FEffect/ItemEff.img/1112807/1/4#",
    "#FEffect/ItemEff.img/1112809/1/1#"
];

function GetEngagementBoxFromEtc(itemID) {
    if (itemID == ENG_ETC[0] || itemID == ENG_ETC[1]) return ENG_BOXES[0];
    if (itemID == ENG_ETC[2] || itemID == ENG_ETC[3]) return ENG_BOXES[1];
    if (itemID == ENG_ETC[4] || itemID == ENG_ETC[5]) return ENG_BOXES[2];
    if (itemID == ENG_ETC[6] || itemID == ENG_ETC[7]) return ENG_BOXES[3];    
}

function GetRingFromEtc(itemID) {
    if (itemID == ENG_ETC[0] || itemID == ENG_ETC[1]) return WED_RINGS[0];
    if (itemID == ENG_ETC[2] || itemID == ENG_ETC[3]) return WED_RINGS[1];
    if (itemID == ENG_ETC[4] || itemID == ENG_ETC[5]) return WED_RINGS[2];
    if (itemID == ENG_ETC[6] || itemID == ENG_ETC[7]) return WED_RINGS[3];
}