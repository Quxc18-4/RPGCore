package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.ExplosiveMelody;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CrescendoBlast implements ISkill {

    @Override
    public String getId() { return "bard_crescendo_blast"; }

    @Override
    public String getName() { return "Khúc Ca Chấn Động"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.GOAT_HORN; }

    @Override
    public int getMaxLevel() { return 6; } // Tối đa 6 cấp

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 25.0 + (level * 5.0); }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu 12s, giảm dần còn 9.5s ở max level
        return (long) ((12.0 - (level * 0.5)) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level); // Đảm bảo cấp 0 vẫn hiển thị chỉ số cấp 1

        int dmgPercent = 130 + (displayLevel * 20); // Base 150% ở Cấp 1, Max 250% ở Cấp 6
        double stunDuration = 0.9 + (displayLevel * 0.1); // Base 1.0s ở Cấp 1, Max 1.5s ở Cấp 6

        return Arrays.asList(
                "§7Thổi mạnh vào nhạc cụ, phát ra một luồng",
                "§7sóng âm chói tai theo hình nón phía trước.",
                "",
                "§8▪ §fLoại Âm Tiết: §b[ Âm Cao ]",
                "§8▪ §fLoại Giai Điệu: §c[ Bùng Nổ ]",
                "§8▪ §fPhạm vi: §e6 Block",
                "",
                "§c[Thi Triển - Giai Điệu Bùng Nổ]",
                "§f- Gây §c" + dmgPercent + "% ST Phép§f lên kẻ địch trong vùng.",
                "§f- Đẩy lùi mạnh và làm choáng mục tiêu trong §e" + String.format("%.1f", stunDuration) + "s§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // 1. CHUẨN BỊ CHỈ SỐ SÁT THƯƠNG VÀ KHỐNG CHẾ
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player);
        int sharpToneCount = ToneModifier.getSharpToneCount(player);

        double dmgMulti = (1.3 + (level * 0.2)) * highToneMultiplier;
        double extraMagicPen = sharpToneCount * 10.0;

        // Thời gian choáng tính bằng Tick (20 Ticks = 1s)
        double stunSeconds = 0.9 + (level * 0.1);
        int stunTicks = (int) (stunSeconds * 20);

        // 2. KHU VỰC ĐIỀU PHỐI GIAI ĐIỆU (GDD BARD)
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        // LÕI 2: Hơi Thở Của Gió (Hồi Mana nếu HP > 90%)
        boolean triggeredCore2 = false;
        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level)); // Hoàn trả Mana
                triggeredCore2 = true;
            }
        }

        // 3. TOÁN HỌC VECTOR: TÍNH TOÁN HƯỚNG QUÉT PHẲNG (FLAT YAW)
        double searchRadius = 6.0;
        Location eyeLoc = player.getEyeLocation();
        Vector playerDir = eyeLoc.getDirection();

        // [ĐÃ SỬA] Ép trục Y về 0 để chiêu luôn bay song song mặt đất
        playerDir.setY(0).normalize();

        // Âm thanh xuất chiêu
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
        player.getWorld().playSound(eyeLoc, Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.5f);

        // Vẽ Particle hình nón phía trước mặt dựa trên hướng đã làm phẳng
        drawConeParticles(player, searchRadius, playerDir);

        List<LivingEntity> hitTargets = new ArrayList<>();

        for (Entity e : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                // Vector nối từ người chơi đến mục tiêu (cũng phải ép Y về 0 để tính góc quét chính xác)
                Vector dirToTarget = e.getLocation().toVector().subtract(player.getLocation().toVector());
                dirToTarget.setY(0).normalize();

                // Toán học Vector: Góc quét 90 độ (45 độ mỗi bên). cos(45 độ) ~ 0.707
                if (playerDir.dot(dirToTarget) >= 0.707) {
                    hitTargets.add((LivingEntity) e);
                }
            }
        }

        // 4. ÁP DỤNG SÁT THƯƠNG VÀ KHỐNG CHẾ
        for (LivingEntity target : hitTargets) {
            double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, extraMagicPen, true, false);

            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            CombatManager.applySkillDamage(player, target, dmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            // Đẩy lùi văng ra xa (Knockback)
            Vector knockback = playerDir.clone().multiply(1.5).setY(0.4);
            target.setVelocity(knockback);

            // [ĐÃ SỬA]: Dùng Slow cường độ cực cao (Amplifier 10) để trói chân quái vật mà không triệt tiêu Vector Knockback
            StatusEffectManager.applySlow(target, stunTicks, 1.0, 10);

            // Hạt vỡ khi trúng chiêu
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1);
        }

        // 5. HOÀN TẤT CHU TRÌNH ÂM SẮC & GIAI ĐIỆU
        if (isMelodyTriggered) {
            // Đủ điều kiện -> Kích nổ Giai điệu Bùng Nổ độc quyền của chiêu này
            new ExplosiveMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            // Nếu chưa đủ nổ -> Nạp 1 Âm Cao vào Kho
            ToneManager.addTone(player, rpgPlayer, ToneType.HIGH);
        }

        // 6. TÍNH TOÁN COOLDOWN THỦ CÔNG (ĐỂ ÁP DỤNG LÕI 2 & LÕI 9)
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        if (triggeredCore2) {
            finalCd = (long) (finalCd * 0.8);
        }

        if (player.hasMetadata("BARD_ETERNAL_MELODY_TRIGGERED")) {
            finalCd = finalCd / 2;
            player.removeMetadata("BARD_ETERNAL_MELODY_TRIGGERED", RPGCore.getInstance());
        }

        player.setMetadata("CD_" + getId(), new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }

    // Hàm tiện ích: Vẽ Particle hình nón tỏa ra phía trước
    private void drawConeParticles(Player player, double radius, Vector flatDir) {
        Location startLoc = player.getEyeLocation().add(0, -0.5, 0);

        // Trục vuông góc để xoay dựa trên flatDir (Hướng đã làm phẳng trục Y)
        Vector up = new Vector(0, 1, 0);
        Vector right = flatDir.clone().crossProduct(up).normalize();

        new org.bukkit.scheduler.BukkitRunnable() {
            double distance = 1.0;
            @Override
            public void run() {
                if (distance > radius) {
                    this.cancel();
                    return;
                }

                // Vẽ một vòng cung (Arc) nằm ngang phía trước
                for (double angle = -45; angle <= 45; angle += 15) {
                    double radians = Math.toRadians(angle);
                    Vector offsetDir = flatDir.clone().multiply(Math.cos(radians)).add(right.clone().multiply(Math.sin(radians))).normalize();
                    Location particleLoc = startLoc.clone().add(offsetDir.multiply(distance));

                    player.getWorld().spawnParticle(Particle.NOTE, particleLoc, 0, Math.random(), 0, 0, 1);
                    player.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0.05);
                }
                distance += 1.5; // Giãn cách tốc độ tỏa ra của đợt sóng
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}