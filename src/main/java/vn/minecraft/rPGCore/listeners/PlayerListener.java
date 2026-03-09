package vn.minecraft.rPGCore.listeners;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;
    private final ClassManager classManager; // Thêm ClassManager

    public PlayerListener(PlayerManager playerManager, BossBarManager bossBarManager, ClassManager classManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
        this.classManager = classManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Gọi hàm Load (Tự check xem là người cũ hay mới)
        RPGPlayer rpgPlayer = playerManager.loadPlayer(player);
        bossBarManager.createBars(player, rpgPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bossBarManager.removeBars(player);
        // Gọi hàm Save (Ghi xuống file và xóa khỏi RAM)
        playerManager.savePlayer(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // 1. Kích hoạt giao tranh
        rpgPlayer.setInCombat();

        // 2. SCALE SÁT THƯƠNG MÔI TRƯỜNG & CHẾT ĐỐI
        EntityDamageEvent.DamageCause cause = event.getCause();
        double multiplier = rpgPlayer.getTotalMaxHealth() / 20.0;

        if (cause == EntityDamageEvent.DamageCause.FALL ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.DROWNING ||
                cause == EntityDamageEvent.DamageCause.STARVATION ||
                cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FREEZE) {

            event.setDamage(event.getDamage() * multiplier);
        }

        // Cập nhật BossBar sau 1 tick để lấy chỉ số máu mới nhất
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                bossBarManager.updateBars(player, rpgPlayer);
            }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getPlugin(vn.minecraft.rPGCore.RPGCore.class), 1L);
    }

    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent event) {
        // 1. KHI NGƯỜI CHƠI ĐÁNH QUÁI (Đưa vào trạng thái Combat)
        if (event.getDamager() instanceof Player) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(event.getDamager().getUniqueId());
            if (rpgPlayer != null) {
                rpgPlayer.setInCombat();
            }
        }

        // 2. KHI NGƯỜI CHƠI BỊ QUÁI ĐÁNH (Tính Né Tránh, Đỡ Đòn)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            RPGPlayer rpgVictim = playerManager.getRPGPlayer(victim.getUniqueId());

            if (rpgVictim != null) {
                rpgVictim.setInCombat(); // Bị đấm cũng tính là combat

                // Lấy lượng sát thương gốc mà quái gây ra
                double incomingDamage = event.getDamage();

                // Trả qua thuật toán Dodge & Block & AGI
                double finalDamage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamageTaken(victim, rpgVictim, incomingDamage);

                // Nếu né thành công (sát thương trả về 0), hủy sự kiện để không bị knockback
                if (finalDamage <= 0) {
                    event.setCancelled(true);
                } else {
                    event.setDamage(finalDamage);
                }
            }
        }
    }

    @EventHandler
    public void onVanillaRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            // Chặn tính năng ăn no tự hồi máu của Minecraft mặc định
            // Để hệ thống tự chạy theo công thức RPG 1% - 3% của bạn
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                    event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onExpGain(org.bukkit.event.player.PlayerExpChangeEvent event) {
        // Chặn nhặt hạt EXP từ quái/MythicMobs
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            int amount = event.getAmount();
            event.setAmount(0); // Chặn vanilla
            rpgPlayer.addExp(amount); // Chuyển sang RPG logic
        }
    }

    @EventHandler
    public void onLevelChange(org.bukkit.event.player.PlayerLevelChangeEvent event) {
        // Nếu người chơi dùng lệnh /xp để tăng level, sự kiện này sẽ chạy.
        // Bạn có thể để người chơi dùng lệnh này để test,
        // nhưng hãy nhớ gọi updateMaxStats() để tránh lỗi crash máu ở trên.
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            rpgPlayer.updateMaxStats();
            bossBarManager.updateBars(player, rpgPlayer);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        // Kiểm tra nếu người chơi có dữ liệu và đạt Level > 20
        if (rpgPlayer != null && player.getLevel() > 20) {
            // Thực hiện trừ 30% EXP hiện có của cấp độ đó
            rpgPlayer.loseDeathExp(0.3);
        }
    }
}