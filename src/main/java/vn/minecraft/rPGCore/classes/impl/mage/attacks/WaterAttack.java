package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WaterAttack implements IMageBasicAttack {

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();

        // ==========================================
        // [CƠ CHẾ CƯỜNG HÓA]: THỦY ĐAO (BOOMERANG)
        // ==========================================
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            player.removeMetadata("Mage_Focus_Level", plugin);
            player.removeMetadata("Active_Weapon_Enhancement", plugin);

            double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
            double damagePerHit = totalFocusMulti * 0.33; // 33% sức mạnh mỗi lần quẹt

            player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.8f); // Tiếng đầm hơn

            Vector outDirection = startLoc.getDirection().normalize().multiply(1.5); // Tốc độ vừa phải để dễ lái

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = startLoc.clone();
                Set<UUID> hitSet = new HashSet<>(); // Lưu quái đã dính đòn để giới hạn 3 mục tiêu
                boolean isReturning = false;

                @Override
                public void run() {
                    ticks++;

                    // Nếu đã quay về và chạm vào người chơi (bán kính 1.5) -> Thu hồi
                    if (isReturning && currentLoc.distanceSquared(player.getLocation().add(0,1,0)) < 2.25) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1.5f);
                        this.cancel();
                        return;
                    }

                    // Bay ra 25 tick (~37 blocks) thì quay đầu
                    if (!isReturning && ticks > 25) {
                        isReturning = true;
                        hitSet.clear(); // Reset mục tiêu để chém lại lượt về
                    }

                    // Hủy nếu bay quá lâu (kẹt)
                    if (ticks > 80) { this.cancel(); return; }

                    // Tính hướng bay: Đang ra thì theo dir cũ, đang về thì Homing vào Player
                    Vector travelDir;
                    if (isReturning) {
                        travelDir = player.getLocation().add(0, 1, 0).toVector().subtract(currentLoc.toVector()).normalize().multiply(1.5);
                    } else {
                        travelDir = outDirection;
                    }

                    currentLoc.add(travelDir);

                    // Vẽ Boomerang nước siêu to khổng lồ
                    Vector rightVec = travelDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                    if (rightVec.lengthSquared() < 0.001) rightVec.setX(1);

                    for (double d = -1.5; d <= 1.5; d += 0.3) {
                        double backOffset = Math.abs(d) * 0.8;
                        Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(travelDir.clone().normalize().multiply(backOffset));
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 1, 0, 0, 0, 0);
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                    }
                    // Tâm boomerang
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 5, 0.2, 0.2, 0.2, 0);

                    // Va chạm địa hình (Chỉ hủy nếu đang bay ra. Bay về đâm tường kệ nó)
                    if (!isReturning && currentLoc.getBlock().getType().isSolid()) {
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
                        currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 15, 0.5, 0.5, 0.5, 0.1);
                        isReturning = true; // Đâm tường -> bật ngược lại luôn
                        hitSet.clear();
                    }

                    // Va chạm quái (Chém 3 mục tiêu mỗi lượt)
                    if (hitSet.size() < 3) {
                        for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                                LivingEntity target = (LivingEntity) ent;
                                hitSet.add(target.getUniqueId());

                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                                target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 30, 1, 1, 1, 0.1);

                                // Gây sát thương
                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                                double finalDmgMulti = damagePerHit * reaction.damageMultiplier;

                                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);
                                player.removeMetadata("Ghost_L_Blocker", plugin);

                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                                if (hitSet.size() >= 3) break; // Cắt sớm nếu đủ 3
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);

            DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
            return;
        }

        // ==========================================
        // ĐÒN ĐÁNH THƯỜNG CŨ GIỮ NGUYÊN
        // ==========================================
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);
        Vector rightVec = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightVec.lengthSquared() < 0.001) rightVec.setX(1);

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            boolean hasDamagedWeapon = false;
            Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 34) { this.cancel(); return; }
                currentLoc.add(direction);

                if (ticks > 2) {
                    for (double d = -0.8; d <= 0.8; d += 0.2) {
                        double backOffset = Math.abs(d) * 0.8;
                        Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(direction.clone().normalize().multiply(backOffset));
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 1, 0, 0, 0, 0);
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                        LivingEntity target = (LivingEntity) ent;
                        hitSet.add(target.getUniqueId());

                        if (hitSet.size() >= 5) { this.cancel(); }

                        if (!hasDamagedWeapon) {
                            DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
                            hasDamagedWeapon = true;
                        }

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 20, 1, 0.5, 1, 0.1);

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.4, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.WATER);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (hitSet.size() >= 5) return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}