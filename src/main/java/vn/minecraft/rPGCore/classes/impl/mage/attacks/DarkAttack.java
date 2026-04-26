package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DarkAttack implements IMageBasicAttack {
    private final Random random = new Random();

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {

        // 1. TRẠM ĐIỀU PHỐI TỤ LỰC
        if (player.hasMetadata("Mage_Focus_Level")) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = player.getMetadata("Mage_Focus_Level");
            if (!meta.isEmpty()) { // Check list rỗng
                int focusLevel = meta.get(0).asInt();
                new vn.minecraft.rPGCore.skills.mage.focus.FocusDark().castFocus(player, rpgPlayer, weapon, focusLevel);
                return;
            }
        }

        // 2. LOGIC ĐÁNH THƯỜNG
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        player.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 2.0f);

        org.bukkit.entity.ItemDisplay coreDisplay = null;
        try {
            coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.CRYING_OBSIDIAN));
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
            Location lastLoc = startLoc.clone();
            boolean hasDamagedWeapon = false;
            Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 34) { this.cancel(); return; }

                lastLoc = currentLoc.clone();
                currentLoc.add(direction);

                if (finalCore != null && finalCore.isValid()) {
                    Location displayLoc = currentLoc.clone();
                    displayLoc.setYaw(displayLoc.getYaw() + (ticks * 30));
                    displayLoc.setPitch(displayLoc.getPitch() + (ticks * 30));
                    finalCore.teleport(displayLoc);
                }

                if (ticks > 2) {
                    double dist = lastLoc.distance(currentLoc);
                    Vector step = direction.clone().normalize().multiply(0.5);
                    Location tempLoc = lastLoc.clone();
                    for (double d = 0; d < dist; d += 0.5) {
                        tempLoc.getWorld().spawnParticle(Particle.PORTAL, tempLoc, 1, 0.05, 0.05, 0.05, 0);
                        tempLoc.getWorld().spawnParticle(Particle.SQUID_INK, tempLoc, 1, 0, 0, 0, 0);
                        tempLoc.add(step);
                    }
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_ENDER_PEARL_THROW, 0.8f, 0.5f);
                    currentLoc.getWorld().spawnParticle(Particle.SQUID_INK, currentLoc, 10, 0.2, 0.2, 0.2, 0.05);
                    currentLoc.getWorld().spawnParticle(Particle.PORTAL, currentLoc, 15, 0.2, 0.2, 0.2, 0.1);
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

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 0.5f);
                        if (random.nextDouble() <= 0.25) {
                            StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, 40, 0);
                        }

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.8, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.DARK);
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
}