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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GeyserFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.WATER; }
    @Override public String getReactionName() { return "§b§lHải Thần Nộ"; }
    @Override public double getDamageMultiplier() { return 1.25; } // Thuế Khống Chế

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double waveDmg = totalDmg / 2.0; // Chia sát thương chuẩn cho 2 lần trúng

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

        // Chia sát thương chuẩn cho 2 (Vì khó trúng 3 cột nhỏ cùng lúc)
        double singleGeyserDmg = totalDmgMulti / 2.0;
        int geyserCount = 5 + random.nextInt(3); // 5 đến 7 cột nước
        String hitMetaKey = "GeyserHit_" + caster.getUniqueId();

        // Vẽ một vòng nước cảnh báo rộng (R=6) báo hiệu "Vùng đất cấm"
        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f);
        for (double t = 0; t < Math.PI * 2; t += 0.4) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(6 * Math.cos(t), 0.2, 6 * Math.sin(t)), 1, 0, 0, 0, 0, warningDust, true);
        }

        // Loop sinh cột nước liên tiếp
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= geyserCount || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // Random vị trí trong bán kính 5 block quanh tâm
                double offsetX = (random.nextDouble() - 0.5) * 10;
                double offsetZ = (random.nextDouble() - 0.5) * 10;
                Location geyserLoc = center.clone().add(offsetX, 0, offsetZ);
                geyserLoc.setY(geyserLoc.getWorld().getHighestBlockYAt(geyserLoc) + 1.0);

                spawnSingleGeyser(caster, rpgPlayer, geyserLoc, singleGeyserDmg, hitMetaKey);
                count++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 4L); // Cách nhau 4 ticks (0.2s) sinh 1 cột
    }

    private void spawnSingleGeyser(Player caster, RPGPlayer rpgPlayer, Location loc, double dmgMulti, String hitMetaKey) {
        Particle.DustOptions blueDust = new Particle.DustOptions(org.bukkit.Color.BLUE, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Cảnh báo 0.5s (10 ticks)
                if (ticks < 10) {
                    loc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 10, 1.0, 0.2, 1.0, 0.1);
                    for (double t = 0; t < Math.PI * 2; t += 0.8) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(1.5 * Math.cos(t), 0.1, 1.5 * Math.sin(t)), 1, 0, 0, 0, 0, blueDust, true);
                    }
                }
                // Bùng nổ ở tick 10
                else if (ticks == 10) {
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 2.0f, 0.5f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_AGITATED, 1.5f, 1.5f);

                    for (double y = 0; y <= 4.0; y += 0.5) {
                        Location pLoc = loc.clone().add(0, y, 0);
                        loc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 30, 0.8, 0.2, 0.8, 0.1);
                        loc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 10, 0.8, 0.2, 0.8, 0.05);
                    }

                    // Quét quái trong bán kính cột nước nhỏ (R=2.0)
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.0, 3.0, 2.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            // HẤT TUNG: Liên tục bị hất nếu dính nhiều cột
                            target.setVelocity(new Vector(0, 1.35, 0));

                            // TÍNH TOÁN SÁT THƯƠNG & PENALTY
                            double finalMultiToApply = dmgMulti;
                            if (target.hasMetadata(hitMetaKey)) {
                                finalMultiToApply = dmgMulti * 0.10; // Chỉ còn 10% sát thương nếu bị tung hứng tiếp
                            } else {
                                target.setMetadata(hitMetaKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() { target.removeMetadata(hitMetaKey, RPGCore.getInstance()); }
                                }.runTaskLater(RPGCore.getInstance(), 40L); // Cờ tồn tại 2s
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