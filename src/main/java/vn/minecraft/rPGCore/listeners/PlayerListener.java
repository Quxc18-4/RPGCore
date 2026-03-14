package vn.minecraft.rPGCore.listeners;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.entity.LivingEntity; // Đã import thêm
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;
    private final java.util.Map<java.util.UUID, Long> lastFireDamageTimers = new java.util.HashMap<>();

    // ĐÃ XÓA `ClassManager` cho sạch code vì không dùng tới
    public PlayerListener(PlayerManager playerManager, BossBarManager bossBarManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.loadPlayer(player);
        bossBarManager.createBars(player, rpgPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bossBarManager.removeBars(player);
        playerManager.savePlayer(player.getUniqueId());
    }

    // ==========================================
    // SÁT THƯƠNG MÔI TRƯỜNG & TRẠNG THÁI
    // ==========================================
    @EventHandler
    public void onEnvironmentDamage(EntityDamageEvent event) {
        // 1. ÁP DỤNG CỐ ĐỊNH CHO TẤT CẢ SINH VẬT (NGƯỜI CHƠI VÀ QUÁI VẬT)
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            EntityDamageEvent.DamageCause cause = event.getCause();

            boolean isCustomDoT = false; // Cờ đánh dấu Sát thương theo thời gian (Damage over Time)

            // Xử lý Sát thương Lửa (Cháy trên người hoặc đứng trong lửa)
            if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                long now = System.currentTimeMillis();
                long lastDmgTime = lastFireDamageTimers.getOrDefault(entity.getUniqueId(), 0L);

                // Nếu chưa trôi qua 0.5 giây (500ms) kể từ lần cháy trước -> HỦY BỎ
                if (now - lastDmgTime < 500) {
                    event.setCancelled(true);
                    return;
                }

                // Cập nhật lại thời gian cháy và áp dụng sát thương
                lastFireDamageTimers.put(entity.getUniqueId(), now);
                event.setDamage(75.0);
                isCustomDoT = true;
            }
            // Xử lý Sát thương Độc và Héo mòn (Nhân đôi theo từng cấp độ)
            else if (cause == EntityDamageEvent.DamageCause.POISON || cause == EntityDamageEvent.DamageCause.WITHER) {
                org.bukkit.potion.PotionEffectType type = (cause == EntityDamageEvent.DamageCause.POISON) ?
                        org.bukkit.potion.PotionEffectType.POISON : org.bukkit.potion.PotionEffectType.WITHER;

                org.bukkit.potion.PotionEffect effect = entity.getPotionEffect(type);
                if (effect != null) {
                    int level = effect.getAmplifier(); // 0 = Cấp 1, 1 = Cấp 2
                    event.setDamage(50.0 * Math.pow(2, level)); // Độc 1: 50, Độc 2: 100, Độc 3: 200
                } else {
                    event.setDamage(50.0); // Mặc định đề phòng lỗi không check được Potion
                }
                isCustomDoT = true;
            }

            // [MỚI] XÓA I-FRAME NGAY LẬP TỨC ĐỂ KHÔNG CHẶN ĐÒN ĐÁNH CỦA NGƯỜI CHƠI
            if (isCustomDoT) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (entity.isValid() && !entity.isDead()) {
                            entity.setNoDamageTicks(0);
                        }
                    }
                }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
            }
        }

        // 2. CHỈ ÁP DỤNG CHO NGƯỜI CHƠI (Scale theo Max HP và kích hoạt Combat)
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer == null) return;

            // Kích hoạt trạng thái giao tranh
            rpgPlayer.setInCombat();

            // Scale sát thương môi trường khác (Đã loại bỏ FIRE, POISON, WITHER ra khỏi danh sách này)
            EntityDamageEvent.DamageCause cause = event.getCause();
            double multiplier = rpgPlayer.getTotalMaxHealth() / 20.0;

            if (cause == EntityDamageEvent.DamageCause.FALL ||
                    cause == EntityDamageEvent.DamageCause.LAVA ||
                    cause == EntityDamageEvent.DamageCause.DROWNING ||
                    cause == EntityDamageEvent.DamageCause.STARVATION ||
                    cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    cause == EntityDamageEvent.DamageCause.FREEZE) {

                event.setDamage(event.getDamage() * multiplier);
            }

            // Cập nhật BossBar sau 1 tick để lấy chỉ số máu mới nhất
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    bossBarManager.updateBars(player, rpgPlayer);
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
        }
    }

    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(event.getDamager().getUniqueId());
            if (rpgPlayer != null) {
                rpgPlayer.setInCombat();
            }
        }

        if (event.getEntity() instanceof Player victim) {
            RPGPlayer rpgVictim = playerManager.getRPGPlayer(victim.getUniqueId());

            if (rpgVictim != null) {
                rpgVictim.setInCombat();

                double incomingDamage = event.getDamage();
                double finalDamage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamageTaken(victim, rpgVictim, incomingDamage);

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
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                    event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onExpGain(org.bukkit.event.player.PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            int amount = event.getAmount();
            event.setAmount(0);
            rpgPlayer.addExp(amount);
        }
    }

    @EventHandler
    public void onLevelChange(org.bukkit.event.player.PlayerLevelChangeEvent event) {
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

        if (rpgPlayer != null && player.getLevel() > 20) {
            rpgPlayer.loseDeathExp(0.3);
        }
    }
}