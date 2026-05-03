package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GeyserFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.WATER; }
    @Override public String getReactionName() { return "§b§lHải Thần Nộ"; }
    @Override public double getDamageMultiplier() { return 1.6; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double waveDmg = totalDmg / 2.0;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Hải Thần Nộ]",
                "§f- Triệu hồi 5-7 luồng mạch nước ngầm sục sôi liên tiếp",
                "§f  lan tỏa trong khu vực rộng xung quanh tâm ngắm.",
                "§f- Cột nước bùng nổ, §bHất tung§f kẻ địch lên không trung.",
                "§f- Mỗi cột nước gây §c" + (int)waveDmg + "% §fSát thương kỹ năng."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Block targetBlock = caster.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;

        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation();
        } else {
            targetLoc = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(30));
            // [FIX LỖI KẸT TRẦN NHÀ]: Dò sàn tự động nếu cast vào không khí
            Integer floorY = getFloorY(targetLoc, caster.getLocation().getBlockY());
            if (floorY != null) {
                targetLoc.setY(floorY);
            }
        }

        Location center = targetLoc.add(0.5, 1.0, 0.5);
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();
        triggerGeyserField(caster, rpgPlayer, center, totalDmgMulti);
    }

    private void triggerGeyserField(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        caster.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.5f);

        double singleGeyserDmg = totalDmgMulti / 2.0;
        int geyserCount = 5 + random.nextInt(3);
        String hitMetaKey = "GeyserHit_" + caster.getUniqueId();

        List<Location> validLocations = new ArrayList<>();
        double minDistance = 3.5;
        double minDistanceSquared = minDistance * minDistance;

        for (int i = 0; i < geyserCount; i++) {
            for (int attempt = 0; attempt < 50; attempt++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double r = Math.sqrt(random.nextDouble()) * 6.0;

                Location potentialLoc = center.clone().add(r * Math.cos(angle), 0, r * Math.sin(angle));

                // [FIX LỖI KẸT TRẦN NHÀ]: Dùng hàm tự chế dò mặt sàn thực tế từ tọa độ tâm
                Integer floorY = getFloorY(potentialLoc, center.getBlockY());
                if (floorY == null) continue; // Nếu không tìm thấy sàn (hố quá sâu), skip và random lại

                potentialLoc.setY(floorY + 1.0); // Nâng lên 1 block để ngang mặt đất

                boolean isFarEnough = true;
                for (Location validLoc : validLocations) {
                    if (validLoc.distanceSquared(potentialLoc) < minDistanceSquared) {
                        isFarEnough = false;
                        break;
                    }
                }

                if (isFarEnough) {
                    validLocations.add(potentialLoc);
                    break;
                }
            }
        }

        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f);
        for (double t = 0; t < Math.PI * 2; t += 0.4) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(6 * Math.cos(t), 0.2, 6 * Math.sin(t)), 1, 0, 0, 0, 0, warningDust, true);
        }

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= validLocations.size() || !caster.isOnline()) {
                    this.cancel();
                    return;
                }
                spawnSingleGeyser(caster, rpgPlayer, validLocations.get(index), singleGeyserDmg, hitMetaKey);
                index++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 4L);
    }

    private void spawnSingleGeyser(Player caster, RPGPlayer rpgPlayer, Location loc, double dmgMulti, String hitMetaKey) {
        Particle.DustOptions blueDust = new Particle.DustOptions(org.bukkit.Color.BLUE, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks < 10) {
                    loc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 10, 1.0, 0.2, 1.0, 0.1);
                    for (double t = 0; t < Math.PI * 2; t += 0.8) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(1.5 * Math.cos(t), 0.1, 1.5 * Math.sin(t)), 1, 0, 0, 0, 0, blueDust, true);
                    }
                }
                else if (ticks == 10) {
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 2.0f, 0.5f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_AGITATED, 1.5f, 1.5f);

                    for (double y = 0; y <= 4.0; y += 0.5) {
                        Location pLoc = loc.clone().add(0, y, 0);
                        loc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 30, 0.8, 0.2, 0.8, 0.1);
                        loc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 10, 0.8, 0.2, 0.8, 0.05);
                    }

                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.0, 3.0, 2.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            target.setVelocity(new Vector(0, 1.35, 0));

                            double finalMultiToApply = dmgMulti;
                            if (target.hasMetadata(hitMetaKey)) {
                                finalMultiToApply = dmgMulti * 0.10;
                            } else {
                                target.setMetadata(hitMetaKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() { target.removeMetadata(hitMetaKey, RPGCore.getInstance()); }
                                }.runTaskLater(RPGCore.getInstance(), 40L);
                            }

                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalMultiToApply, 0, true, false);
                            CombatManager.applySkillDamage(caster, target, hitDmg);
                        }
                    }
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // TÌM SÀN THÔNG MINH (INDOOR TERRAIN HUGGING)
    // ==========================================
    private Integer getFloorY(Location currentLoc, int referenceY) {
        // Quét quanh mốc tâm Y. Quét lên 3 block (đồi gập ghềnh) và rớt xuống tối đa 5 block (hố/bậc thang)
        for (int y = referenceY + 3; y >= referenceY - 5; y--) {
            Location checkLoc = currentLoc.clone();
            checkLoc.setY(y);
            if (checkLoc.getBlock().getType().isSolid()) {
                return y;
            }
        }
        return null;
    }
}