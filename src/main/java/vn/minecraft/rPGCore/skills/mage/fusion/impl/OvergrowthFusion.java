package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class OvergrowthFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }

    @Override public String getReactionName() { return "§a§lSINH SÔI"; }
    @Override public double getDamageMultiplier() { return 1.0; } // Sát thương chuẩn 100%, không khuếch đại[cite: 5]

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        // 1. [KHỐNG CHẾ AoE]: Trói chân diện rộng xung quanh mục tiêu[cite: 5]
        for (Entity ent : target.getWorld().getNearbyEntities(hitLocation, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(caster, ent)) {
                // Trói chân 3 giây (60 ticks) bằng hệ thống StatusEffectManager lõi[cite: 5]
                StatusEffectManager.applySlow((LivingEntity) ent, 60, 1.0, 10);
                ent.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ent.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
            }
        }

        // 2. [HỒI PHỤC INSTANT]: Thay vì chờ quái chết sinh giọt sương[cite: 5],
        // Phản ứng Tức thời (Fusion) sẽ hút sinh lực quái nạp thẳng về Mage.
        double maxHp = caster.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healAmount = maxHp * 0.05; // Hồi 5% HP tối đa[cite: 5]

        caster.setHealth(Math.min(maxHp, caster.getHealth() + healAmount));
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);

        caster.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a+ §c" + String.format("%.1f", healAmount) + " ♥ §a(Sinh Sôi)"));
    }
}