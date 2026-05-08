package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BurningBramblesFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§6§lSự Tái Sinh"; }

    @Override public double getDamageMultiplier() { return 1.50; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Sự Tái Sinh]",
                "§f- Triệu hồi một Đại Trận Dây Leo khổng lồ xung quanh bản thân.",
                "",
                "§a[Nội tại - Xum Xuê]§f",
                "§f- Trong §a2 giây§f đầu, kẻ địch đi xuyên qua dây leo",
                "§f  sẽ bị làm chậm §e60% tốc độ di chuyển§f.",
                "",
                "§c[Nội tại - Bùng Cháy]§f",
                "§f- Sau 2s, dây leo bùng cháy dữ dội. Kẻ địch chạm vào:",
                "§f- Sát thương lần đầu: §c" + (int)totalDmg + "%§f Sát Thương,",
                "§f  Tự động kích hoạt phản ứng §6§lTHIÊU RỤI§f.",
                "§f- Sát thương duy trì: §c" + (int)(totalDmg * 0.1) + "%§f mỗi giây.",
                "",
                "§b[Nội tại - Đột Biến]§f",
                "§f- Xác suất có §b10%§f dây leo mang §bLửa Xanh§f,",
                "§f  gây thêm §c30%§f sát thương cho §cBùng Cháy§f."
        );
    }

    private static class VinePillar {
        List<BlockDisplay> blocks = new ArrayList<>();
        Location baseLoc;
        boolean isBlueFire;

        public VinePillar(Location baseLoc, boolean isBlueFire) {
            this.baseLoc = baseLoc;
            this.isBlueFire = isBlueFire;
        }
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location center = caster.getLocation();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();
        String burnMarkKey = "Brambles_Burn_" + caster.getUniqueId();

        List<VinePillar> vinePillars = summonDistributed3DVines(center);
        runBramblesTask(caster, rpgPlayer, center, vinePillars, totalDmgMulti, burnMarkKey);
    }

    private void runBramblesTask(Player caster, RPGPlayer rpgPlayer, Location center, List<VinePillar> vinePillars, double dmgMulti, String markKey) {
        new BukkitRunnable() {
            int ticks = 0;
            final double outerR = 15.0;

            @Override
            public void run() {
                // TỔNG THỜI GIAN: 120 ticks = 6 giây (2s Slow + 4s Cháy)
                if (!caster.isOnline() || ticks > 120) {
                    clearVines(vinePillars, center);
                    this.cancel();
                    return;
                }

                if (ticks % 5 == 0) {
                    List<Entity> nearby = (List<Entity>) center.getWorld().getNearbyEntities(center, outerR, 5, outerR);

                    for (Entity e : nearby) {
                        if (!(e instanceof LivingEntity) || e.equals(caster) || !CombatManager.canTarget(caster, e)) continue;
                        LivingEntity target = (LivingEntity) e;

                        VinePillar touchedVine = getTouchedVine(target.getLocation(), vinePillars);

                        if (touchedVine != null) {
                            // GIAI ĐOẠN 1: LÀM CHẬM 2 GIÂY ĐẦU (tick 0 -> 39)
                            if (ticks < 40) {
                                StatusEffectManager.applySlow(target, 10, 0.60, 3);
                            }
                            // GIAI ĐOẠN 2: BÙNG CHÁY (TỪ GIÂY THỨ 2 TRỞ ĐI)
                            else {
                                applyBurstAndIgnite(caster, rpgPlayer, target, dmgMulti, touchedVine.isBlueFire, markKey);
                            }
                        }
                    }
                }

                // Giây thứ 2 (Tick 40) bắt đầu bùng lửa
                if (ticks == 40) {
                    center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
                    center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.5f, 1.0f);
                }

                if (ticks > 40) {
                    for (VinePillar pillar : vinePillars) {
                        if (Math.random() < 0.4) {
                            Particle pType = pillar.isBlueFire ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
                            double yOffset = 0.5 + Math.random() * 3.5;
                            pillar.baseLoc.getWorld().spawnParticle(pType, pillar.baseLoc.clone().add(0, yOffset, 0), 2, 0.2, 0.2, 0.2, 0.02);

                            if (Math.random() < 0.1) {
                                pillar.baseLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pillar.baseLoc.clone().add(0, yOffset, 0), 1, 0.1, 0.1, 0.1, 0.02);
                            }
                        }
                    }
                    if (ticks % 20 == 0) {
                        center.getWorld().playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // HITBOX ĐƯỢC CHỈNH VỪA KHÍT VỚI DÂY LEO (Bán kính 0.7 blocks)
    private VinePillar getTouchedVine(Location entityLoc, List<VinePillar> pillars) {
        for (VinePillar pillar : pillars) {
            double dx = entityLoc.getX() - pillar.baseLoc.getX();
            double dz = entityLoc.getZ() - pillar.baseLoc.getZ();
            // 0.6 * 0.6 = 0.36 -> Vừa khít khoảng cách chạm người vào dây leo
            if (dx * dx + dz * dz <= 0.36) {
                return pillar;
            }
        }
        return null;
    }

    // XỬ LÝ SÁT THƯƠNG VÀ REACTION
    private void applyBurstAndIgnite(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double baseMulti, boolean isBlueFire, String markKey) {
        long currentTime = System.currentTimeMillis();

        // 1. NẾU LÀ LẦN ĐẦU CHẠM LỬA (Chưa có Mark) -> 100% Burst Damage & Kích Reaction
        if (!target.hasMetadata(markKey)) {
            target.setMetadata(markKey, new FixedMetadataValue(RPGCore.getInstance(), true));

            // Thiết lập Cooldown (1000ms) cho sát thương đốt duy trì
            target.setMetadata(markKey + "_Cooldown", new FixedMetadataValue(RPGCore.getInstance(), currentTime + 1000L));

            // Sát thương 100% (Hoặc 130% nếu là Lục Hỏa)
            double appliedMulti = isBlueFire ? baseMulti * 1.30 : baseMulti;
            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, appliedMulti, 0, true, false);
            CombatManager.applySkillDamage(caster, target, hitDmg);

            // Tự động gọi ReactionEngine (Đánh Mộc rồi Đánh Lửa -> THIÊU RỤI)
            ReactionEngine.applyMarkOnly(target, MageElement.NATURE);
            ReactionEngine.processHit(caster, target, MageElement.FIRE);

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.2f);
        }
        // 2. NẾU ĐÃ CHÁY, VÀ VẪN ĐỨNG TRONG DÂY LEO -> Chịu 10% mỗi giây (1000ms)
        else {
            long cooldown = target.hasMetadata(markKey + "_Cooldown") ? target.getMetadata(markKey + "_Cooldown").get(0).asLong() : 0L;
            if (currentTime >= cooldown) {
                // Cập nhật lại Cooldown 1 giây
                target.setMetadata(markKey + "_Cooldown", new FixedMetadataValue(RPGCore.getInstance(), currentTime + 1000L));

                // Gây 10% Sát thương (Cấu rỉa máu từ từ)
                double appliedMulti = isBlueFire ? (baseMulti * 0.10) * 1.30 : (baseMulti * 0.10);
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, appliedMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }

    private List<VinePillar> summonDistributed3DVines(Location center) {
        List<VinePillar> pillars = new ArrayList<>();
        Random random = new Random();

        // TĂNG SỐ LƯỢNG THÊM 15 DÂY LEO (Từ 57 lên 72 trụ)
        double[] rings = {5.0, 9.5, 14.0};
        int[] counts = {13, 23, 36}; // 13 + 23 + 36 = 72

        for (int rIndex = 0; rIndex < rings.length; rIndex++) {
            double currentR = rings[rIndex];
            int count = counts[rIndex];

            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                double jitterR = (random.nextDouble() - 0.5) * 2.0;
                double jitterAngle = (random.nextDouble() - 0.5) * 0.3;

                double finalR = currentR + jitterR;
                double finalAngle = angle + jitterAngle;

                double x = finalR * Math.cos(finalAngle);
                double z = finalR * Math.sin(finalAngle);

                Location baseLoc = center.clone().add(x, 0, z);
                int floorY = getSolidFloorY(baseLoc);
                Location centerBase = new Location(center.getWorld(), baseLoc.getBlockX() + 0.5, floorY, baseLoc.getBlockZ() + 0.5);

                boolean isBlueFire = random.nextDouble() <= 0.10;
                VinePillar pillar = new VinePillar(centerBase, isBlueFire);

                for (int h = 0; h < 5; h++) {
                    Location blockLoc = centerBase.clone().add(0, h, 0);

                    BlockDisplay vine = center.getWorld().spawn(blockLoc, BlockDisplay.class, display -> {
                        display.setBlock(Bukkit.createBlockData(Material.TWISTING_VINES_PLANT));
                        Transformation trans = display.getTransformation();
                        trans.getScale().set(0f, 0f, 0f);
                        display.setTransformation(trans);
                    });

                    new BukkitRunnable() {
                        @Override public void run() {
                            vine.setInterpolationDuration(10);
                            Transformation t = vine.getTransformation();
                            t.getScale().set(1f, 1f, 1f);
                            t.getTranslation().set(-0.5f, 0f, -0.5f);
                            vine.setTransformation(t);
                        }
                    }.runTaskLater(RPGCore.getInstance(), 1L + (h * 3L));

                    pillar.blocks.add(vine);
                }
                pillars.add(pillar);
            }
        }
        center.getWorld().playSound(center, Sound.BLOCK_VINE_PLACE, 2.0f, 0.5f);
        return pillars;
    }

    private int getSolidFloorY(Location loc) {
        for (int y = loc.getBlockY() + 3; y >= loc.getBlockY() - 5; y--) {
            if (loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType().isSolid()) return y;
        }
        return loc.getBlockY();
    }

    // CƠ CHẾ RÚT DÂY LEO XUỐNG MƯỢT MÀ TỪNG TICK (Giống Nấm Ký Sinh)
    private void clearVines(List<VinePillar> pillars, Location center) {
        center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);

        new BukkitRunnable() {
            int shrinkTicks = 0;
            final float startScale = 1.0f;

            @Override
            public void run() {
                // Sau 15 ticks (0.75s) thì thu xong và xóa Entity
                if (shrinkTicks >= 15) {
                    for (VinePillar pillar : pillars) {
                        for (BlockDisplay v : pillar.blocks) {
                            if (v != null && v.isValid()) v.remove();
                        }
                    }
                    this.cancel();
                    return;
                }

                // Tính toán tỷ lệ thu nhỏ (Từ 1.0 -> 0)
                float currentScale = startScale * (1.0f - (shrinkTicks / 15.0f));

                for (VinePillar pillar : pillars) {
                    for (BlockDisplay v : pillar.blocks) {
                        if (v != null && v.isValid()) {
                            Transformation t = v.getTransformation();
                            t.getScale().set(currentScale, currentScale, currentScale);
                            // Giữ cho phần đáy không bị nhấc bổng lên bằng cách chia nửa
                            t.getTranslation().set(-currentScale / 2.0f, 0f, -currentScale / 2.0f);
                            v.setTransformation(t);
                        }
                    }
                }
                shrinkTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}