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
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.AegisMelody;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MarchingAnthem implements ISkill {

    private final String ACTIVE_METADATA = "BARD_MARCHING_ACTIVE";
    private final Random random = new Random();
    private static boolean isRegistered = false;

    // ==========================================
    // ĐĂNG KÝ CHỈ SỐ BUFF CHUẨN SOLID VÀO COMBAT MANAGER
    // ==========================================
    public MarchingAnthem() {
        if (!isRegistered) {
            CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
                @Override
                public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                    if (victim instanceof Player && victim.hasMetadata("BARD_MARCHING_DR") && victim.hasMetadata("BARD_MARCHING_EXPIRE")) {
                        long expire = victim.getMetadata("BARD_MARCHING_EXPIRE").get(0).asLong();
                        if (System.currentTimeMillis() < expire) {
                            return victim.getMetadata("BARD_MARCHING_DR").get(0).asInt() / 100.0;
                        }
                    }
                    return 0.0;
                }
            });

            CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
                @Override
                public void applyBonusStats(Player player, Map<String, Double> stats) {
                    if (player.hasMetadata("BARD_MARCHING_MS") && player.hasMetadata("BARD_MARCHING_EXPIRE")) {
                        long expire = player.getMetadata("BARD_MARCHING_EXPIRE").get(0).asLong();
                        if (System.currentTimeMillis() < expire) {
                            int msPercent = player.getMetadata("BARD_MARCHING_MS").get(0).asInt();
                            stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + msPercent);
                        }
                    }
                }
            });
            isRegistered = true;
        }
    }

    @Override
    public String getId() { return "bard_marching_anthem"; }

    @Override
    public String getName() { return "Khúc Ca Xung Trận"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.GOLDEN_BOOTS; }

    @Override
    public int getMaxLevel() { return 3; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 22; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("bard_healing_anthem", 4);
    }

    @Override
    public double getManaCost(int level) {
        return 80.0 + (level * 20.0); // Lv1: 100, Lv2: 120, Lv3: 140
    }

    public double getManaDrainPerSecond(int level) {
        return 20.0 + (level * 10.0); // Lv1: 30/s, Lv2: 40/s, Lv3: 50/s
    }

    @Override
    public long getCooldown(int level) {
        return (long) ((19.0 - level) * 1000); // Lv1: 18s, Lv2: 17s, Lv3: 16s
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);
        int baseDr = 4 + (displayLevel * 2); // 6%, 8%, 10%
        int baseMs = 15 + (displayLevel * 15); // 30%, 45%, 60%
        int drain = (int) getManaDrainPerSecond(displayLevel);

        return Arrays.asList(
                "§7Lan tỏa một khúc quân hành ca rực lửa,",
                "§7tiếp bước cho đồng đội tiến lên phía trước.",
                "",
                "§8▪ §fLoại Âm Tiết: §b[ Âm Vang ]",
                "§8▪ §fLoại Giai Điệu: §a[ Can Trường ]",
                "§8▪ §fPhạm vi: §e8 Block",
                "",
                "§c[Thi Triển - Khúc Ca Xung Trận]",
                "§f- Tiêu hao §b" + drain + " Mana/giây§f để duy trì Khúc Ca.",
                "§f- Nhận §e+" + baseMs + "% Tốc độ di chuyển§f cho bản thân và đồng minh.",
                "§f- Đồng thời §aGiảm " + baseDr + "% Sát thương nhận vào§f.",
                "",
                "§8(Tái kích hoạt hoặc cạn Năng lượng sẽ hủy duy trì kỹ năng)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();

        if (player.hasMetadata(ACTIVE_METADATA)) {
            deactivateAura(player, rpgPlayer, level, cdKey);
            return true;
        }

        if (player.hasMetadata(cdKey)) {
            long timeLeft = player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis();
            if (timeLeft > 0) return false;
        }

        // KIỂM TRA MANA MỞ CỬA
        double gateCost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < gateCost) {
            player.sendMessage("§c§l[!] §cKhông đủ Năng lượng để mở vùng! (Cần " + gateCost + " Năng lượng)");
            return false;
        }

        // TÍNH CHỈ SỐ BUFF TỪ ÂM CAO
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.ECHO);
        final int finalDrPercent = (int) Math.round((4 + (level * 2)) * highToneMultiplier);
        final int finalMsPercent = (int) Math.round((15 + (level * 15)) * highToneMultiplier);

        // KIỂM TRA LÕI 8 VÀ KÍCH NỔ GIAI ĐIỆU
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = new ArrayList<>(ToneManager.getCurrentTones(player));
        boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        if (isMelodyTriggered) {
            // [LÕI 2]: Hơi Thở Của Gió (Chỉ việc cắm cờ, Mana vốn đã không trừ)
            if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0 && (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90)) {
                player.setMetadata("BARD_PENDING_CDR_CORE2", new FixedMetadataValue(RPGCore.getInstance(), true));
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fĐã nhận phúc lợi Kỹ năng!");
            }

            // [LÕI 4]: Hợp Âm Dội Lại (Hoàn trả 1 Âm)
            if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_4") > 0 && !currentTones.isEmpty()) {
                ToneType restoredTone = currentTones.get(random.nextInt(currentTones.size()));
                ToneManager.addTone(player, rpgPlayer, restoredTone);
            }

            // Hàm này tự gọi MelodyUtils.processGlobalPerks() để lo Lõi 1, Lõi 6 và Lõi 9!
            new AegisMelody().cast(player, rpgPlayer, player.getLocation());
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.ECHO);
        }

        player.setMetadata(ACTIVE_METADATA, new FixedMetadataValue(RPGCore.getInstance(), true));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.2f);
        player.sendMessage("§a§l[+] §fBạn đã tấu lên §eKhúc Ca Xung Trận§f!");

        startAuraTask(player, rpgPlayer, level, cdKey, finalDrPercent, finalMsPercent);
        return true;
    }

    private void startAuraTask(Player player, RPGPlayer rpgPlayer, int level, String cdKey, int drPercent, int msPercent) {
        final double drainPerTick = getManaDrainPerSecond(level) / 2;
        final double fixedRadius = 8.0; // Bán kính cứng tuyệt đối

        new BukkitRunnable() {
            @Override
            public void run() {
                // Kiểm tra điều kiện sống sót và trạng thái kích hoạt
                if (!player.isOnline() || player.isDead() || !player.hasMetadata(ACTIVE_METADATA)) {
                    deactivateAura(player, rpgPlayer, level, cdKey);
                    this.cancel();
                    return;
                }

                double currentMana = rpgPlayer.getCurrentResource();

                // [FIX LỖI CẠN MANA]: Kiểm tra nếu Mana không đủ cho chu kỳ tiếp theo -> Tự ngắt ngay lập tức
                if (currentMana < drainPerTick) {
                    player.sendMessage("§c§l[-] §cCạn kiệt Năng lượng! Khúc Ca Xung Trận tự động ngắt.");
                    deactivateAura(player, rpgPlayer, level, cdKey);
                    this.cancel();
                    return;
                }

                // Trừ Mana duy trì
                rpgPlayer.setCurrentResource(currentMana - drainPerTick);

                // Vẽ hào quang vòng tròn biên giới 8 Block
                Location center = player.getLocation();
                for (int i = 0; i < 360; i += 30) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * fixedRadius;
                    double z = Math.sin(angle) * fixedRadius;
                    center.getWorld().spawnParticle(Particle.GLOW, center.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                }

                // Buff bản thân và quét đồng minh trong phạm vi
                applyAuraBuff(player, drPercent, msPercent);
                for (Entity e : center.getWorld().getNearbyEntities(center, fixedRadius, 2.0, fixedRadius)) {
                    if (e instanceof Player && !e.equals(player) && !CombatManager.canTarget(player, e)) {
                        applyAuraBuff((Player) e, drPercent, msPercent);
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 10L);
    }

    private void applyAuraBuff(Player target, int drPercent, int msPercent) {
        long expireTime = System.currentTimeMillis() + 1200; // Buffer 1.2s để tránh giật lag giữa các tick
        target.setMetadata("BARD_MARCHING_DR", new FixedMetadataValue(RPGCore.getInstance(), drPercent));
        target.setMetadata("BARD_MARCHING_MS", new FixedMetadataValue(RPGCore.getInstance(), msPercent));
        target.setMetadata("BARD_MARCHING_EXPIRE", new FixedMetadataValue(RPGCore.getInstance(), expireTime));
        target.getWorld().spawnParticle(Particle.NOTE, target.getLocation().add(0, 2, 0), 1, 0.5, 0, 0, 1);
    }

    private void deactivateAura(Player player, RPGPlayer rpgPlayer, int level, String cdKey) {
        if (player.hasMetadata(ACTIVE_METADATA)) {
            player.removeMetadata(ACTIVE_METADATA, RPGCore.getInstance());
            if (player.isOnline()) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                player.sendMessage("§8§l[-] §7Đã hết hiệu quả của Khúc Ca Xung Trận.");
            }
        }

        // BẮT ĐẦU TÍNH COOLDOWN
        long finalCd = getCooldown(level);

        // Thanh toán Lõi 2 (-20%)
        if (player.hasMetadata("BARD_PENDING_CDR_CORE2")) {
            finalCd = (long) (finalCd * 0.8);
            player.removeMetadata("BARD_PENDING_CDR_CORE2", RPGCore.getInstance());
        }

        // ĐỌC CỜ TỪ MelodyUtils.processGlobalPerks()
        if (player.hasMetadata("BARD_ETERNAL_MELODY_TRIGGERED")) {
            finalCd = (long) (finalCd * 0.5);
            player.removeMetadata("BARD_ETERNAL_MELODY_TRIGGERED", RPGCore.getInstance());
        }

        if (player.isOnline()) {
            player.setMetadata(cdKey, new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
            startCooldownNotifier(player, finalCd);
        }
    }
}