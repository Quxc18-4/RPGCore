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
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.ValorMelody;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PiercingAnthem implements ISkill {

    private final Random random = new Random();

    @Override
    public String getId() { return "bard_piercing_anthem"; }

    @Override
    public String getName() { return "Khúc Ca Xuyên Phá"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.DISC_FRAGMENT_5; }

    @Override
    public int getMaxLevel() { return 10; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 3.0);
    }

    @Override
    public long getCooldown(int level) {
        return (long) ((8.0 - (level * 0.2)) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);
        int dmgPercent = 100 + (displayLevel * 20);
        return Arrays.asList(
                "§7Phóng ra một lưỡi dao sóng âm xuyên thấu",
                "§7mọi thứ trên đường bay trước khi phát nổ.",
                "",
                "§8▪ §fLoại Âm Tiết: §c[ Âm Sắc ]",
                "§8▪ §fLoại Giai Điệu: §a[ Dũng Cảm ]",
                "§8▪ §fPhạm vi: §e12 Block",
                "",
                "§c[Thi Triển - Khúc Ca Xuyên Phá]",
                "§f- Gây §c" + dmgPercent + "% ST Phép§f cho kẻ địch trúng chiêu.",
                "§f- Vụ nổ tạo ra 3 mảnh §eDư Âm§f trên mặt đất.",
                "§f- Hạ gục kẻ địch sẽ rớt thêm 1 mảnh (Tối đa 6).",
                "",
                "§e[Nội Tại - Dư Âm]",
                "§f- Đồng minh nhặt Dư Âm nhận §c+10% Tỉ lệ Chí mạng",
                "§f  và §b+15% Xuyên Giáp/Phép§f trong 6 giây.",
                "",
                "§8(Hiệu ứng Dư Âm không cộng dồn)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.SHARP);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.SHARP);

        // Khai báo Effectively Final theo luật[cite: 14]
        final double dmgMulti = (1.0 + (level * 0.2)) * highToneMultiplier;
        final double extraMagicPen = sharpToneCount * 10.0;

        Location startLoc = player.getEyeLocation().add(0, -0.2, 0);
        Vector direction = startLoc.getDirection().normalize();

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

        if (isMelodyTriggered) {
            new ValorMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.SHARP);
        }

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        player.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.8f, 2.0f);

        Vector tempRight = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (tempRight.lengthSquared() == 0) {
            tempRight = direction.clone().crossProduct(new Vector(1, 0, 0)).normalize();
        }
        final Vector right = tempRight;

        new BukkitRunnable() {
            Location current = startLoc.clone();
            int ticksLived = 0;
            int killCount = 0;

            // BỘ NHỚ LƯU MỤC TIÊU ĐÃ TRÚNG ĐÒN (Chống sát thương lặp)
            Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                // 1. CHỈ KÍCH NỔ KHI ĐẾN ĐÍCH HOẶC TRÚNG TƯỜNG[cite: 13]
                if (ticksLived++ > 15 || current.getBlock().getType().isSolid() || !player.isOnline()) {
                    triggerExplosion(current);
                    this.cancel();
                    return;
                }

                current.add(direction.clone().multiply(0.8));

                for (double i = -1.5; i <= 1.5; i += 0.3) {
                    double backOffset = (i * i) * 0.4;
                    Location particleLoc = current.clone().add(right.clone().multiply(i)).subtract(direction.clone().multiply(backOffset));
                    current.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0, 0, 0, 0);
                    current.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
                }

                // 2. QUÉT MỤC TIÊU XUYÊN THẤU
                for (Entity e : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) { // Lọc Đồng Minh[cite: 14]
                        LivingEntity target = (LivingEntity) e;

                        // Lọc các mục tiêu đã ăn sát thương từ lưỡi đao này
                        if (hitTargets.contains(target.getUniqueId())) continue;
                        hitTargets.add(target.getUniqueId());

                        double hpBefore = target.getHealth();

                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, extraMagicPen, true, false); // Tính toán DMG[cite: 14]
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true)); // Chặn lặp sát thương Vanilla[cite: 14]
                        CombatManager.applySkillDamage(player, target, dmg); // Gây DMG chuẩn (Đã kèm Lifesteal)[cite: 14]
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance()); // Xóa cờ chặn[cite: 14]

                        if (target.isDead() || hpBefore - dmg <= 0) {
                            killCount++;
                        }
                    }
                }
            }

            private void triggerExplosion(Location loc) {
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 2.0f);

                for(int i = 0; i < 20; i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    Vector burst = new Vector(Math.cos(angle), random.nextDouble() * 0.5, Math.sin(angle)).normalize().multiply(0.5);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 0, burst.getX(), burst.getY(), burst.getZ());
                }

                int totalNotes = Math.min(6, 3 + killCount);
                List<Location> residueLocs = new ArrayList<>();

                for (int i = 0; i < totalNotes; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 4.0;
                    double offsetZ = (random.nextDouble() - 0.5) * 4.0;
                    Location noteLoc = loc.clone().add(offsetX, 0.5, offsetZ);

                    while (noteLoc.getBlock().getType().isAir() && noteLoc.getY() > noteLoc.getWorld().getMinHeight()) {
                        noteLoc.subtract(0, 1, 0);
                    }
                    residueLocs.add(noteLoc.add(0, 1, 0));
                }

                spawnVirtualResidue(residueLocs);
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

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

    private void spawnVirtualResidue(List<Location> locations) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 120 || locations.isEmpty()) {
                    this.cancel();
                    return;
                }

                List<Location> toRemove = new ArrayList<>();
                for (Location loc : locations) {
                    loc.getWorld().spawnParticle(Particle.WAX_ON, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.2, 0.2, 0);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0.02);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                        if (e instanceof Player && !CombatManager.canTarget((Player) e, e)) { // Lọc đồng đội[cite: 14]
                            Player ally = (Player) e;

                            if (!ally.hasMetadata("BARD_RESIDUE_BUFF")) {
                                applyResidueBuff(ally);
                                toRemove.add(loc);

                                ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                                ally.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ally.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                                break;
                            }
                        }
                    }
                }
                locations.removeAll(toRemove);
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 5L);
    }

    private void applyResidueBuff(Player player) {
        long expireTime = System.currentTimeMillis() + (6 * 1000);
        player.setMetadata("BARD_RESIDUE_BUFF", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.hasMetadata("BARD_RESIDUE_BUFF")) {
                    long currentExpire = player.getMetadata("BARD_RESIDUE_BUFF").get(0).asLong();
                    if (System.currentTimeMillis() >= currentExpire) {
                        player.removeMetadata("BARD_RESIDUE_BUFF", RPGCore.getInstance());
                    }
                }
            }
        }.runTaskLater(RPGCore.getInstance(), 125L);
    }
}