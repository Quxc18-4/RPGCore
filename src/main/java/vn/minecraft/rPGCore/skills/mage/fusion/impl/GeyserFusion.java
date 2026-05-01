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
    @Override public double getDamageMultiplier() { return 1.8; } // Đã thu Thuế Khống Chế

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double waveDmg = totalDmg / 2.0;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Hải Thần Nộ]",
                "§f- Triệu hồi 5-7 luồng mạch nước ngầm sục sôi liên tiếp",
                "§f  lan tỏa trong khu vực rộng xung quanh tâm ngắm.",
                "§f- Cột nước bùng nổ, §bHất tung§f kẻ địch lên không trung.",
                "§f- Mỗi cột nước gây §c§l" + (int)waveDmg + "% §fSát thương kỹ năng.",
                "§f  §7(Kẻ địch bị tung hứng nhiều lần nhận 10% Sát thương)."
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
        triggerGeyserField(caster, rpgPlayer, center, totalDmgMulti);
    }

    private void triggerGeyserField(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        caster.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.5f);

        double singleGeyserDmg = totalDmgMulti / 2.0;
        int geyserCount = 5 + random.nextInt(3); // Cần sinh 5 đến 7 cột
        String hitMetaKey = "GeyserHit_" + caster.getUniqueId();

        // 1. [THUẬT TOÁN TỐI ƯU]: Rejection Sampling (Sinh tọa độ không trùng lặp)
        List<Location> validLocations = new ArrayList<>();
        double minDistance = 3.5; // Khoảng cách tối thiểu giữa 2 tâm cột nước
        double minDistanceSquared = minDistance * minDistance; // Bình phương để tối ưu CPU

        for (int i = 0; i < geyserCount; i++) {
            // Giới hạn 50 lần thử (Failsafe) để tránh Infinity Loop nếu Random quá xui
            for (int attempt = 0; attempt < 50; attempt++) {
                // Random góc và khoảng cách (Bán kính vùng cast là 6 block)
                double angle = random.nextDouble() * 2 * Math.PI;
                double r = Math.sqrt(random.nextDouble()) * 6.0; // Căn bậc 2 để phân bố đều đặn

                Location potentialLoc = center.clone().add(r * Math.cos(angle), 0, r * Math.sin(angle));
                potentialLoc.setY(potentialLoc.getWorld().getHighestBlockYAt(potentialLoc) + 1.0);

                // Quét xem có nằm quá gần cột nào đã được chốt không
                boolean isFarEnough = true;
                for (Location validLoc : validLocations) {
                    if (validLoc.distanceSquared(potentialLoc) < minDistanceSquared) {
                        isFarEnough = false;
                        break; // Nằm đè lên nhau -> Hủy vòng lặp kiểm tra
                    }
                }

                if (isFarEnough) {
                    validLocations.add(potentialLoc); // Đủ xa -> Lưu vào danh sách chốt
                    break; // Thoát vòng lặp Failsafe, chuyển sang sinh cột nước tiếp theo
                }
            }
        }

        // Vẽ vùng đất cấm cảnh báo (R=6)
        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f);
        for (double t = 0; t < Math.PI * 2; t += 0.4) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(6 * Math.cos(t), 0.2, 6 * Math.sin(t)), 1, 0, 0, 0, 0, warningDust, true);
        }

        // 2. Kích hoạt chuỗi hiệu ứng dựa trên danh sách tọa độ sạch
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= validLocations.size() || !caster.isOnline()) {
                    this.cancel();
                    return;
                }
                // Triệu hồi cột nước tại tọa độ an toàn đã được tính toán
                spawnSingleGeyser(caster, rpgPlayer, validLocations.get(index), singleGeyserDmg, hitMetaKey);
                index++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 4L); // Stagger 4 ticks (0.2s) tạo nhịp điệu tung hứng
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

                            target.setVelocity(new Vector(0, 1.35, 0)); // Hard CC: Hất tung

                            double finalMultiToApply = dmgMulti;
                            if (target.hasMetadata(hitMetaKey)) {
                                finalMultiToApply = dmgMulti * 0.10; // Đã trúng thì chỉ ăn 10%
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
}