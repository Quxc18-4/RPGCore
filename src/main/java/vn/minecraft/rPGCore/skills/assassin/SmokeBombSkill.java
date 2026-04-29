package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.managers.CombatManager; // Import để dùng Modifier
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class SmokeBombSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> stealthEndTimes = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public SmokeBombSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_smoke_bomb"; }
    @Override public String getName() { return "Bom Khói Ảo Ảnh"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.FIREWORK_STAR; }
    @Override public int getMaxLevel() { return 5; }

    @Override public double getManaCost(int level) { return 35.0 + (level * 5.0); }

    // [CẬP NHẬT]: Giảm thời gian hồi chiêu
    @Override public long getCooldown(int level) { return (22 - level) * 1000L; }

    @Override
    public List<String> getDescription(int level) {
        int duration = 12 + (level * 2);
        return Arrays.asList(
                "§7Ném bom khói xuống chân, lẩn trốn vào bóng đêm.",
                "§7- Trở nên §fTàng Hình Hoàn Toàn §7trong §a" + duration + "s§7.",
                "",
                "§b[Nội tại - Áo Choáng Bóng Đêm]:",
                "§7Giảm §c50% sát thương §7nhận vào khi tàng hình.",
                "§7Miễn nhiễm hoàn toàn sát thương từ mũi tên.",
                "",
                "§e[Nội tại - Lẩn Khuất]:",
                "§7Tự động vào trạng thái tàng hình sau 10s không chiến đấu.",
                "",
                "§c*Lưu ý: Việc tung Kỹ năng hoặc đánh trúng mục",
                "§ctiêu sẽ lập tức làm mất trạng thái Tàng hình."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long baseCd = getCooldown(level);
        long finalCd = getFinalCooldown(rpgPlayer, baseCd);

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        // ==========================================
        // [LOGIC RECAST]: THANH TẨY DẤU VẾT
        // ==========================================
        if (player.hasMetadata("Assassin_Stealth")) {
            if (player.hasMetadata("Summoner_Target_Stack")) {
                player.removeMetadata("Summoner_Target_Stack", plugin);
                player.sendMessage("§d§l[!] Làn khói mới đã gột rửa hoàn toàn dấu vết truy đuổi!");
            }
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        applyStealth(player, 12 + level * 2);

        Location castLoc = player.getLocation();
        player.getWorld().playSound(castLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, castLoc, 60, 1.5, 1.0, 1.5, 0.05);

        // Gây mù địch xung quanh dùng StatusEffectManager
        for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(castLoc, 4.0, 3.0, 4.0)) {
            if (e instanceof LivingEntity && e != player) {
                StatusEffectManager.applyEffect((LivingEntity)e, PotionEffectType.DARKNESS, 100, 0);
            }
        }

        return true;
    }

    public void applyStealth(Player player, int durationSeconds) {
        // [AGGRO REMOVAL]: Xóa mục tiêu của quái vật xung quanh
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(30.0, 15.0, 30.0)) {
            if (e instanceof Mob) {
                Mob mob = (Mob) e;
                if (mob.getTarget() != null && mob.getTarget().equals(player)) mob.setTarget(null);
            }
        }

        long durationMillis = durationSeconds * 1000L;
        stealthEndTimes.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);

        if (!player.hasMetadata("Assassin_Stealth")) {
            player.setMetadata("Assassin_Stealth", new FixedMetadataValue(plugin, true));
            player.setMetadata("Assassin_Smoke_Bomb_Active", new FixedMetadataValue(plugin, true));

            for (Player p : Bukkit.getOnlinePlayers()) p.hidePlayer(plugin, player);

            BukkitRunnable stealthTask = new BukkitRunnable() {
                Location lastLoc = player.getLocation();
                int tick = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || !player.hasMetadata("Assassin_Stealth") || System.currentTimeMillis() > stealthEndTimes.getOrDefault(player.getUniqueId(), 0L)) {
                        breakStealth(player);
                        activeTasks.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }

                    Location currentLoc = player.getLocation();
                    // [PARTICLE LOGIC]: Chỉ sinh hạt khi di chuyển & thưa hơn (mỗi 4 ticks)
                    if (currentLoc.distanceSquared(lastLoc) > 0.16) {
                        player.getWorld().spawnParticle(Particle.SMOKE, currentLoc.clone().add(0, 2.2, 0), 1, 0.05, 0, 0.05, 0.01);
                        lastLoc = currentLoc;
                    }
                    tick++;
                }
            };
            stealthTask.runTaskTimer(plugin, 0L, 1L);
            activeTasks.put(player.getUniqueId(), stealthTask);

            // [ĐÃ SỬA BUG]: Lưu Task ID vào Metadata để dọn dẹp khi chuyển phái!
            player.setMetadata("Assassin_Stealth_Task", new FixedMetadataValue(plugin, stealthTask.getTaskId()));
        } else {
            player.sendMessage("§8§o(Thời gian ẩn thân đã được làm mới...)");
        }
    }

    public void breakStealth(Player player) {
        if (player.hasMetadata("Assassin_Stealth")) {
            player.removeMetadata("Assassin_Stealth", plugin);
            player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);

            // [ĐÃ SỬA BUG]: Hủy luôn Task nếu đang chạy để tránh rò rỉ hạt khói
            if (player.hasMetadata("Assassin_Stealth_Task")) {
                int taskId = player.getMetadata("Assassin_Stealth_Task").get(0).asInt();
                Bukkit.getScheduler().cancelTask(taskId);
                player.removeMetadata("Assassin_Stealth_Task", plugin);
            }

            player.sendMessage("§c§l[!] Bạn đã hiện hình!");
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
        }
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
}