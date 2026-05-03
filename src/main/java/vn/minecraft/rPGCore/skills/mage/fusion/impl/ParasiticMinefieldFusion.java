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
    @Override public String getReactionName() { return "§2§lBào Tử Ký Sinh"; }
    @Override public double getDamageMultiplier() { return 1.15; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double mineDmg = totalDmg * 0.40;
        double tickDmg = totalDmg * 0.05;
        double explodeDmg = totalDmg * 0.45;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Lãnh Địa Ký Sinh]",
                "§f- Phân tán 5-8 Nấm Biến Dị vào khu vực ngắm tới.",
                "§f- Kẻ địch đạp trúng nấm chịu §c" + (int)mineDmg + "% §fSát thương kỹ năng,",
                "§f  và bị tiêm §2Bào Tử Ký Sinh§f rút §c" + (int)tickDmg + "% §fSát thương/giây.",
                "",
                "§e[Nội tại - Lây Nhiễm]",
                "§f- Quái mang Ký Sinh chết nổ tung, gây §c" + (int)explodeDmg + "% §fSát thương",
                "§f  kỹ năng diện rộng và sinh ra 2 Nấm Con.",
                "§f- §7(Nấm Con chỉ mang 70% sát thương và không thể sinh sôi)."
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
            // Sửa lỗi kẹt trần nhà khi aim vào khoảng không
            Integer floorY = getFloorY(targetLoc, caster.getLocation().getBlockY());
            if(floorY != null) {
                targetLoc.setY(floorY);
            }
        }

        Location center = targetLoc.add(0.5, 1.0, 0.5);

        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(center, Sound.BLOCK_SPORE_BLOSSOM_FALL, 1.5f, 0.5f);
        caster.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 80, 4.0, 1.0, 4.0, 0);

        int initialMines = 5 + random.nextInt(4);
        scatterMines(caster, rpgPlayer, center, initialMines, totalDmgMulti, 1);
    }

    // ==========================================
    // TÌM SÀN THÔNG MINH (INDOOR TERRAIN HUGGING)
    // ==========================================
    private Integer getFloorY(Location currentLoc, int referenceY) {
        for (int y = referenceY + 3; y >= referenceY - 5; y--) {
            Location checkLoc = currentLoc.clone();
            checkLoc.setY(y);
            if (checkLoc.getBlock().getType().isSolid()) {
                return y;
            }
        }
        return null;
    }

    // ==========================================
    // TRIỆU HỒI NẤM (CÓ QUẢN LÝ ĐỜI F1/F2)
    // ==========================================
    private void scatterMines(Player caster, RPGPlayer rpgPlayer, Location center, int count, double baseTotalMulti, int generation) {
        double genScaling = (generation == 1) ? 1.0 : 0.70;
        double currentGenTotalMulti = baseTotalMulti * genScaling;

        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 8.0;
            double offsetZ = (random.nextDouble() - 0.5) * 8.0;
            Location mineLoc = center.clone().add(offsetX, 0, offsetZ);

            // Sửa lỗi tìm block Y khi sinh mìn
            Integer floorY = getFloorY(mineLoc, center.getBlockY());
            if (floorY == null) continue; // Bỏ qua nếu là khoảng không/hố sâu

            mineLoc.setY(floorY + 1.0);

            org.bukkit.entity.BlockDisplay mushroom = mineLoc.getWorld().spawn(mineLoc, org.bukkit.entity.BlockDisplay.class, display -> {
                display.setBlock(org.bukkit.Bukkit.createBlockData(Material.WARPED_FUNGUS));
                Transformation transform = display.getTransformation();
                transform.getScale().set(0f, 0f, 0f);
                display.setTransformation(transform);
            });

            new BukkitRunnable() {
                int lifetime = 0;
                final float targetScale = 1.1f;

                @Override
                public void run() {
                    if (lifetime >= 200 || !caster.isOnline() || mushroom.isDead()) {
                        mushroom.remove();
                        this.cancel();
                        return;
                    }

                    if (lifetime < 10) {
                        float currentScale = Math.min(targetScale, (lifetime + 1) * 0.15f);
                        Transformation transform = mushroom.getTransformation();
                        transform.getScale().set(currentScale, currentScale, currentScale);
                        transform.getTranslation().set(-currentScale / 2.0f, 0f, -currentScale / 2.0f);
                        mushroom.setTransformation(transform);
                    }

                    if (lifetime % 10 == 0) {
                        mineLoc.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, mineLoc.clone().add(0, 0.6, 0), 2, 0.4, 0.4, 0.4, 0);
                    }

                    boolean triggered = false;
                    for (Entity entity : mineLoc.getWorld().getNearbyEntities(mineLoc, 1.2, 1.5, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            triggerMineExplosion(caster, rpgPlayer, mineLoc, target, currentGenTotalMulti, generation, baseTotalMulti);
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
    // KÝ SINH & NỔ XÁC (AOE)
    // ==========================================
    private void applyParasite(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double currentGenTotalMulti, int generation, double baseTotalMulti) {
        double tickDmgMulti = currentGenTotalMulti * 0.05;
        String metaKey = "Parasite_Gen_" + generation;

        if (target.hasMetadata("Parasite_Gen_1") || target.hasMetadata("Parasite_Gen_2")) return;
        target.setMetadata(metaKey, new FixedMetadataValue(RPGCore.getInstance(), true));

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
                    deathLoc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, deathLoc.clone().add(0, 1, 0), 50, 1.5, 1.5, 1.5, 0);

                    double explosionDmgMulti = currentGenTotalMulti * 0.45;
                    for (Entity e : deathLoc.getWorld().getNearbyEntities(deathLoc, 3.0, 3.0, 3.0)) {
                        if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) e, explosionDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) e, hitDmg);
                        }
                    }

                    if (generation == 1) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                scatterMines(caster, rpgPlayer, deathLoc, 2, baseTotalMulti, 2);
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
    // NỔ MÌN (SÁT THƯƠNG ĐƠN MỤC TIÊU LÊN KẺ ĐẠP TRÚNG)
    // ==========================================
    private void triggerMineExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, LivingEntity triggerEntity, double currentGenTotalMulti, int generation, double baseTotalMulti) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 30, 1.0, 1.0, 1.0, 0);

        double singleDmgMulti = currentGenTotalMulti * 0.40;
        double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, triggerEntity, singleDmgMulti, 0, true, false);
        CombatManager.applySkillDamage(caster, triggerEntity, hitDmg);

        applyParasite(caster, rpgPlayer, triggerEntity, currentGenTotalMulti, generation, baseTotalMulti);
    }
}