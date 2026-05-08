package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ThunderstormFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.LIGHT; }
    @Override public String getReactionName() { return "§e§lBão Điện Từ"; } // Đã cập nhật tên

    @Override public double getDamageMultiplier() { return 0.80; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double reactDmg = totalDmg * 1.5; // Giả lập hiển thị DMG sau phản ứng Chớp Giật

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Bão Điện Từ]",
                "§f- Triệu hồi một Vùng Sấm Sét bao trùm bán kính §a10 block§f",
                "§f  xung quanh vị trí thi triển, tồn tại trong §a5 giây§f.",
                "§f- Giáng liên tục 10 tia sét hỗn loạn từ trên trời xuống.",
                "§f- Mỗi tia sét kích hoạt §e§lCHỚP GIẬT§f, gây §c" + (int)reactDmg + "% §fSát thương",
                "§f  và Tê Liệt hoàn toàn kẻ địch trong phạm vi ảnh hưởng.",
                "",
                "§b[Nội tại - Miễn Nhiễm Điện Từ]",
                "§f- Kẻ địch vừa bị sét đánh sẽ không bị sét nhắm mục tiêu",
                "§f  trong §a4 giây§f tiếp theo, đồng thời miễn nhiễm Tê liệt",
                "§f- Giảm §e90% Sát thương§f từ sét nếu bị dính lan."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location center = caster.getLocation().clone();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);

        startThunderstorm(caster, rpgPlayer, center, totalDmgMulti);
    }

    private void startThunderstorm(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        final double stormRadius = 10.0;
        final int maxStrikes = 10;

        // Cờ khóa mục tiêu để xử lý cơ chế giảm 90% DMG
        String immunityKey = "Thunderstorm_Immunity_" + caster.getUniqueId();

        // Vẽ vòng ranh giới Bão
        Particle.DustOptions stormColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
        for (double t = 0; t < Math.PI * 2; t += 0.4) {
            Location edge = center.clone().add(stormRadius * Math.cos(t), 0.2, stormRadius * Math.sin(t));
            center.getWorld().spawnParticle(Particle.DUST, edge, 1, 0, 0, 0, 0, stormColor, true);
        }

        new BukkitRunnable() {
            int strikes = 0;

            @Override
            public void run() {
                if (strikes >= maxStrikes || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. Lọc mục tiêu (CHỈ CHỌN NHỮNG KẺ ĐỊCH CHƯA CÓ CỜ MIỄN NHIỄM)
                List<LivingEntity> validTargets = new ArrayList<>();
                for (Entity e : center.getWorld().getNearbyEntities(center, stormRadius, 5.0, stormRadius)) {
                    if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                        // [UPDATE]: Bỏ qua nếu đang trong thời gian 4s miễn nhiễm
                        if (!e.hasMetadata(immunityKey)) {
                            validTargets.add((LivingEntity) e);
                        }
                    }
                }

                Location strikeLoc;

                // 2. LOGIC 70/30 TARGETING
                if (!validTargets.isEmpty() && random.nextInt(100) < 70) {
                    strikeLoc = validTargets.get(random.nextInt(validTargets.size())).getLocation();
                } else {
                    double r = random.nextDouble() * stormRadius;
                    double angle = random.nextDouble() * 2 * Math.PI;
                    strikeLoc = center.clone().add(r * Math.cos(angle), 0, r * Math.sin(angle));

                    int highestY = strikeLoc.getWorld().getHighestBlockYAt(strikeLoc);
                    if (Math.abs(highestY - center.getBlockY()) < 10) {
                        strikeLoc.setY(highestY + 1.0);
                    }
                }

                // 3. THỰC THI SÉT ĐÁNH
                executeLightningStrike(caster, rpgPlayer, strikeLoc, totalDmgMulti, immunityKey);

                strikes++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 10L);
    }

    private void executeLightningStrike(Player caster, RPGPlayer rpgPlayer, Location strikeLoc, double totalDmgMulti, String immunityKey) {
        // Hiệu ứng Sét Vanilla
        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);

        // Hiệu ứng Hạt hỗ trợ (Đã thay đổi Particle để tránh lỗi Color)
        strikeLoc.getWorld().spawnParticle(Particle.FIREWORK, strikeLoc.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        strikeLoc.getWorld().spawnParticle(Particle.END_ROD, strikeLoc, 20, 1.5, 1.5, 1.5, 0.1);

        // Hitbox 3x3 (Radius ~ 1.5 block)
        for (Entity e : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 1.5, 3.0, 1.5)) {
            if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                LivingEntity target = (LivingEntity) e;

                double appliedMulti;

                // [CƠ CHẾ GIẢM SÁT THƯƠNG & MIỄN KHỐNG CHẾ (DR)]
                if (target.hasMetadata(immunityKey)) {
                    // Nếu đang có cờ miễn nhiễm: KHÔNG kích phản ứng, chỉ nhận 10% sát thương gốc.
                    appliedMulti = totalDmgMulti * 0.10;
                } else {
                    // Nếu chưa có cờ: Áp ấn -> Kích phản ứng Chớp Giật -> Tính DMG
                    ReactionEngine.applyMarkOnly(target, MageElement.LIGHT);
                    ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(caster, target, MageElement.FIRE);

                    appliedMulti = totalDmgMulti * (reaction != null ? reaction.damageMultiplier : 1.0);

                    // Đóng cờ Miễn nhiễm nhắm bắn & phản ứng 4s (80 ticks)
                    target.setMetadata(immunityKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) target.removeMetadata(immunityKey, RPGCore.getInstance());
                        }
                    }.runTaskLater(RPGCore.getInstance(), 80L);
                }

                // Gây sát thương cuối cùng
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, appliedMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }
}