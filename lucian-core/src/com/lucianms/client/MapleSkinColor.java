package com.lucianms.client;

public enum MapleSkinColor {

    // @formatter:off
    NORMAL(0),
      DARK(1),
     BLACK(2),
      PALE(3),
      BLUE(4),
     GREEN(5),
     WHITE(9),
      PINK(10),
     BROWN(11),
 ELF_WHITE(12),
  ELF_GREY(13),
ELF_NORMAL(14);
    // @formatter:on
    final int id;

    MapleSkinColor(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MapleSkinColor getById(int id) {
        for (MapleSkinColor l : MapleSkinColor.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }
}
