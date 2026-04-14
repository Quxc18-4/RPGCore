package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class FireAttack implements IMageBasicAttack {
    private final Random random = new Random();

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        // ==========================================
        // [CƠ CHẾ CƯỜNG HÓA]: HỎA CẦU NỔ LAN (DAMAGE SPLIT)
        // ==========================================
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            player.removeMetadata("Mage_Focus_Level", plugin);
            player.removeMetadata("Active_Weapon_Enhancement", plugin);

            // Sát thương tổng (Ví dụ: Lv10 = 335% -> 3.35)
            double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);

            player.getWorld().playSound(startLoc, Sound.ENTITY_GHAST_SHOOT, 1.2f, 0.8f); // Tiếng nổ trầm

            // Tạo nhân Magma Block to
            org.bukkit.entity.ItemDisplay coreDisplay = null;
            try {
                coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                    display.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
                    display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);
                    display.setTeleportDuration(1);
                    org.bukkit.util.Transformation transform = display.getTransformation();
                    transform.getScale().set(0.8f, 0.8f, 0.8f); // To gấp đôi bình thường
                    display.setTransformation(transform);
                });
            } catch (Exception ignored) {}
            final org.bukkit.entity.ItemDisplay finalCore = coreDisplay;

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = startLoc.clone();

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40) { explode(currentLoc, totalFocusMulti, player, rpgPlayer); this.cancel(); return; }
                    currentLoc.add(direction);

                    if (finalCore != null && finalCore.isValid()) {
                        Location displayLoc = currentLoc.clone();
                        displayLoc.setYaw(displayLoc.getYaw() + (ticks * 20));
                        displayLoc.setPitch(displayLoc.getPitch() + (ticks * 20));
                        finalCore.teleport(displayLoc);
                    }

                    // Trail Lửa rực rỡ
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 10, 0.4, 0.4, 0.4, 0.05);
                    currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc, 2, 0.2, 0.2, 0.2, 0);

                    // Chạm đất hoặc Chạm Quái -> Nổ
                    if (currentLoc.getBlock().getType().isSolid()) {
                        explode(currentLoc, totalFocusMulti, player, rpgPlayer);
                        this.cancel();
                        return;
                    }
                    for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            explode(currentLoc, totalFocusMulti, player, rpgPlayer);
                            this.cancel();
                            return;
                        }
                    }
                }

                @Override
                public synchronized void cancel() {
                    super.cancel();
                    if (finalCore != null && finalCore.isValid()) finalCore.remove();
                }
            }.runTaskTimer(plugin, 0L, 1L);

            DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
            return;
        }

        // ==========================================
        // ĐÒN ĐÁNH THƯỜNG CŨ GIỮ NGUYÊN
        // ==========================================
        player.getWorld().playSound(startLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.5f);

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
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 6, 0.2, 0.2, 0.2, 0.02);
                    currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                    currentLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                    currentLoc.getWorld().spawnParticle(Particle.EXPLOSION, currentLoc, 1, 0, 0, 0, 0);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                        LivingEntity target = (LivingEntity) ent;
                        this.cancel();

                        if (!hasDamagedWeapon) {
                            DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
                            hasDamagedWeapon = true;
                        }

                        if (random.nextDouble() <= 0.25) target.setFireTicks(60);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1, 0, 0, 0, 0);

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.9, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.FIRE);
                        player.removeMetadata("Ghost_L_Blocker", plugin);
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Hàm xử lý vụ nổ diện rộng của Hỏa Cầu Cường Hóa
    private void explode(Location hitLoc, double totalDmgMulti, Player player, RPGPlayer rpgPlayer) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
        hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 50, 2.0, 2.0, 2.0, 0.1);

        // Lấy toàn bộ mục tiêu trong bán kính 4 Blocks
        List<LivingEntity> validTargets = new ArrayList<>();
        for (org.bukkit.entity.Entity ent : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                validTargets.add((LivingEntity) ent);
            }
        }

        if (validTargets.isEmpty()) return;

        // Cơ chế Sát thương chia sẻ (Damage Split) - Thấp nhất 30% tổng Dmg
        double dmgPerTarget = Math.max(totalDmgMulti / validTargets.size(), totalDmgMulti * 0.30);

        for (LivingEntity target : validTargets) {
            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.FIRE);
            double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
            CombatManager.applySkillDamage(player, target, finalDmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            target.setFireTicks(100); // Áp cháy mặc định 5s
        }
    }
}