package com.dan.achievementstats;

import net.minecraft.util.Identifier;

public class Stat {

    private String stat;
    private float value;


    public Stat(String stat, float value) {
        this.stat = stat;
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public Identifier getStat() {
        return new Identifier("", stat);
    }
}
