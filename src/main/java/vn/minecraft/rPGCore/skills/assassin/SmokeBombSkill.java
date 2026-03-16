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
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class SmokeBombSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private final Map<UUID, Long> stealthEndTimes = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public SmokeBombSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "assassin_smoke_bomb"; }
    @Override public String getName() { return "Bom Khói Ảo Ảnh"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.FIREWORK_STAR; }
    @Override public int getMaxLevel() { return 5; }

    @Override
    public double getManaCost(int level) {
        return 35.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        return (17 - level) * 1000L;
    }

    @Override
    public List<String> getDescription(int level) {
        int duration = 10 + (level * 2);
        return Arrays.asList(
                "§7Ném bom khói xuống chân, lẩn trốn vào bóng đêm.",
                "§7- Trở nên §fTàng Hình Hoàn Toàn §7trong §a" + duration + "s§7.",
                "§7- Có thể kích hoạt lại để §eLàm Mới §7thời gian.",
                "§7- Kẻ địch trong vùng khói bị Bóng tối (Darkness) tạm thời.",
                "§7- Đòn đánh tiếp theo sẽ §cChí Mạng 100%§7 và giải trừ tàng hình.",
                "§c*Lưu ý: Nếu di chuyển, bạn sẽ để lại vết khói trên đầu."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long now = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (now - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), now);

        Location castLoc = player.getLocation();
        player.getWorld().playSound(castLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, castLoc, 100, 2.0, 1.0, 2.0, 0.05);

        for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(castLoc, 4.0, 3.0, 4.0)) {
            if (e instanceof LivingEntity && e != player) {
                StatusEffectManager.applyEffect((LivingEntity)e, PotionEffectType.DARKNESS, 100, 0);
            }
        }

        // XÓA BỎ AGGRO CỦA QUÁI VẬT XUNG QUANH
        for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(castLoc, 20.0, 10.0, 20.0)) {
            if (e instanceof Mob) {
                Mob mob = (Mob) e;
                if (mob.getTarget() != null && mob.getTarget().equals(player)) {
                    mob.setTarget(null); // Tẩy não quái đang đuổi
                }
            }
        }

        long durationMillis = (10 + (level * 2)) * 1000L;
        stealthEndTimes.put(player.getUniqueId(), now + durationMillis);

        if (activeTasks.containsKey(player.getUniqueId())) {
            player.sendMessage("§8§l[!] Làn khói được làm mới. Khôi phục tàng hình!");
            // Đảm bảo cờ Synergy luôn tồn tại nếu làm mới
            player.setMetadata("Assassin_Smoke_Bomb_Active", new FixedMetadataValue(plugin, true));
        } else {
            player.setMetadata("Assassin_Stealth", new FixedMetadataValue(plugin, true));
            // ==========================================
            // [MỚI FIX]: GẮN CỜ SYNERGY CHO GIA TỐC BÓNG ĐÊM ĐỌC
            // ==========================================
            player.setMetadata("Assassin_Smoke_Bomb_Active", new FixedMetadataValue(plugin, true));
            player.sendMessage("§8§l[!] Bạn đã lẩn khuất vào bóng đêm...");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(plugin, player);
            }

            BukkitRunnable stealthTask = new BukkitRunnable() {
                Location lastLoc = player.getLocation();

                @Override
                public void run() {
                    if (!player.isOnline() || !player.hasMetadata("Assassin_Stealth") || System.currentTimeMillis() > stealthEndTimes.getOrDefault(player.getUniqueId(), 0L)) {
                        breakStealth(player);
                        activeTasks.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }

                    Location currentLoc = player.getLocation();
                    if (currentLoc.distanceSquared(lastLoc) > 0.01) {
                        player.getWorld().spawnParticle(Particle.SMOKE, currentLoc.clone().add(0, 2.2, 0), 2, 0.1, 0, 0.1, 0);
                        lastLoc = currentLoc;
                    }
                }
            };
            stealthTask.runTaskTimer(plugin, 0L, 1L);
            activeTasks.put(player.getUniqueId(), stealthTask);
        }

        return true;
    }

    private void breakStealth(Player player) {
        if (player.hasMetadata("Assassin_Stealth") || player.hasMetadata("Assassin_Smoke_Bomb_Active")) {
            player.removeMetadata("Assassin_Stealth", plugin);
            // ==========================================
            // [MỚI FIX]: XÓA CỜ SYNERGY KHI HIỆN HÌNH
            // ==========================================
            player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);

            player.sendMessage("§c§l[!] Bạn đã hiện hình!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }
        }
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
}