package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SirensChorus implements ISkill {

    @Override
    public String getId() { return "bard_sirens_chorus"; }

    @Override
    public String getName() { return "Dạ Khúc Dẫn Hồn"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.SCULK_SHRIEKER; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 25; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 45.0 + (level * 5.0); }

    @Override
    public long getCooldown(int level) {
        return (long) ((20.0 - level) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);
        int dmgPerTick = 20 + (displayLevel * 5);

        return Arrays.asList(
                "§7Hát lên một dạ khúc u buồn, tạo ra một",
                "§7hố đen âm nhạc tại vị trí mục tiêu.",
                "",
                "§8▪ §fLoại Âm Tiết: §6[ Âm Trầm ]",
                "§8▪ §fLoại Giai Điệu: §a[ U Ám ]",
                "§8▪ §fPhạm vi: §e6 Block",
                "",
                "§c[Thi Triển - Dạ Khúc Dẫn Hồn]",
                "§f- Duy trì Hố Đen Âm Nhạc trong §e5 giây§f.",
                "§f- Mỗi 0.5s, gây §c" + dmgPerTick + "% ST Phép§f lên kẻ địch",
                "§f  bên trong và liên tục §bhút chúng về tâm§f.",
                "",
                "§a[Cộng Hưởng Giai Điệu - U Ám]",
                "§f- Địch trúng Giai Điệu bị cắm cờ U Ám:",
                "§f  Giảm §c10% Sát thương§f gây ra trong 5 giây."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // [CHUẨN HÓA API]: Gọi thẳng siêu hàm để xử lý Cooldown, Giảm Năng Lượng (Set 4) và Trừ Mana
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // ==========================================
        // 1. ĐIỀU PHỐI ÂM SẮC & KÍCH NỔ GIAI ĐIỆU
        // ==========================================
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        boolean triggeredCore2 = false;
        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                // Hoàn lại lượng Mana vừa bị hàm handleCostAndCooldown trừ đi
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                triggeredCore2 = true;
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        if (isMelodyTriggered) {
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.LOW);
        }

        // ==========================================
        // 2. TÍNH TOÁN CHỈ SỐ GÂY SÁT THƯƠNG
        // ==========================================
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.LOW);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer, ToneType.LOW);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.LOW);

        final double tickDmgMulti = (0.2 + (level * 0.05)) * highToneMultiplier;
        final double extraMagicPen = sharpToneCount * 10.0;
        final double radius = 6.0 * echoRadiusMultiplier;

        Location targetLoc = player.getTargetBlock(null, 15).getLocation().add(0.5, 1, 0.5);

        player.getWorld().playSound(targetLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.5f);
        player.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        // ==========================================
        // 3. VÒNG LẶP HỐ ĐEN ÂM NHẠC (GRAVITY VORTEX)
        // ==========================================
        new BukkitRunnable() {
            int ticksLived = 0;
            double angle = 0.0;

            @Override
            public void run() {
                if (ticksLived++ >= 10 || !player.isOnline()) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    targetLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLoc, 1);
                    this.cancel();
                    return;
                }

                angle += Math.PI / 4;
                for (double r = radius; r > 0; r -= 1.5) {
                    double x = Math.cos(angle + r) * r;
                    double z = Math.sin(angle + r) * r;
                    targetLoc.getWorld().spawnParticle(Particle.NOTE, targetLoc.clone().add(x, 0.2, z), 0, Math.random(), 0, 0, 1);
                    targetLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, targetLoc.clone().add(x, 0.2, z), 1, 0, 0, 0, 0.02);
                }
                targetLoc.getWorld().spawnParticle(Particle.PORTAL, targetLoc, 30, radius/2, 0.5, radius/2, 0.1);

                for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, radius, 3.0, radius)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        LivingEntity victim = (LivingEntity) e;

                        Vector pullDir = targetLoc.toVector().subtract(victim.getLocation().toVector());
                        if (pullDir.lengthSquared() > 1.0) {
                            victim.setVelocity(pullDir.normalize().multiply(0.35).setY(0.1));
                        }

                        // [CHUẨN HÓA API]: Cắm cờ chống lặp, gọi qua CombatManager tính toán sát thương
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, victim, tickDmgMulti, extraMagicPen, true, false);
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, victim, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        if (isMelodyTriggered) {
                            StatusEffectManager.applySlow(victim, 100, 0.3, 1);
                            MelodyUtils.applyTimedBuff(victim, null, BardClass.META_GLOOM_DEBUFF, 100L, false);
                            victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 10L);

        // ==========================================
        // 4. QUẢN LÝ COOLDOWN (ĐỘNG THEO LÕI)
        // ==========================================
        // Sử dụng siêu hàm getFinalCooldown() từ ISkill.java thay vì phép tính thủ công
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

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