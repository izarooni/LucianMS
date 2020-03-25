package com.lucianms.client.meta;

public class Union {

    private final Union union;
    private int rank;

    public Union(Union union) {
        this.union = union;
        this.rank = 1;
    }

    public enum Type {
        Kirin(1);

        private int rank;

        Type(int rank) {
            this.rank = rank;
        }

        public int getRank() {
            return rank;
        }


    }

}
