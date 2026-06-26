package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class MelodyUtils {

    public static void applyTimedBuff(LivingEntity target, RPGPlayer rpgTarget, String metaKey, long durationTicks, boolean updateSpeed) {
        target.setMetadata(metaKey, new FixedMetadataValue(RPGCore.getInstance(), true));

        if (updateSpeed && rpgTarget != null) {
            rpgTarget.updateMoveSpeed();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid() && target.hasMetadata(metaKey)) {
                    target.removeMetadata(metaKey, RPGCore.getInstance());
                    if (updateSpeed && rpgTarget != null && target instanceof Player && ((Player)target).isOnline()) {
                        rpgTarget.updateMoveSpeed();
                    }
                }
            }
        }.runTaskLater(RPGCore.getInstance(), durationTicks);
    }

    public static void processGlobalPerks(Player bard, RPGPlayer rpgBard, Location center, double radius, double durationTicks) {
        int allyCount = 0;
        boolean hasAfterglow = rpgBard.getSkillData().getSkillLevel("BARD_CORE_1") > 0;
        boolean hasDissonance = rpgBard.getSkillData().getSkillLevel("BARD_CORE_6") > 0;

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) e;
                boolean isAlly = target.equals(bard) || (target instanceof Player && !CombatManager.canTarget(bard, target));

                if (isAlly) {
                    allyCount++;
                    if (hasAfterglow && target instanceof Player) {
                        RPGPlayer rpgTarget = RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
                        if (rpgTarget != null) {
                            rpgTarget.setCurrentResource(rpgTarget.getCurrentResource() + 50.0);
                        }
                    }
                } else {
                    if (hasDissonance) {
                        applyTimedBuff(target, null, BardClass.META_DISSONANCE_DEBUFF, (long) durationTicks, false);
                    }
                }
            }
        }

        if (allyCount >= 4 && rpgBard.getSkillData().getSkillLevel("BARD_CORE_9") > 0) {
            applyTimedBuff(bard, rpgBard, "BARD_ETERNAL_MELODY_TRIGGERED", 100L, false);
            bard.sendMessage("§d§l[Giai Điệu Vĩnh Cửu] §fCộng hưởng hoàn hảo! Giảm 50% hồi chiêu.");
        }
    }

    public static void playArpeggio(Location loc, org.bukkit.Sound sound, float volume, float basePitch) {
        loc.getWorld().playSound(loc, sound, volume, basePitch);
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() { loc.getWorld().playSound(loc, sound, volume, basePitch * 1.25f); }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 2L);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() { loc.getWorld().playSound(loc, sound, volume, basePitch * 1.5f); }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 4L);
    }

    public static void drawExpandingAura(Location center, double radius, org.bukkit.Color color, org.bukkit.Particle themeParticle) {
        org.bukkit.World world = center.getWorld();
        if (world == null) return;

        Location origin = center.clone().add(0, 0.5, 0);
        org.bukkit.Particle.DustTransition dustTrans = new org.bukkit.Particle.DustTransition(color, org.bukkit.Color.WHITE, 1.8f);

        for (double i = 0; i < Math.PI * 2; i += Math.PI / 6) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, center.clone().add(x, 0.1, z), 2, 0.1, 0.1, 0.1, 0, dustTrans);
        }

        if (themeParticle != null) {
            world.spawnParticle(themeParticle, origin, 40, radius / 2, 0.5, radius / 2, 0.05);
        }

        world.spawnParticle(Particle.SWEEP_ATTACK, origin, 5, radius / 3, 0.2, radius / 3, 0);
        world.spawnParticle(Particle.NOTE, center.clone().add(0, 1.5, 0), 12, radius / 1.5, 0.8, radius / 1.5, 1);
    }

    public static void drawTargetFeedback(LivingEntity target, org.bukkit.Particle particle) {
        target.getWorld().spawnParticle(particle, target.getLocation().add(0, 2.2, 0), 8, 0.3, 0.1, 0.3, 0.02);
    }

    // ==========================================
    // KIỂM TRA & SET GLOBAL COOLDOWN CHO GIAI ĐIỆU (12 GIÂY)
    // ==========================================
    public static boolean canTriggerMelody(Player player) {
        if (player.hasMetadata("BARD_MELODY_GCD")) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = player.getMetadata("BARD_MELODY_GCD");
            // [BỌC THÉP]: Kiểm tra list có rỗng không trước khi lấy phần tử
            if (meta != null && !meta.isEmpty()) {
                long expireTime = meta.get(0).asLong();
                return System.currentTimeMillis() >= expireTime;
            }
        }
        return true;
    }

    public static void setMelodyCooldown(Player player) {
        // Khóa Giai Điệu trong 12 giây (12000 ms)
        long expireTime = System.currentTimeMillis() + 12000L;
        player.setMetadata("BARD_MELODY_GCD", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), expireTime));
    }

    // ==========================================
    // TRÌNH PHÁT NHẠC THỜI GIAN THỰC (MUSIC ENGINE)
    // ==========================================
    public static void playMelodySequence(Location loc, org.bukkit.Sound instrument, float volume, float[] pitches, int[] ticks) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int currentNote = 0;
            int currentTick = 0;

            @Override
            public void run() {
                // Đã đánh hết nốt nhạc -> Tắt máy phát
                if (currentNote >= pitches.length) {
                    this.cancel();
                    return;
                }
                // Tới nhịp (tick) được chỉ định -> Đánh nốt nhạc
                if (currentTick == ticks[currentNote]) {
                    loc.getWorld().playSound(loc, instrument, volume, pitches[currentNote]);
                    currentNote++;
                }
                currentTick++;
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);
    }
}