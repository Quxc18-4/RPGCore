package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Particle;

public enum MageElement {
    FIRE("§cLửa", org.bukkit.Particle.FLAME),
    WATER("§bNước", Particle.FALLING_WATER),
    WIND("§aGió", org.bukkit.Particle.CLOUD),
    NATURE("§2Thiên Nhiên", Particle.HAPPY_VILLAGER),
    LIGHT("§eÁnh Sáng", org.bukkit.Particle.END_ROD),
    DARK("§8Bóng Tối", Particle.LARGE_SMOKE);

    private final String displayName;
    private final org.bukkit.Particle particle;

    MageElement(String displayName, org.bukkit.Particle particle) {
        this.displayName = displayName;
        this.particle = particle;
    }

    public String getDisplayName() { return displayName; }
    public org.bukkit.Particle getParticle() { return particle; }
}