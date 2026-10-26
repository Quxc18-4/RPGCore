package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.VitalityMelody;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HealingAnthem implements ISkill {

    @Override
    public String getId() { return "bard_healing_anthem"; }

    @Override
    public String getName() { return "Khúc Ca Sự Sống"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.GLISTERING_MELON_SLICE; }

    @Override
    public int getMaxLevel() { return 6; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 12; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("bard_scattering_chords", 8);
    }

    @Override
    public double getManaCost(int level) {
        return 100.0 + ((level - 1) * 8.0);
    }

    @Override
    public long getCooldown(int level) {
        return (long) ((24.0 - (level - 1) * 1.0) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        // [CÂN BẰNG MỚI]: Base 10%, tăng 1% mỗi cấp (Max 10%)
        int totalPool = 10 + (displayLevel - 1);

        return Arrays.asList(
                "§7Cất lên một khúc ca êm dịu, chia sẻ sinh lực",
                "§7cho bản thân và tối đa 3 đồng minh lân cận.",
                "",
                "§8▪ §fLoại Âm Tiết: §6[ Âm Trầm ]",
                "§8▪ §fLoại Giai Điệu: §a[ Sự Sống ]",
                "§8▪ §fPhạm vi: §e8 Block",
                "",
                "§c[Thi Triển - Khúc Ca Sự Sống]",
                "§f- Hồi phục bằng §a" + totalPool + "% Máu Tối Đa của bản thân§f.",
                "§f- Lượng hồi phục này sẽ được chia đều cho tối đa 3 đồng minh.",
                "§f  (Hồi phục theo một lượng HP cố định).",
                "",
                "§c[Nội Tại - Đơn Điệu]",
                "§f- Nếu xung quanh không có đồng minh, lượng hồi phục tạo ra",
                "§f  bị giảm một nửa, nhưng bù lại §bgiảm 30% Hồi chiêu§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // ==========================================
        // KHU VỰC ĐIỀU PHỐI GIAI ĐIỆU
        // ==========================================
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);

        boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);
        boolean triggeredCore2 = false;

        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                triggeredCore2 = true;
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        // ==========================================
        // CƠ CHẾ SPLIT-POOL HEALING (TRỊ LIỆU CHIA SẺ)
        // ==========================================
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.LOW);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer, ToneType.LOW);
        double searchRadius = 8.0 * echoRadiusMultiplier;

        List<Player> allies = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                allies.add((Player) e);
            }
        }

        allies.sort(Comparator.comparingDouble(a -> a.getLocation().distanceSquared(player.getLocation())));
        List<Player> finalTargets = new ArrayList<>();
        finalTargets.add(player);

        for (int i = 0; i < Math.min(3, allies.size()); i++) {
            finalTargets.add(allies.get(i));
        }

        int targetCount = finalTargets.size();
        boolean isSolo = (targetCount == 1);

        // [CÂN BẰNG MỚI]: Base 0.1 (10%), tăng 0.01 (1%) mỗi cấp
        double totalPoolPercent = (0.1 + ((level - 1) * 0.01)) * highToneMultiplier;
        double healPercentPerTarget;

        if (isSolo) {
            healPercentPerTarget = totalPoolPercent / 2.0;
        } else {
            healPercentPerTarget = totalPoolPercent / targetCount;
        }

        double casterMaxHp = rpgPlayer.getTotalMaxHealth();
        double flatHealAmount = casterMaxHp * healPercentPerTarget;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.5f);
        MelodyUtils.drawExpandingAura(player.getLocation(), searchRadius, org.bukkit.Color.LIME, Particle.HEART);

        for (Player target : finalTargets) {
            RPGPlayer rpgTarget = RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (rpgTarget != null) {
                rpgTarget.heal(flatHealAmount);
                // Đã cập nhật đúng chuẩn API do Giám đốc sửa: HAPPY_VILLAGER
                MelodyUtils.drawTargetFeedback(target, Particle.HAPPY_VILLAGER);
            }
        }

        // ==========================================
        // HOÀN TẤT CHU TRÌNH ÂM SẮC & GIAI ĐIỆU
        // ==========================================
        if (isMelodyTriggered) {
            new VitalityMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.LOW);
        }

        // ==========================================
        // TÍNH TOÁN COOLDOWN THÔNG MINH
        // ==========================================
        long baseCd = getCooldown(level);

        if (isSolo) {
            baseCd = (long) (baseCd * 0.70);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l[Nội Tại - Đơn Điệu] §fGiảm 30% thời gian hồi chiêu!"));
        }

        long finalCd = getFinalCooldown(rpgPlayer, baseCd);

        if (triggeredCore2) finalCd = (long) (finalCd * 0.8);
        if (player.hasMetadata("BARD_ETERNAL_MELODY_TRIGGERED")) {
            finalCd /= 2;
            player.removeMetadata("BARD_ETERNAL_MELODY_TRIGGERED", RPGCore.getInstance());
        }

        player.setMetadata("CD_" + getId(), new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }
}