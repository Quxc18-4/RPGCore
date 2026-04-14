package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class NatureAttack implements IMageBasicAttack {
    private final Random random = new Random();

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        // ==========================================
        // [CƠ CHẾ CƯỜNG HÓA]: BOM HẠT (SEED BOMB)
        // ==========================================
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            player.removeMetadata("Mage_Focus_Level", plugin);
            player.removeMetadata("Active_Weapon_Enhancement", plugin);

            // Tính sát thương (VD: 335% -> 3.35)
            double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);

            player.getWorld().playSound(startLoc, Sound.ENTITY_SLIME_JUMP, 1.0f, 0.8f);

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = startLoc.clone();

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40) { explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer); this.cancel(); return; }
                    currentLoc.add(direction);

                    // [VISUAL]: Hạt giống bằng Dust màu xanh lá bay đi
                    Particle.DustOptions seedColor = new Particle.DustOptions(org.bukkit.Color.LIME, 2.0f);
                    currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 3, 0.1, 0.1, 0.1, seedColor);
                    currentLoc.getWorld().spawnParticle(Particle.COMPOSTER, currentLoc, 1, 0.1, 0.1, 0.1, 0);

                    // Va chạm nổ
                    if (currentLoc.getBlock().getType().isSolid()) {
                        explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer);
                        this.cancel();
                        return;
                    }

                    for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer);
                            this.cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);

            DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
            return;
        }

        // ==========================================
        // ĐÒN ĐÁNH THƯỜNG CŨ
        // ==========================================
        player.getWorld().playSound(startLoc, Sound.ITEM_CROP_PLANT, 0.8f, 0.8f);

        org.bukkit.entity.ItemDisplay coreDisplay = null;
        try {
            coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.MOSS_BLOCK));
                display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);
                display.setTeleportDuration(1);
                org.bukkit.util.Transformation transform = display.getTransformation();
                transform.getScale().set(0.4f, 0.4f, 0.4f);
                display.setTransformation(transform);
            });
        } catch (Exception ignored) {}
        final org.bukkit.entity.ItemDisplay finalCore = coreDisplay;

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

                if (finalCore != null && finalCore.isValid()) {
                    Location displayLoc = currentLoc.clone();
                    displayLoc.setYaw(displayLoc.getYaw() + (ticks * 30));
                    displayLoc.setPitch(displayLoc.getPitch() + (ticks * 30));
                    finalCore.teleport(displayLoc);
                }

                if (ticks > 2) {
                    currentLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_GRASS_BREAK, 0.8f, 0.8f);
                    currentLoc.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 15, 0.3, 0.3, 0.3, 0, org.bukkit.Bukkit.createBlockData(Material.OAK_LEAVES));
                    if (random.nextDouble() <= 0.33) spawnEmergingNatureSludge(currentLoc.clone().add(0, 1, 0), player);
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

                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
                        if (random.nextDouble() <= 0.15) {
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 20, 10);
                            target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0, org.bukkit.Bukkit.createBlockData(Material.OAK_LEAVES));
                        }

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.8, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.NATURE);
                        player.removeMetadata("Ghost_L_Blocker", plugin);
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
    }

    // ==========================================
    // HÀM NỔ BOM HẠT TRÓI CHÂN & CHIA SÁT THƯƠNG
    // ==========================================
    private void explodeSeed(Location hitLoc, double totalDmgMulti, Player player, RPGPlayer rpgPlayer) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GRASS_BREAK, 1.5f, 0.5f);
        hitLoc.getWorld().spawnParticle(Particle.BLOCK, hitLoc, 50, 2.0, 0.5, 2.0, org.bukkit.Bukkit.createBlockData(Material.MOSS_BLOCK));
        hitLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hitLoc, 30, 2.0, 1.0, 2.0, 0);

        List<LivingEntity> validTargets = new ArrayList<>();
        for (org.bukkit.entity.Entity ent : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.5, 2.5, 3.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                validTargets.add((LivingEntity) ent);
            }
        }

        if (validTargets.isEmpty()) return;

        double dmgPerTarget = Math.max(totalDmgMulti / validTargets.size(), totalDmgMulti * 0.30);

        for (LivingEntity target : validTargets) {
            // Trói chân diện rộng 2 giây
            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10);

            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.NATURE);
            double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
            CombatManager.applySkillDamage(player, target, finalDmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
        }
    }

    private void spawnEmergingNatureSludge(Location loc, Player shooter) {
        Location ground = loc.clone();
        while (!ground.getBlock().getType().isSolid() && ground.getY() > ground.getWorld().getMinHeight()) {
            ground.subtract(0, 1, 0);
        }
        ground.add(0, 0.1, 0);

        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 30) { this.cancel(); return; }
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    Location pLoc = ground.clone().add(Math.cos(angle)*r, Math.random() * 0.6, Math.sin(angle)*r);
                    ground.getWorld().spawnParticle(Particle.BLOCK, pLoc, 1, 0, 0.1, 0, 0, org.bukkit.Bukkit.createBlockData(Material.OAK_LEAVES));
                    ground.getWorld().spawnParticle(Particle.COMPOSTER, pLoc, 1, 0, 0.1, 0, 0);
                }

                if (time % 5 == 0) {
                    for (org.bukkit.entity.Entity ent : ground.getWorld().getNearbyEntities(ground, 1.5, 1.5, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) {
                            StatusEffectManager.applyEffect((LivingEntity) ent, PotionEffectType.SLOWNESS, 20, 1);
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}