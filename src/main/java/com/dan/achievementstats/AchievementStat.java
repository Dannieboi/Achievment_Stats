package com.dan.achievementstats;

public class AchievementStat {

    private Stat health;
    private Stat attacKDamage;
    private Stat speed;
    private Stat miningSpeed;
    private String stat;

    public AchievementStat(String stat) {
        //by default each stat at 0
        this.stat = stat;
        this.health = new Stat("health", 0.0f);
        this.attacKDamage = new Stat("attackdamage", 0.0f);
        this.speed = new Stat("movementspeed", 0.0f);
        this.miningSpeed = new Stat("miningspeed", 0.0f);
    }

    public String getStat() {
        return stat;
    }

    public void setHealth(float health) {
        this.health.setValue(health);
    }

    public void setAttacKDamage(float attacKDamage) {
        this.attacKDamage.setValue(attacKDamage);
    }

    public void setSpeed(float speed) {
        this.speed.setValue(speed);
    }

    public void setMiningSpeed(float miningSpeed) {
        this.miningSpeed.setValue(miningSpeed);
    }

    public Stat getHealth() {
        return health;
    }

    public Stat getAttacKDamage() {
        return attacKDamage;
    }

    public Stat getSpeed() {
        return speed;
    }

    public Stat getMiningSpeed() {
        return miningSpeed;
    }
}
