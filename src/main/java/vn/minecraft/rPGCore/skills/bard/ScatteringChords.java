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
        int totalDmg = 50 + ((displayLevel - 1) * 8);
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
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) return false;
        }

        // ==========================================
        // MASTER FLOW: KIỂM TRA NỔ GIAI ĐIỆU (GDD)
        // ==========================================
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);

        boolean willTriggerMelody = (currentTones.size() + 1) >= maxCapacity;
        boolean canExplode = MelodyUtils.canTriggerMelody(player);

        boolean triggeredCore2 = false;
        double cost = getManaCost(level);
        if (willTriggerMelody && canExplode && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            double hpPercent = player.getHealth() / rpgPlayer.getTotalMaxHealth();
            if (hpPercent > 0.90) {
                cost = 0;
                triggeredCore2 = true;
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        if (rpgPlayer.getCurrentResource() < cost) return false;
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

        ToneManager.addTone(player, rpgPlayer, ToneType.ECHO);

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
        }

        // ==========================================
        // GÂY SÁT THƯƠNG KỸ NĂNG
        // ==========================================
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        int sharpToneCount = ToneModifier.getSharpToneCount(player);

        double searchRadius = 10.0 * echoRadiusMultiplier;
        double dmgMulti = (0.5 + ((level - 1) * 0.08)) * highToneMultiplier;
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

        // Bắn từng Tên lửa sóng âm tới các mục tiêu
        for (LivingEntity target : finalTargets) {
            launchSonicProjectile(player, rpgPlayer, target, dmgMulti, extraMagicPen);
        }

        long finalCd = getCooldown(level);
        double cdrPercent = rpgPlayer.getTotalStatsMap().getOrDefault("Giảm hồi chiêu", 0.0);
        finalCd = (long) (finalCd * (1.0 - (cdrPercent / 100.0)));
        if (triggeredCore2) finalCd = (long) (finalCd * 0.8);

        if (player.hasMetadata("BARD_ETERNAL_MELODY_TRIGGERED")) {
            finalCd = finalCd / 2;
            player.removeMetadata("BARD_ETERNAL_MELODY_TRIGGERED", RPGCore.getInstance());
        }

        player.setMetadata(cdKey, new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        return true;
    }

    // ==========================================
    // TÊN LỬA SÓNG ÂM (SONA'S BEAM STYLE)
    // ==========================================
    private void launchSonicProjectile(Player player, RPGPlayer rpgPlayer, LivingEntity target, double dmgMulti, double extraMagicPen) {
        Location start = player.getEyeLocation().add(0, -0.2, 0);
        Vector startVec = start.toVector();

        // Điểm uốn (Control Point) để tạo quỹ đạo cong nịnh mắt
        double randomX = (Math.random() - 0.5) * 4.0;
        double randomY = 1.0 + (Math.random() * 2.0); // Ép đường đạn luôn có xu hướng vút lên trên một chút rồi mới cắm xuống
        double randomZ = (Math.random() - 0.5) * 4.0;
        Vector controlVec = startVec.clone().midpoint(target.getLocation().toVector()).add(new Vector(randomX, randomY, randomZ));

        Particle.DustTransition dust = new Particle.DustTransition(Color.AQUA, Color.BLUE, 1.5f);

        new org.bukkit.scheduler.BukkitRunnable() {
            double t = 0.0;
            double speed = 0.08; // Tốc độ đạn (Mất khoảng ~12 Ticks để trúng đích)

            @Override
            public void run() {
                // Hủy nếu quái chết, người chơi thoát, hoặc đạn bay hết chu kỳ
                if (t >= 1.0 || !target.isValid() || target.isDead() || !player.isOnline()) {

                    // [SÁT THƯƠNG KHI CHẠM ĐÍCH]: Đạn nổ mới mất máu
                    if (target.isValid() && !target.isDead()) {
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, extraMagicPen, true, false);
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, target, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        // Âm thanh và Vụ nổ khi đạn ghim vào người
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    }
                    this.cancel();
                    return;
                }

                // Cập nhật vị trí kẻ địch liên tục (Nếu nó đang chạy trốn)
                Vector endVec = target.getLocation().add(0, target.getHeight() / 2, 0).toVector();

                // Toán học Bezier Curve: Nội suy vị trí hiện tại của viên đạn
                double u = 1 - t;
                Vector currentPos = startVec.clone().multiply(u * u)
                        .add(controlVec.clone().multiply(2 * u * t))
                        .add(endVec.clone().multiply(t * t));

                Location loc = new Location(player.getWorld(), currentPos.getX(), currentPos.getY(), currentPos.getZ());

                // Vẽ Lõi Đạn (Khối sáng xanh dương)
                loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 3, 0.1, 0.1, 0.1, 0, dust);
                // Vẽ Đuôi Đạn (Các nốt nhạc rớt lại phía sau)
                loc.getWorld().spawnParticle(Particle.NOTE, loc, 1, Math.random(), 0, 0, 1);

                // Hiệu ứng "Vuốt Sóng Âm" mỏng xuất hiện ngắt quãng
                if (t % 0.2 < speed) {
                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                }

                t += speed;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}