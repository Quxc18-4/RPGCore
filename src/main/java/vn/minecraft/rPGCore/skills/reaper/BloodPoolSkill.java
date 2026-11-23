package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BloodPoolSkill extends AbstractEnhancementSkill {

    public BloodPoolSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "reaper_blood_pool"; }
    @Override public String getName() { return "Huyết Độn"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.REDSTONE; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 2.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 24.0 - (level * 1.0);
        return (long) (Math.max(15.0, cd) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);
        int dotDmg = 35 + (displayLvl * 5); // 40% -> 85% ST Vũ khí mỗi 0.5s

        return List.of(
                "§7Tự hòa tan cơ thể thành một Huyết Trì (Vũng",
                "§7máu) di động, trốn tránh thực tại.",
                "",
                "§e[Thi Triển - Huyết Độn]",
                "§f- Tiêu hao §c20% Máu Hiện Tại§f, lặn xuống",
                "§f  vũng máu trong §c3.0 giây§f.",
                "§f- Trong trạng thái này, bản thân được §eTăng 30%",
                "§e  Tốc Độ§f, và được §aMiễn nhiễm sát thương§f.",
                "",
                "§4[Nội tại - Lãnh Địa Khát Máu]",
                "§f- Kẻ địch dẫm lên Huyết Trì sẽ bị §eLàm chậm 30%§f",
                "§f  và chịu §c" + dotDmg + "% ST Vũ Khí§f mỗi 0.5s."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        double currentHp = player.getHealth();
        double hpCost = currentHp * 0.20;
        if (currentHp - hpCost <= 1.0) {
            player.sendMessage("§cKhông đủ Sinh lực để phân rã cơ thể!");
            return false;
        }

        // ==========================================
        // 1. THANH TOÁN TÀI NGUYÊN & ĐỒNG HỒ CDR CHUẨN
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        player.setHealth(currentHp - hpCost);

        // Áp dụng Giảm Hồi Chiêu (CDR) và Notifier chuẩn V2
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_HONEY_BLOCK_SLIDE, 1.0f, 0.5f);
        player.sendMessage("§4§l[!] HUYẾT ĐỘN!");

        // ==========================================
        // 2. ÉP TRẠNG THÁI VÂNISH BUKKIT & UNTARGETABLE
        // ==========================================
        player.setInvulnerable(true);
        player.setMetadata("Reaper_Untargetable", new FixedMetadataValue(plugin, true));

        // Tăng 30% Tốc độ di chuyển chuẩn V2 (Không dùng Potion Speed Vanilla)
        StatusEffectManager.applySpeed(player, 60, 0.30);

        // KÍCH HOẠT BUKKIT VANISH (/v): Ẩn người chơi khỏi tất cả Player khác
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.hidePlayer(plugin, player);
            }
        }

        startBloodPoolTask(player, rpgPlayer, level);
        return true;
    }

    private void startBloodPoolTask(Player player, RPGPlayer rpgPlayer, int level) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_Pool_Task", new FixedMetadataValue(plugin, taskId));

        double dmgMulti = 0.35 + (level * 0.05); // 40% -> 85% ST Vũ khí mỗi 0.5s

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60; // 3 giây = 60 ticks

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() ||
                        (!player.hasMetadata("Reaper_Pool_Task") || player.getMetadata("Reaper_Pool_Task").get(0).asLong() != taskId)) {
                    removePoolState(player);
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    removePoolState(player);
                    player.sendMessage("§8§o(Huyết Độn kết thúc...)");
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 0.1, 0);

                // ==========================================
                // 1. RENDER VŨNG MÁU & HỦY AGGRO MOBS
                // ==========================================
                for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                    if (e instanceof Mob mob) {
                        if (mob.getTarget() != null && mob.getTarget().equals(player)) {
                            mob.setTarget(null);
                        }
                    }
                }

                // Render hạt Vũng Máu
                for (double i = 0; i < 2 * Math.PI; i += Math.PI / 6) {
                    double x = 2.5 * Math.cos(i);
                    double z = 2.5 * Math.sin(i);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.MAROON, 1.5f));
                }
                loc.getWorld().spawnParticle(Particle.DUST, loc, 15, 1.5, 0.1, 1.5, 0, new Particle.DustOptions(Color.RED, 1.2f));

                // ==========================================
                // 2. GÂY SÁT THƯƠNG & HÚT MÁU TỰ ĐỘNG CHUẨN V2
                // ==========================================
                if (ticks % 10 == 0) {
                    for (Entity e : player.getNearbyEntities(2.5, 1.5, 2.5)) {
                        if (e instanceof LivingEntity victim && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            // Làm chậm 30% trong 1s chuẩn V2
                            StatusEffectManager.applySlow(victim, 20, 0.30, 1);

                            // Tính toán sát thương
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, dmgMulti, 0.0, false, false);

                            // Chống lặp sát thương đệ quy
                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));

                            // Tự động gây sát thương VÀ tự nạp Hút Máu chuẩn hệ thống vào Lifesteal_Pool
                            CombatManager.applySkillDamage(player, victim, finalDmg);

                            player.removeMetadata("Ghost_L_Blocker", plugin);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void removePoolState(Player player) {
        player.setInvulnerable(false);
        player.removeMetadata("Reaper_Untargetable", plugin);
        player.removeMetadata("Reaper_Pool_Task", plugin);

        // HIỆN LẠI PLAYER (Hủy Bukkit Vanish)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.showPlayer(plugin, player);
            }
        }
    }
}