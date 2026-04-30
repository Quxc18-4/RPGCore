package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class BlackFireFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.DARK; }

    @Override public String getReactionName() { return "§5§lHẮC HỎA"; }
    @Override public double getDamageMultiplier() { return 1.0; }

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        target.getWorld().playSound(hitLocation, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.5f);

        // [EXECUTE MECHANIC]: Đốt máu theo % Máu đã mất trong 4 giây (Mỗi giây 1 lần)[cite: 5]
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Tự động hủy Task để tránh rò rỉ bộ nhớ (Memory Leak) nếu quái chết hoặc offline[cite: 5]
                if (ticks >= 4 || target.isDead() || !target.isValid() || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);
                target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);

                // Tính toán Máu đã mất[cite: 5]
                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHealth = target.getHealth();
                double missingHealth = maxHealth - currentHealth;

                // Công thức: 5% máu đã mất[cite: 5]
                double blackFireDmg = missingHealth * 0.05;
                blackFireDmg = Math.min(blackFireDmg, 350.0); // Khóa (Cap) sát thương để chống oneshot Boss[cite: 5]
                blackFireDmg = Math.max(blackFireDmg, 10.0);

                // Sử dụng CombatManager lõi để áp dụng sát thương Kỹ năng[cite: 5]
                CombatManager.applySkillDamage(caster, target, blackFireDmg);
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L); // 20 ticks = 1 giây[cite: 5]
    }
}