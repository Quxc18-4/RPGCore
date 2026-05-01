package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ParasiticMinefieldFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.NATURE; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§2§lLãnh Địa Ký Sinh"; }
    @Override public double getDamageMultiplier() { return 1.30; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double mineDmg = totalDmg * 0.30;
        double tickDmg = totalDmg * 0.05;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Lãnh Địa Ký Sinh]",
                "§f- Phân tán 5-8 Nấm Biến Dị (F1) vào khu vực ngắm tới.",
                "§f- Đạp trúng nấm gây nổ §c§l" + (int)mineDmg + "% §fSát thương,",
                "§f  và gieo rắc §2Bào Tử Ký Sinh§f rút §c§l" + (int)tickDmg + "% §fMáu/giây.",
                "§f- §c[Lây Nhiễm]§f: Kẻ địch F1 chết sẽ nổ xác đẻ thêm Nấm F2.",
                "§f  (Nấm F2 có thể lây nhiễm nhưng không thể sinh sôi thêm)."
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
        }

        targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc));
        Location center = targetLoc.add(0.5, 1.0, 0.5);

        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        // Trả lại đám mây bào tử ma mị, không gây chói mắt
        caster.getWorld().playSound(center, Sound.BLOCK_SPORE_BLOSSOM_FALL, 1.5f, 0.5f);
        caster.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 80, 4.0, 1.0, 4.0, 0);

        int initialMines = 5 + random.nextInt(4); // 5-8 nấm
        // Gen 1 (F1): Có thể sinh đẻ tiếp
        scatterMines(caster, rpgPlayer, center, initialMines, totalDmgMulti, 1);
    }

    // ==========================================
    // HÀM TRIỆU HỒI NẤM (CÓ HIỆU ỨNG MỌC TỪ ĐẤT)
    // ==========================================
    private void scatterMines(Player caster, RPGPlayer rpgPlayer, Location center, int count, double totalDmgMulti, int generation) {
        double mineDmgMulti = totalDmgMulti * 0.30;

        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 8.0;
            double offsetZ = (random.nextDouble() - 0.5) * 8.0;
            Location mineLoc = center.clone().add(offsetX, 0, offsetZ);

            mineLoc.setY(mineLoc.getWorld().getHighestBlockYAt(mineLoc) + 1.0);

            // Triệu hồi với kích thước ban đầu = 0
            org.bukkit.entity.BlockDisplay mushroom = mineLoc.getWorld().spawn(mineLoc, org.bukkit.entity.BlockDisplay.class, display -> {
                display.setBlock(org.bukkit.Bukkit.createBlockData(Material.WARPED_FUNGUS));
                org.bukkit.util.Transformation transform = display.getTransformation();
                transform.getScale().set(0f, 0f, 0f);
                display.setTransformation(transform);
            });

            new BukkitRunnable() {
                int lifetime = 0;
                final float targetScale = 1.1f; // Đã thu nhỏ lại theo ý Giám đốc

                @Override
                public void run() {
                    if (lifetime >= 200 || !caster.isOnline() || mushroom.isDead()) {
                        mushroom.remove();
                        this.cancel();
                        return;
                    }

                    // [VFX ANIMATION]: Hiệu ứng mọc nấm dần dần (Lerping) trong 8-10 ticks đầu
                    if (lifetime < 10) {
                        float currentScale = Math.min(targetScale, (lifetime + 1) * 0.15f); // Tăng dần scale

                        org.bukkit.util.Transformation transform = mushroom.getTransformation();
                        transform.getScale().set(currentScale, currentScale, currentScale);
                        // Canh giữa mượt mà theo từng frame mọc
                        transform.getTranslation().set(-currentScale / 2.0f, 0f, -currentScale / 2.0f);
                        mushroom.setTransformation(transform);
                    }

                    // Tỏa sương mù bào tử
                    if (lifetime % 10 == 0) {
                        mineLoc.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, mineLoc.clone().add(0, 0.6, 0), 2, 0.4, 0.4, 0.4, 0);
                    }

                    boolean triggered = false;
                    for (Entity entity : mineLoc.getWorld().getNearbyEntities(mineLoc, 1.2, 1.5, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            triggerMineExplosion(caster, rpgPlayer, mineLoc, target, totalDmgMulti, mineDmgMulti, generation);
                            triggered = true;
                            break;
                        }
                    }

                    if (triggered) {
                        mushroom.remove();
                        this.cancel();
                    }
                    lifetime++;
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
        }
    }


    // ==========================================
    // DEBUFF KÝ SINH (FIX NHỊP ĐIỆU LÂY NHIỄM)
    // ==========================================
    private void applyParasite(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double totalDmgMulti, int generation) {
        double tickDmgMulti = totalDmgMulti * 0.05;
        String metaKey = "Parasite_Gen_" + generation;

        if (target.hasMetadata("Parasite_Gen_1") || target.hasMetadata("Parasite_Gen_2")) return;
        target.setMetadata(metaKey, new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 3 || !caster.isOnline()) {
                    target.removeMetadata(metaKey, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                if (target.isDead()) {
                    Location deathLoc = target.getLocation();
                    deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.5f);
                    // Bùng nổ đám mây bào tử to
                    deathLoc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, deathLoc.clone().add(0, 1, 0), 50, 1.5, 1.5, 1.5, 0);

                    double explosionDmgMulti = totalDmgMulti * 0.30;
                    for (Entity e : deathLoc.getWorld().getNearbyEntities(deathLoc, 3.0, 3.0, 3.0)) {
                        if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) e, explosionDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) e, hitDmg);
                        }
                    }

                    if (generation == 1) {
                        // [VFX TIMING]: Chờ 8 ticks (0.4s) để "mây bào tử" bay xuống đất rồi mới đẻ nấm
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                scatterMines(caster, rpgPlayer, deathLoc, 2, totalDmgMulti, 2);
                            }
                        }.runTaskLater(RPGCore.getInstance(), 8L);
                    }

                    target.removeMetadata(metaKey, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, target.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0);
                double tickHitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, tickDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, tickHitDmg);

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L);
    }

    // ==========================================
    // NỔ MÌN VÀ GẮN KÝ SINH THEO GEN (ĐỜI)
    // ==========================================
    private void triggerMineExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, LivingEntity triggerEntity, double totalDmgMulti, double mineDmgMulti, int generation) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 30, 1.0, 1.0, 1.0, 0); // Vụ nổ phấn hoa êm ái

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, mineDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);

                if (target.equals(triggerEntity)) {
                    applyParasite(caster, rpgPlayer, target, totalDmgMulti, generation);
                }
            }
        }
    }
}