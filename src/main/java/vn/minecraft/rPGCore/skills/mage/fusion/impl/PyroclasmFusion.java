package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    @Override public String getReactionName() { return "§c§lĐẠI HỎA CẦU"; }
    @Override public double getDamageMultiplier() { return 1.40; } // Buff hệ số 1.4

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double waveDmg = totalDmg / 3.0;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Đại Hỏa Cầu]",
                "§f- Triệu hồi Mưa Thiên Thạch tại vị trí tâm ngắm.",
                "§f- §c90%§f: Giáng 3 đợt thiên thạch, mỗi đợt gây §c§l" + (int)waveDmg + "% §fSát thương.",
                "§f  §7(Trúng nhiều viên trong cùng 1 đợt chỉ nhận 10% Sát thương).",
                "§f- §c10%§f: Giáng 1 Đại Hỏa Cầu khổng lồ, gây §c§l" + (int)totalDmg + "% §fSát thương AoE."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        // 1. Raycast tìm vị trí con trỏ chuột (Max 30 blocks)
        org.bukkit.block.Block targetBlock = caster.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;
        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation();
        } else {
            targetLoc = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(30));
            targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc)); // Đẩy xuống mặt đất
        }

        // Khởi tạo Sát thương tổng (Ví dụ: 5.5 * 1.4 = 7.7 = 770%)
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        // 2. Roll Gacha (RNG)
        if (random.nextInt(100) < 10) {
            // 10% - ĐẠI HỎA CẦU KHỔNG LỒ
            caster.sendMessage("§c§l[!] Ma Lực Đột Biến: ĐẠI HỎA CẦU XUẤT HIỆN!");
            castGiantMeteor(caster, rpgPlayer, targetLoc, totalDmgMulti);
        } else {
            // 90% - MƯA THIÊN THẠCH (3 ĐỢT)
            castMeteorShower(caster, rpgPlayer, targetLoc, totalDmgMulti);
        }
    }

    // ==========================================================
    // CƠ CHẾ 1: ĐẠI HỎA CẦU (1 Viên to, Nổ to, 100% Sát thương tổng)
    // ==========================================================
    private void castGiantMeteor(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        Location skyLoc = center.clone().add(0, 18, 0);
        Vector fallDir = new Vector(0, -1.0, 0);

        caster.getWorld().playSound(center, Sound.ENTITY_GHAST_WARN, 1.5f, 0.5f);

        // Vẽ vòng cảnh báo (Bán kính 6m)
        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.RED, 2.0f);
        for(double t = 0; t < Math.PI * 2; t+= 0.2) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(6*Math.cos(t), 0.2, 6*Math.sin(t)), 1, 0,0,0, 0, warningDust, true);
        }

        new BukkitRunnable() {
            Location current = skyLoc.clone();
            @Override
            public void run() {
                if (current.getY() <= center.getY() || current.getBlock().getType().isSolid()) {
                    current.getWorld().playSound(current, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);
                    current.getWorld().spawnParticle(Particle.EXPLOSION, current, 3);
                    current.getWorld().spawnParticle(Particle.LAVA, current, 50, 4.0, 4.0, 4.0, 0.1);

                    for (Entity entity : current.getWorld().getNearbyEntities(current, 6.0, 6.0, 6.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            // Gây 100% Sát thương tổng (770%)[cite: 1]
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) entity, totalDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) entity, hitDmg);
                        }
                    }
                    this.cancel();
                    return;
                }

                current.add(fallDir);
                current.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current, 20, 1.0, 1.0, 1.0, 0.05);
                current.getWorld().spawnParticle(Particle.FLAME, current, 30, 2.0, 2.0, 2.0, 0.05);
                current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 30, 1.5, 1.5, 1.5, 0.1, org.bukkit.Bukkit.createBlockData(Material.MAGMA_BLOCK));
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================================
    // CƠ CHẾ 2: MƯA THIÊN THẠCH (3 Đợt, có cơ chế Penalty tái va chạm)
    // ==========================================================
    private void castMeteorShower(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        caster.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);

        // Chia sát thương thành 3 phần (Ví dụ: 770% / 3 = 256.6%)
        double waveDmgMulti = totalDmgMulti / 3.0;

        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave >= 3 || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // Gọi 1 đợt thiên thạch (Rơi 5-8 viên ngẫu nhiên quanh tâm 5m)
                spawnMeteorWave(caster, rpgPlayer, center, waveDmgMulti, wave);
                wave++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 15L); // Mỗi đợt cách nhau 15 ticks (0.75s)
    }

    private void spawnMeteorWave(Player caster, RPGPlayer rpgPlayer, Location center, double waveDmgMulti, int waveIndex) {
        int meteorCount = 5 + random.nextInt(4); // Rơi 5 đến 8 viên mỗi đợt
        String hitMetaKey = "Pyroclasm_" + waveIndex + "_" + caster.getUniqueId(); // Key check tái va chạm

        for (int i = 0; i < meteorCount; i++) {
            // Random vị trí xuất hiện trên trời (Bán kính 5m)
            double offsetX = (random.nextDouble() - 0.5) * 10;
            double offsetZ = (random.nextDouble() - 0.5) * 10;
            Location skyLoc = center.clone().add(offsetX, 12 + random.nextInt(4), offsetZ);

            // Random góc rơi hơi nghiêng
            Vector fallDir = new Vector((random.nextDouble() - 0.5)*0.2, -1.0, (random.nextDouble() - 0.5)*0.2);

            new BukkitRunnable() {
                Location current = skyLoc.clone();
                @Override
                public void run() {
                    if (current.getY() <= center.getY() - 1 || current.getBlock().getType().isSolid()) {
                        // NỔ VIÊN THIÊN THẠCH NHỎ
                        current.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                        current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1);
                        current.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.5, 0.5, 0.5, 0.1);

                        for (Entity entity : current.getWorld().getNearbyEntities(current, 2.5, 2.5, 2.5)) {
                            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                                LivingEntity target = (LivingEntity) entity;

                                // [LOGIC PENALTY]: Nếu quái vật đã dính damage của Đợt này rồi
                                double finalMultiToApply = waveDmgMulti;
                                if (target.hasMetadata(hitMetaKey)) {
                                    // Bị giảm sát thương chỉ còn 10%
                                    finalMultiToApply = waveDmgMulti * 0.10;
                                } else {
                                    // Lần đầu dính -> Gắn cờ (Tồn tại 1 giây)
                                    target.setMetadata(hitMetaKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                                    // Tự xóa cờ dọn rác
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() { target.removeMetadata(hitMetaKey, RPGCore.getInstance()); }
                                    }.runTaskLater(RPGCore.getInstance(), 20L);
                                }

                                // [CHUẨN RPGCORE]: Áp dụng Sát thương[cite: 1]
                                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalMultiToApply, 0, true, false);
                                CombatManager.applySkillDamage(caster, target, hitDmg);
                            }
                        }
                        this.cancel();
                        return;
                    }

                    // Vẽ Hạt Lửa mô phỏng thiên thạch nhỏ
                    current.add(fallDir);
                    current.getWorld().spawnParticle(Particle.FLAME, current, 5, 0.2, 0.2, 0.2, 0.05);
                    current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 5, 0.3, 0.3, 0.3, 0.1, org.bukkit.Bukkit.createBlockData(Material.MAGMA_BLOCK));
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
        }
    }
}