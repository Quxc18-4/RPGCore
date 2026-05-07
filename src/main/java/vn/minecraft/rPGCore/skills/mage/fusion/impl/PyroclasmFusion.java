package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PyroclasmFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.FIRE; }
    @Override public String getReactionName() { return "§c§lMưa Thiên Thạch"; }
    @Override public double getDamageMultiplier() { return 1.45; } // Buff hệ số 1.4[cite: 9]

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double waveDmg = totalDmg / 3.0;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Mưa Thiên Thạch]",
                "§f- Triệu hồi Mưa Thiên Thạch tại vị trí tâm ngắm.",
                "§f- §c90%§f: Giáng 3 đợt thiên thạch, mỗi đợt gây §c" + (int)waveDmg + "% §fSát thương.",
                "§f  §8(Trúng nhiều viên trong cùng 1 đợt chỉ nhận 10% Sát thương).",
                "§f- §c10%§f: Giáng 1 Đại Hỏa Cầu khổng lồ, gây §c" + (int)totalDmg + "% §fSát thương."
        ); 
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        // 1. Raycast tìm vị trí con trỏ chuột (Max 30 blocks)[cite: 9]
        Block targetBlock = caster.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;

        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation();
        } else {
            targetLoc = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(30));
            // [FIX INDOOR]: Tự động tìm sàn nhà thực tế, tránh kéo chiêu lên nóc hang
            Integer floorY = getFloorY(targetLoc, caster.getLocation().getBlockY());
            if (floorY != null) {
                targetLoc.setY(floorY);
            }
        }

        // Tinh chỉnh tâm nổ cao hơn mặt đất 0.5 block để Hitbox chính xác hơn
        Location center = targetLoc.add(0.5, 0.5, 0.5);
        double totalDmgMulti = baseMultiplier * getDamageMultiplier(); 

        // 2. Roll Gacha (RNG)[cite: 9]
        if (random.nextInt(100) < 10) {
            caster.sendMessage("§c§l[!] Ma Lực Đột Biến: ĐẠI HỎA CẦU XUẤT HIỆN!");
            castGiantMeteor(caster, rpgPlayer, center, totalDmgMulti);
        } else {
            castMeteorShower(caster, rpgPlayer, center, totalDmgMulti);
        }
    }

    // ==========================================
    // TÌM SÀN THÔNG MINH (INDOOR TERRAIN HUGGING)
    // ==========================================
    private Integer getFloorY(Location currentLoc, int referenceY) {
        // Quét từ trên xuống: Bắt đầu từ vị trí cao hơn bước trước 3 block (để leo dốc)
        // Dò xuống sâu tối đa 5 block (để rớt hố)
        for (int y = referenceY + 3; y >= referenceY - 5; y--) {
            Location checkLoc = currentLoc.clone();
            checkLoc.setY(y);
            if (checkLoc.getBlock().getType().isSolid()) {
                return y;
            }
        }
        return null;
    }

    // ==========================================================
    // CƠ CHẾ 1: ĐẠI HỎA CẦU (1 Viên to, Nổ to, 100% Sát thương tổng)[cite: 9]
    // ==========================================================
    private void castGiantMeteor(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        // [ĐIỀU CHỈNH ĐỘ CAO]: Giảm từ 18 xuống 8 block
        Location skyLoc = center.clone().add(0, 8, 0);
        Vector fallDir = new Vector(0, -1.0, 0);

        caster.getWorld().playSound(center, Sound.ENTITY_GHAST_WARN, 1.5f, 0.5f);

        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.RED, 2.0f);
        for(double t = 0; t < Math.PI * 2; t+= 0.2) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(6*Math.cos(t), 0.2, 6*Math.sin(t)), 1, 0,0,0, 0, warningDust, true);
        }

        new BukkitRunnable() {
            Location current = skyLoc.clone();
            @Override
            public void run() {
                // Kiểm tra va chạm sàn hoặc dưới Y tâm
                if (current.getY() <= center.getY() || current.getBlock().getType().isSolid()) {
                    current.getWorld().playSound(current, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);
                    current.getWorld().spawnParticle(Particle.EXPLOSION, current, 3);
                    current.getWorld().spawnParticle(Particle.LAVA, current, 50, 4.0, 4.0, 4.0, 0.1);

                    for (Entity entity : current.getWorld().getNearbyEntities(current, 6.0, 6.0, 6.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) entity, totalDmgMulti, 0, true, false); 
                            CombatManager.applySkillDamage(caster, (LivingEntity) entity, hitDmg); 
                            // Thêm hiệu ứng bốc cháy nhẹ
                            entity.setFireTicks(40);
                        }
                    }
                    this.cancel();
                    return;
                }

                current.add(fallDir);
                // [TĂNG VFX]: Làm đuôi lửa dày hơn để bù đắp khoảng cách rơi ngắn
                current.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current, 30, 1.5, 1.5, 1.5, 0.05);
                current.getWorld().spawnParticle(Particle.FLAME, current, 50, 2.5, 2.5, 2.5, 0.05);
                current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 40, 2.0, 2.0, 2.0, 0.1, org.bukkit.Bukkit.createBlockData(Material.MAGMA_BLOCK));
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================================
    // CƠ CHẾ 2: MƯA THIÊN THẠCH (3 Đợt, có cơ chế Penalty tái va chạm)[cite: 9]
    // ==========================================================
    private void castMeteorShower(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        caster.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        double waveDmgMulti = totalDmgMulti / 3.0; 

        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave >= 3 || !caster.isOnline()) { 
                    this.cancel();
                    return;
                }
                spawnMeteorWave(caster, rpgPlayer, center, waveDmgMulti, wave); 
                wave++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 15L);
    }

    private void spawnMeteorWave(Player caster, RPGPlayer rpgPlayer, Location center, double waveDmgMulti, int waveIndex) {
        int meteorCount = 5 + random.nextInt(4); 
        String hitMetaKey = "Pyroclasm_" + waveIndex + "_" + caster.getUniqueId(); 

        for (int i = 0; i < meteorCount; i++) {
            // Random vị trí xuất hiện trên trời (Bán kính 5m)
            double offsetX = (random.nextDouble() - 0.5) * 10;
            double offsetZ = (random.nextDouble() - 0.5) * 10;

            // [ĐIỀU CHỈNH ĐỘ CAO]: Rải từ 6 đến 7.5 block
            Location skyLoc = center.clone().add(offsetX, 6.0 + (random.nextDouble() * 1.5), offsetZ);

            Vector fallDir = new Vector((random.nextDouble() - 0.5)*0.2, -1.0, (random.nextDouble() - 0.5)*0.2); 

            new BukkitRunnable() {
                Location current = skyLoc.clone();
                @Override
                public void run() {
                    if (current.getY() <= center.getY() || current.getBlock().getType().isSolid()) {
                        current.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                        current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1); // Đã sửa tên Particle cho 1.21
                        current.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.5, 0.5, 0.5, 0.1); 

                        for (Entity entity : current.getWorld().getNearbyEntities(current, 2.5, 2.5, 2.5)) { 
                            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                                LivingEntity target = (LivingEntity) entity;

                                // [LOGIC PENALTY]: Check tái va chạm[cite: 9]
                                double finalMultiToApply = waveDmgMulti;
                                if (target.hasMetadata(hitMetaKey)) {
                                    finalMultiToApply = waveDmgMulti * 0.10; 
                                } else {
                                    target.setMetadata(hitMetaKey, new FixedMetadataValue(RPGCore.getInstance(), true)); 
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() { target.removeMetadata(hitMetaKey, RPGCore.getInstance()); }
                                    }.runTaskLater(RPGCore.getInstance(), 20L); 
                                }

                                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalMultiToApply, 0, true, false); 
                                CombatManager.applySkillDamage(caster, target, hitDmg); 
                            }
                        }
                        this.cancel();
                        return;
                    }

                    current.add(fallDir);
                    // [TĂNG VFX]: Đuôi lửa đậm hơn
                    current.getWorld().spawnParticle(Particle.FLAME, current, 10, 0.3, 0.3, 0.3, 0.05);
                    current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 10, 0.4, 0.4, 0.4, 0.1, org.bukkit.Bukkit.createBlockData(Material.MAGMA_BLOCK));
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L); 
        }
    }
}