package com.lucianms.client.meta;

public class Union {

    private int rank;
    private String name;

    public Union() {
        this.rank = 1;
        this.name = "none";
    }

    public Union(String union) {
        this.rank = 1;
        this.name = union;
    }

    public int getRank() {
        return this.rank;
    }

    public boolean setRank(int rank) {
        this.rank = rank;
        return true;
    }

    public String getName() {
        return this.name;
    }

    public boolean setUnion(String union) {
        this.name = union;
        return true;
    }


}
