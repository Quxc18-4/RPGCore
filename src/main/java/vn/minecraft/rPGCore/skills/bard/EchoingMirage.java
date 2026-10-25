package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MirageMelody; // Giả định class kích hoạt Ảo Ảnh
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EchoingMirage implements ISkill {

    @Override
    public String getId() { return "bard_echoing_mirage"; }

    @Override
    public String getName() { return "Cung Đàn Lưu Ảnh"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.PHANTOM_MEMBRANE; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 15; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 35.0 + (level * 5.0); }

    @Override
    public long getCooldown(int level) { return (long) ((14.0 - level) * 1000); }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        int dashDmg = 60 + (displayLevel * 20); // Lướt: 80% -> 160%
        int explosionDmg = 130 + (displayLevel * 30); // Nổ: 160% -> 280%

        return Arrays.asList(
                "§7Hóa thân thành những nốt nhạc ảo ảnh,",
                "§7cắt xuyên qua chiến trường để lại tàn dư.",
                "",
                "§8▪ §fLoại Âm Tiết: §c[ Âm Sắc ]",
                "§8▪ §fLoại Giai Điệu: §a[ Ảo Ảnh ]",
                "",
                "§c[Thi Triển - Cung Đàn Lưu Ảnh]",
                "§f- Lướt nhanh về phía trước, gây §c" + dashDmg + "% ST Phép",
                "§f  lên những kẻ địch bị xuyên qua.",
                "§f- Để lại một Lưu Ảnh tại vị trí xuất phát.",
                "§f- Sau 1.2s, Lưu Ảnh phát nổ gây §c" + explosionDmg + "% ST Phép§f.",
                "",
                "§a[Cộng Hưởng Giai Điệu - Ảo Ảnh]",
                "§f- Lập tức Tàng hình trong 2s.",
                "§f- Xóa bỏ mọi sự chú ý (Aggro) của quái vật."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // [SIÊU HÀM]: Tự động xử lý Mana, Cooldown, Set 4 Magical
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // ==========================================
        // 1. ĐIỀU PHỐI ÂM SẮC & GIAI ĐIỆU
        // ==========================================
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                // Hoàn lại Mana vì Hơi Thở Của Gió (Do hàm handleCostAndCooldown đã lỡ trừ trước)
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        if (isMelodyTriggered) {
            // Gọi Melody Giai Điệu Ảo Ảnh (Xử lý Tàng hình & Xóa Aggro)
            new MirageMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.SHARP);
        }

        // ==========================================
        // 2. CHUẨN BỊ CHỈ SỐ GÂY SÁT THƯƠNG
        // ==========================================
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.SHARP);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.SHARP);

        // Base Dash: 0.6 + 0.2/lv | Base Explosion: 1.3 + 0.3/lv
        final double dashDmgMulti = (0.6 + (level * 0.2)) * highToneMultiplier;
        final double explodeDmgMulti = (1.3 + (level * 0.3)) * highToneMultiplier;
        final double extraMagicPen = sharpToneCount * 10.0;

        Location startLoc = player.getLocation().clone();

        // ==========================================
        // 3. THỰC THI CƠ CHẾ LƯỚT (DASH)
        // ==========================================
        Vector dashVector = player.getLocation().getDirection().normalize().multiply(1.8).setY(0.25);
        player.setVelocity(dashVector);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);

        // Vòng lặp vẽ đường lướt và sát thương kẻ địch trên đường bay
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 8 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // Vẽ hạt bụi ma thuật dọc đường lướt
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);

                // Gây sát thương quét (Hitbox 1.5)
                for (Entity e : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;
                        // Chống lặp sát thương Dash (Tránh 1 con ăn 3-4 tick dash)
                        if (!target.hasMetadata("Dash_Hit_" + player.getUniqueId())) {
                            target.setMetadata("Dash_Hit_" + player.getUniqueId(), new FixedMetadataValue(RPGCore.getInstance(), true));

                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dashDmgMulti, extraMagicPen, true, false);
                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, target, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            // Hẹn giờ xóa cờ chống lặp sát thương
                            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> target.removeMetadata("Dash_Hit_" + player.getUniqueId(), RPGCore.getInstance()), 20L);
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        // ==========================================
        // 4. LƯU ẢNH VÀ PHÁT NỔ SAU 1.2 GIÂY (24 TICKS)
        // ==========================================
        // Vẽ bóng mờ tại vị trí xuất phát
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 24) {
                    this.cancel();
                    return;
                }
                // Hiệu ứng "Lưu ảnh" xoáy tại chỗ
                startLoc.getWorld().spawnParticle(Particle.WHITE_ASH, startLoc.clone().add(0, 1, 0), 10, 0.4, 0.8, 0.4, 0.02);
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        // Phát nổ sau Delay
        new BukkitRunnable() {
            @Override
            public void run() {
                startLoc.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.8f);
                startLoc.getWorld().playSound(startLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                startLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, startLoc.clone().add(0, 1, 0), 1);

                // AoE Nổ (Bán kính 4 block)
                for (Entity e : startLoc.getWorld().getNearbyEntities(startLoc, 4, 3, 4)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, explodeDmgMulti, extraMagicPen, true, false);

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, target, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                    }
                }
            }
        }.runTaskLater(RPGCore.getInstance(), 24L);

        // [SIÊU HÀM]: Khóa Cooldown (Đã bao gồm Start Notifier)
        applyGlobalCooldown(player, rpgPlayer, level);

        return true;
    }
}