package server.maps;

/**
 * @author AngelSL
 * @author izarooni
 */
public enum FieldLimit {

    UNABLE_TO_JUMP(0x01),
    UNABLE_TO_USE_SKILLS(0x02),
    UNABLE_TO_SUMMON_DOOR(0x03),
    SUMMON(0x04),
    DOOR(0x08),
    CHANGECHANNEL(0x10),
    CANNOTVIPROCK(0x40),
    CANNOTMINIGAME(0x80),
    //NoClue1(0x100), // APQ and a couple quest maps have this
    CANNOTUSEMOUNTS(0x200),
    //NoClue2(0x400), // Monster carnival?
    //NoClue3(0x800), // Monster carnival?
    CANNOTUSEPOTION(0x1000),
    //NoClue4(0x2000), // No notes
    //Unused(0x4000),
    //NoClue5(0x8000), // Ariant colosseum-related?
    //NoClue6(0x10000), // No notes
    CANNOTJUMPDOWN(0x20000);
    //NoClue7(0x40000); // Seems to .. disable Rush if 0x2 is set
    private final long i;

    FieldLimit(long i) {
        this.i = i;
    }

    public long getValue() {
        return i;
    }

    public boolean check(int fieldLimit) {
        return ((fieldLimit >> i) & 1) != 1;
    }

    public int add(int fieldLimit, FieldLimit flag) {
        if (check(fieldLimit)) {
            return fieldLimit;
        }
        return fieldLimit | (1 << flag.getValue());
    }
}
