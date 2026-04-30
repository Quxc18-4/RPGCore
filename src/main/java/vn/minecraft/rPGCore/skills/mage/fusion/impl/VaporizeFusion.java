package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class VaporizeFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.WATER; }

    @Override public String getReactionName() { return "§c§lBỐC HƠI"; }
    @Override public double getDamageMultiplier() { return 1.50; } // Khuếch đại 150%

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        // [VFX & SFX]: Hiệu ứng khói nước bốc hơi
        target.getWorld().spawnParticle(Particle.CLOUD, hitLocation.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        target.getWorld().playSound(hitLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
    }
}