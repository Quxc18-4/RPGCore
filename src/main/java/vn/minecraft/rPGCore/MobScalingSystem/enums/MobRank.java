package vn.minecraft.rPGCore.MobScalingSystem.enums;

public enum MobRank {
    NORMAL(1.0, 1.0),
    ELITE(10.0, 2.5),
    MINIBOSS(20.0, 4.0),
    REGION_BOSS(120.0, 8.0),
    WORLD_BOSS(600.0, 15.0);

    public final double hp, dmg;
    MobRank(double hp, double dmg) {
        this.hp = hp; this.dmg = dmg;
    }
}