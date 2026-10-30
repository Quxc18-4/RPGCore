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
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
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

    @Override public double getManaCost(int level) { return 15.0 + (level * 2.0); }
    @Override public long getCooldown(int level) { return 3000L; }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        // [CÂN BẰNG MỚI]: Base 50%, tăng 10% mỗi cấp (Max 140% ở cấp 10)
        int totalDmg = 40 + (displayLevel * 10);

        return Arrays.asList(
                "§7Thổi lên 1 giai điệu, phóng sóng âm",
                "§7tự động săn lùng 3 kẻ địch gần nhất.",
                "",
                "§8▪ §fLoại Âm Tiết: §b[ Âm Vang ]",
                "§8▪ §fLoại Giai Điệu: §a[ Dồn Dập ]",
                "§8▪ §fPhạm vi: §e10 Block",
                "",
                "§c[Thi Triển - Hợp Âm Tán Xạ]",
                "§f- Gây §c" + totalDmg + "% Sát Thương Phép§f mỗi tia."
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

        boolean willTriggerMelody = (currentTones.size() + 1) >= maxCapacity;
        boolean canExplode = MelodyUtils.canTriggerMelody(player);

        boolean triggeredCore2 = false;
        if (willTriggerMelody && canExplode && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            double hpPercent = player.getHealth() / rpgPlayer.getTotalMaxHealth();
            if (hpPercent > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                triggeredCore2 = true;
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        // ==========================================
        // GÂY SÁT THƯƠNG KỸ NĂNG (Tích hợp Trigger Tone: ECHO)
        // ==========================================
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.ECHO);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.ECHO);

        double searchRadius = 10.0; // <-- Chốt cứng 10 block theo đúng mô tả

        // Base Damage: 0.4 + (0.1 * level) -> Level 1 là 0.5 (50%), Level 10 là 1.4 (140%)
        double dmgMulti = (0.4 + (level * 0.10)) * highToneMultiplier;
        double extraMagicPen = sharpToneCount * 10.0;

        List<LivingEntity> validTargets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                validTargets.add((LivingEntity) e);
            }
        }

        validTargets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));
        List<LivingEntity> finalTargets = validTargets.subList(0, Math.min(3, validTargets.size()));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.8f);

        for (LivingEntity target : finalTargets) {
            launchSonicProjectile(player, rpgPlayer, target, dmgMulti, extraMagicPen);
        }

        // Hoàn tất chu trình Âm & Giai điệu
        if (willTriggerMelody) {
            if (canExplode) {
                new UpbeatMelody().cast(player, rpgPlayer, player.getLocation());
                ToneManager.consumeTones(player, rpgPlayer);
                MelodyUtils.setMelodyCooldown(player);
            } else {
                long remaining = (player.getMetadata("BARD_MELODY_GCD").get(0).asLong() - System.currentTimeMillis()) / 1000;
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§l[!] §fGiai điệu đang hồi: §e" + remaining + "s"));
            }
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.ECHO);
        }

        // Cập nhật Cooldown thủ công
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        if (triggeredCore2) finalCd = (long) (finalCd * 0.8);
        if (player.hasMetadata("BARD_ETERNAL_MELODY_TRIGGERED")) {
            finalCd = finalCd / 2;
            player.removeMetadata("BARD_ETERNAL_MELODY_TRIGGERED", RPGCore.getInstance());
        }

        player.setMetadata("CD_" + getId(), new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);
        return true;
    }

    private void launchSonicProjectile(Player player, RPGPlayer rpgPlayer, LivingEntity target, double dmgMulti, double extraMagicPen) {
        Location start = player.getEyeLocation().add(0, -0.2, 0);
        Vector startVec = start.toVector();

        double randomX = (Math.random() - 0.5) * 4.0;
        double randomY = 1.0 + (Math.random() * 2.0);
        double randomZ = (Math.random() - 0.5) * 4.0;
        Vector controlVec = startVec.clone().midpoint(target.getLocation().toVector()).add(new Vector(randomX, randomY, randomZ));

        Particle.DustTransition dust = new Particle.DustTransition(Color.AQUA, Color.BLUE, 1.5f);

        new org.bukkit.scheduler.BukkitRunnable() {
            double t = 0.0;
            double speed = 0.08;

            @Override
            public void run() {
                if (t >= 1.0 || !target.isValid() || target.isDead() || !player.isOnline()) {
                    if (target.isValid() && !target.isDead()) {
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, extraMagicPen, true, false);
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, target, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    }
                    this.cancel();
                    return;
                }

                Vector endVec = target.getLocation().add(0, target.getHeight() / 2, 0).toVector();
                double u = 1 - t;
                Vector currentPos = startVec.clone().multiply(u * u)
                        .add(controlVec.clone().multiply(2 * u * t))
                        .add(endVec.clone().multiply(t * t));

                Location loc = new Location(player.getWorld(), currentPos.getX(), currentPos.getY(), currentPos.getZ());
                loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 3, 0.1, 0.1, 0.1, 0, dust);
                loc.getWorld().spawnParticle(Particle.NOTE, loc, 1, Math.random(), 0, 0, 1);

                if (t % 0.2 < speed) {
                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                }
                t += speed;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}