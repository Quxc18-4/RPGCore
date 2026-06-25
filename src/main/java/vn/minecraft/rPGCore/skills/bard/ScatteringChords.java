package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Color;
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
import vn.minecraft.rPGCore.classes.impl.bard.melodies.UpbeatMelody;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class ScatteringChords implements ISkill {

    @Override public String getId() {return "bard_scattering_chords"; }
    @Override public String getName() { return "Hợp Âm Tán Xạ"; }
    @Override public String getRequiredClass() { return "bard"; }
    @Override public Material getIcon() { return Material.AMETHYST_SHARD; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 15.0 + (level * 2.0); }

    @Override
    public long getCooldown(int level) { return 3000L; } // 3.0s Hồi chiêu cố định

    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        // Công thức Re-balance: Cấp 1 = 50%, mỗi cấp + 8%
        int totalDmg = 50 + ((displayLevel - 1) * 8);
        int radius = 10;

        return Arrays.asList(
                "§7Thổi lên 1 giai điệu, giải phóng các dải",
                "§7sóng âm tự động săn lùng 3 kẻ địch gần nhất.",
                "",
                "§8▪ §fLoại Âm Tiết: §b[ Âm Vang ]",
                "§8▪ §fLoại Giai Điệu: §a[ Dồn Dập ]",
                "§8▪ §fPhạm vi: §e" + radius + " Block",
                "",
                "§c[Thi Triển - Hợp Âm Tán Xạ]",
                "§f- Phóng sóng âm tới §e3 kẻ địch§f gần nhất.",
                "§f- Không cần ngắm bắn (Tự động khóa mục tiêu).",
                "§f- Gây §c" + totalDmg + "% Sát Thương Phép Thuật§f mỗi tia."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {

        // 1. Gọi Siêu Hàm từ ISkill: Xử lý Cooldown và trừ Mana
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // 2. LẤY HỆ SỐ TỪ TONE MODIFIER
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        int sharpToneCount = ToneModifier.getSharpToneCount(player);

        // Kiểm tra nổ Giai Điệu
        int maxTones = rpgPlayer.hasSetBonus("BARD_CORE_8", 1) ? 3 : 2;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        boolean isMelodyTriggered = currentTones.size() >= maxTones;

        if (isMelodyTriggered) {
            new UpbeatMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
        }

        // 3. TÍNH TOÁN SÁT THƯƠNG THEO SCALE MỚI
        double searchRadius = 10.0 * echoRadiusMultiplier;
        double dmgMulti = (0.5 + ((level - 1) * 0.08)) * highToneMultiplier;
        double extraMagicPen = sharpToneCount * 10.0;

        // 4. TÌM MỤC TIÊU HỢP LỆ
        List<LivingEntity> validTargets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            // Tuân thủ Rules: Check canTarget[cite: 1]
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                validTargets.add((LivingEntity) e);
            }
        }

        validTargets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));
        List<LivingEntity> finalTargets = validTargets.subList(0, Math.min(3, validTargets.size()));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.8f);

        // 5. VÒNG LẶP GÂY SÁT THƯƠNG
        for (LivingEntity target : finalTargets) {
            drawSonicCurve(player.getLocation().add(0, 1.2, 0), target.getLocation().add(0, 1.0, 0));

            double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, extraMagicPen, true, false);

            // Tuân thủ Rules: Cắm cờ chống lặp sát thương[cite: 1]
            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            CombatManager.applySkillDamage(player, target, dmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
        }

        // 6. NẠP ÂM VÀ KHÓA COOLDOWN
        ToneManager.addTone(player, rpgPlayer, ToneType.ECHO);
        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    private void drawSonicCurve(Location start, Location end) {
        Vector startVec = start.toVector();
        Vector endVec = end.toVector();

        Vector midVec = startVec.clone().midpoint(endVec);
        double randomX = (Math.random() - 0.5) * 3.0;
        double randomZ = (Math.random() - 0.5) * 3.0;
        midVec.add(new Vector(randomX, 1.5, randomZ));

        int steps = 20;
        Particle.DustTransition dust = new Particle.DustTransition(Color.AQUA, Color.BLUE, 0.8f);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;

            Vector point = startVec.clone().multiply(u * u)
                    .add(midVec.clone().multiply(2 * u * t))
                    .add(endVec.clone().multiply(t * t));

            Location loc = new Location(start.getWorld(), point.getX(), point.getY(), point.getZ());
            start.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0, dust);
        }
    }
}