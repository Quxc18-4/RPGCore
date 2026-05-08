package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class WorldTreeFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§2§lMầm Hoa Nở Rộ"; }

    @Override public double getDamageMultiplier() { return 1.50; }

    @Override

    public List<String> getLore(double baseDmgPct) {
        double bloomDmg = baseDmgPct * getDamageMultiplier();
        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Mầm Hoa Nở Rộ]",
                "§f- Triệu hồi một Mầm Hoa Khổng Lồ đâm chồi trong §a2 giây§f.",
                "§f- Sau khi nở, duy trì trong §a5 giây§f, mỗi giây bắn §a5-8 đạn Phấn Hoa§f.",
                "§f- Phấn hoa nổ diện rộng, gây §c" + (int)bloomDmg + "% §fSát thương",
                "§f  và tự động kích hoạt phản ứng §2§lSINH SÔI§f.",
                "",
                "§b[Nội tại - Phấn Hoa]§f",
                "§f- Kẻ địch mang Ấn Phấn Hoa sẽ giảm 90% sát thương nhận vào",
                "§f  từ các vụ nổ Phấn hoa tiếp theo trong 5 giây."
        );
    }

    // ==========================================
    // MÁY QUÉT ĐỊA HÌNH: TÌM MẶT ĐẤT CỨNG TUYỆT ĐỐI
    // ==========================================
    private int getSolidFloorY(Location loc) {
        // Quét thẳng từ vị trí ngắm (cộng thêm 2 block để trừ hao nếp gấp địa hình) xuống dưới đáy thế giới
        for (int y = loc.getBlockY() + 2; y >= loc.getWorld().getMinHeight(); y--) {
            Material type = loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType();
            if (type.isSolid()) {
                return y; // Trả về tọa độ Y của khối cứng (Đất, Đá, Gỗ...) đầu tiên chạm phải
            }
        }
        return loc.getWorld().getHighestBlockYAt(loc);
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {

        // 1. TIA NGẮM XUYÊN CỎ (Dùng RayTrace của World thay vì getTargetBlock)
        org.bukkit.util.RayTraceResult rayResult = caster.getWorld().rayTraceBlocks(
                caster.getEyeLocation(),
                caster.getLocation().getDirection(),
                30.0,
                org.bukkit.FluidCollisionMode.NEVER,
                true // [QUAN TRỌNG]: true = Xuyên qua hoàn toàn cỏ, hoa, mạng nhện...
        );

        Location targetLoc;
        if (rayResult != null && rayResult.getHitPosition() != null) {
            targetLoc = new Location(caster.getWorld(), rayResult.getHitPosition().getX(), rayResult.getHitPosition().getY(), rayResult.getHitPosition().getZ());
        } else {
            targetLoc = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(30));
        }

        // 2. ÉP XUỐNG SÀN NHÀ (Bám đất tuyệt đối)
        int solidY = getSolidFloorY(targetLoc);

        // Căn giữa tâm Block (X + 0.5, Z + 0.5) và đặt ngay trên mặt khối cứng (Y + 1.0)
        Location spawnLoc = new Location(caster.getWorld(), targetLoc.getBlockX() + 0.5, solidY + 1.0, targetLoc.getBlockZ() + 0.5);

        spawnLoc.setYaw(0f);
        spawnLoc.setPitch(0f);

        double totalDmgMulti = baseMultiplier * getDamageMultiplier();
        String pollenMarkKey = "Pollen_Mark_" + caster.getUniqueId();

        // 3. Sinh thực thể và Chạy Animation
        BlockDisplay magicFlower = summonMagicFlower(spawnLoc);
        runFlowerTask(caster, rpgPlayer, spawnLoc, magicFlower, totalDmgMulti, pollenMarkKey);
    }

    private void runFlowerTask(Player caster, RPGPlayer rpgPlayer, Location center, BlockDisplay display, double dmgMulti, String markKey) {
        new BukkitRunnable() {
            int ticks = 0;
            int firingSeconds = 0;
            final float targetScale = 3.0f; // Scale khổng lồ x3
            final Random random = new Random();

            @Override
            public void run() {
                if (!caster.isOnline() || display.isDead()) {
                    display.remove();
                    this.cancel();
                    return;
                }

                // ==========================================
                // GIAI ĐOẠN 1: MỌC HOA CHUẨN TOÁN HỌC TỪ TÂM (COPY TỪ NẤM)
                // ==========================================
                if (ticks < 40) {
                    if (ticks % 5 == 0) {
                        center.getWorld().spawnParticle(Particle.BLOCK, center, 10, 0.5, 0.1, 0.5, 0.1, org.bukkit.Bukkit.createBlockData(Material.DIRT));
                    }

                    // Phóng to dần trong 30 ticks đầu tiên
                    if (ticks < 30) {
                        float currentScale = Math.min(targetScale, (ticks + 1) * (targetScale / 30.0f));
                        Transformation transform = display.getTransformation();
                        transform.getScale().set(currentScale, currentScale, currentScale);
                        // Công thức Căn giữa Tuyệt đối của Nấm Ký Sinh
                        transform.getTranslation().set(-currentScale / 2.0f, 0f, -currentScale / 2.0f);
                        display.setTransformation(transform);
                    }

                    ticks++;
                    return;
                }

                // ==========================================
                // GIAI ĐOẠN 2: BẮN SÚNG CỐI MỖI GIÂY (Chạy 5 lần)
                // ==========================================
                if (ticks % 20 == 0) {
                    if (firingSeconds >= 5) {
                        despawnMagicFlower(display, center); // Chuyển sang Hoạt ảnh thu nhỏ
                        this.cancel();
                        return;
                    }

                    int mortarCount = 5 + random.nextInt(4); // 5 -> 8 viên
                    center.getWorld().playSound(center, Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.5f, 0.5f);

                    Location flowerTop = center.clone().add(0, 1.5, 0);

                    for (int i = 0; i < mortarCount; i++) {
                        double r = 1.0 + random.nextDouble() * 9.0;
                        double angle = random.nextDouble() * 2 * Math.PI;
                        Location dropLoc = center.clone().add(r * Math.cos(angle), 0, r * Math.sin(angle));

                        int highestY = dropLoc.getWorld().getHighestBlockYAt(dropLoc);
                        if (Math.abs(highestY - center.getBlockY()) < 8) {
                            dropLoc.setY(highestY + 0.5);
                        } else {
                            dropLoc.setY(center.getBlockY());
                        }

                        executeSporeMortar(caster, rpgPlayer, flowerTop, dropLoc, dmgMulti, markKey);
                    }
                    firingSeconds++;
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L); // Chạy mỗi 1 tick để render animation
    }

    private void executeSporeMortar(Player caster, RPGPlayer rpgPlayer, Location startLoc, Location endLoc, double dmgMulti, String markKey) {
        new BukkitRunnable() {
            double t = 0.0;
            final double speed = 0.03 + (Math.random() * 0.02);

            @Override
            public void run() {
                t += speed;

                double currentX = startLoc.getX() + (endLoc.getX() - startLoc.getX()) * t;
                double currentZ = startLoc.getZ() + (endLoc.getZ() - startLoc.getZ()) * t;
                double currentY = startLoc.getY() + (endLoc.getY() - startLoc.getY()) * t + Math.sin(t * Math.PI) * 5.0;

                Location current = new Location(startLoc.getWorld(), currentX, currentY, currentZ);
                current.getWorld().spawnParticle(Particle.COMPOSTER, current, 2, 0.1, 0.1, 0.1, 0);

                for (Entity e : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                        triggerSporeExplosion(caster, rpgPlayer, current, dmgMulti, markKey, 1.0);
                        this.cancel();
                        return;
                    }
                }

                if (t >= 1.0) {
                    triggerSporeExplosion(caster, rpgPlayer, endLoc, dmgMulti, markKey, 0.8);
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void triggerSporeExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, double dmgMulti, String markKey, double markChance) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.COMPOSTER, loc, 30, 1.5, 0.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 0.5, 0), 15, 1.5, 0.5, 1.5, 0.0);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 2.0, 1.5)) {
            if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                LivingEntity target = (LivingEntity) e;

                double appliedMulti = dmgMulti;
                boolean alreadyMarked = target.hasMetadata(markKey);

                if (alreadyMarked) {
                    appliedMulti = dmgMulti * 0.10;
                } else {
                    ReactionEngine.applyMarkOnly(target, MageElement.NATURE);
                    ReactionEngine.ReactionResult res = ReactionEngine.processHit(caster, target, MageElement.WATER);
                    appliedMulti = dmgMulti * (res != null ? res.damageMultiplier : 1.0);

                    if (Math.random() <= markChance) {
                        target.setMetadata(markKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);

                        new BukkitRunnable() {
                            @Override public void run() { if (target.isValid()) target.removeMetadata(markKey, RPGCore.getInstance()); }
                        }.runTaskLater(RPGCore.getInstance(), 100L);
                    }
                }

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, appliedMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }

    // ==========================================
    // KHỞI TẠO HOA LÚC BAN ĐẦU (Scale = 0, Spawn Consumer)
    // ==========================================
    private BlockDisplay summonMagicFlower(Location center) {
        // [CẢI TIẾN]: Sử dụng spawn Consumer giống hệt Nấm Ký Sinh
        // Điều này đảm bảo Entity có Scale = 0 NGAY TRƯỚC KHI xuất hiện trên client, loại bỏ giật cục.
        return center.getWorld().spawn(center, BlockDisplay.class, display -> {
            display.setBlock(org.bukkit.Bukkit.createBlockData(Material.CACTUS_FLOWER));
            Transformation transform = display.getTransformation();
            transform.getScale().set(0f, 0f, 0f);
            display.setTransformation(transform);
        });
    }

    // ==========================================
    // HOẠT ẢNH THU LẠI VÀ BIẾN MẤT (Toán học đảo ngược)
    // ==========================================
    private void despawnMagicFlower(BlockDisplay bd, Location center) {
        center.getWorld().playSound(center, Sound.BLOCK_CHERRY_LEAVES_BREAK, 1.0f, 0.8f);
        center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 0.5, 0), 40, 1.5, 1.0, 1.5, 0.1);

        new BukkitRunnable() {
            int shrinkTicks = 0;
            final float startScale = 3.0f;

            @Override
            public void run() {
                if (shrinkTicks >= 20 || bd.isDead()) {
                    bd.remove();
                    this.cancel();
                    return;
                }

                // Từ từ thu nhỏ về 0
                float currentScale = Math.max(0f, startScale - ((shrinkTicks + 1) * (startScale / 20.0f)));

                Transformation t = bd.getTransformation();
                t.getScale().set(currentScale, currentScale, currentScale);
                // Liên tục giữ tâm căn giữa trong lúc thu nhỏ
                t.getTranslation().set(-currentScale / 2.0f, 0f, -currentScale / 2.0f);
                bd.setTransformation(t);

                shrinkTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}