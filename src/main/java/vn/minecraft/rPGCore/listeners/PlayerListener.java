package vn.minecraft.rPGCore.listeners;

import org.bukkit.Particle;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.entity.LivingEntity;
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

    @EventHandler
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            EntityDamageEvent.DamageCause cause = event.getCause();

            boolean isCustomDoT = false;

            if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                long now = System.currentTimeMillis();
                long lastDmgTime = lastFireDamageTimers.getOrDefault(entity.getUniqueId(), 0L);
                if (now - lastDmgTime < 500) { event.setCancelled(true); return; }
                lastFireDamageTimers.put(entity.getUniqueId(), now);
                event.setDamage(75.0);
                isCustomDoT = true;
            }
            else if (cause == EntityDamageEvent.DamageCause.POISON || cause == EntityDamageEvent.DamageCause.WITHER) {
                org.bukkit.potion.PotionEffectType type = (cause == EntityDamageEvent.DamageCause.POISON) ?
                        org.bukkit.potion.PotionEffectType.POISON : org.bukkit.potion.PotionEffectType.WITHER;
                org.bukkit.potion.PotionEffect effect = entity.getPotionEffect(type);
                if (effect != null) event.setDamage(50.0 * Math.pow(2, effect.getAmplifier()));
                else event.setDamage(50.0);
                isCustomDoT = true;
            }

            if (isCustomDoT) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override public void run() { if (entity.isValid() && !entity.isDead()) entity.setNoDamageTicks(0); }
                }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
            }
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer == null) return;
            rpgPlayer.setInCombat();

            EntityDamageEvent.DamageCause cause = event.getCause();
            double multiplier = rpgPlayer.getTotalMaxHealth() / 20.0;
            if (cause == EntityDamageEvent.DamageCause.FALL || cause == EntityDamageEvent.DamageCause.LAVA ||
                    cause == EntityDamageEvent.DamageCause.DROWNING || cause == EntityDamageEvent.DamageCause.STARVATION ||
                    cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.FREEZE) {
                event.setDamage(event.getDamage() * multiplier);
            }

            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() { bossBarManager.updateBars(player, rpgPlayer); }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
        }
    }

    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player attacker = (Player) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();

            if (attacker.hasMetadata("Skill_Damage_Lock")) {
                return;
            }
            if (attacker.hasMetadata("Skill_Just_Cast") || attacker.hasMetadata("Is_Casting")) {
                event.setCancelled(true);
                return;
            }

            RPGPlayer rpgAttacker = playerManager.getRPGPlayer(attacker.getUniqueId());
            if (rpgAttacker != null) {

                // ==========================================
                // BẮT ĐẦU CHECK VŨ KHÍ TẠI ĐÂY
                // ==========================================
                org.bukkit.inventory.ItemStack mainItem = attacker.getInventory().getItemInMainHand();
                org.bukkit.Material mainType = mainItem.getType();
                String classId = rpgAttacker.getCurrentClass().getId().toLowerCase();

                String requiredClass = null;
                boolean isWeapon = false;

                if (mainType == org.bukkit.Material.BOW || mainType == org.bukkit.Material.CROSSBOW) {
                    isWeapon = true;
                    if (!classId.equals("archer")) requiredClass = "Cung Thủ (Archer)";
                } else if (mainType == org.bukkit.Material.SHEARS) {
                    isWeapon = true;
                    if (!classId.equals("assassin")) requiredClass = "Sát Thủ (Assassin)";
                } else if (mainType == org.bukkit.Material.STICK || mainType == org.bukkit.Material.BLAZE_ROD) {
                    isWeapon = true;
                    if (!classId.equals("mage")) requiredClass = "Pháp Sư (Mage)";
                } else if (mainType == org.bukkit.Material.LEAD) {
                    isWeapon = true;
                    if (!classId.equals("summoner")) requiredClass = "Triệu Hồi Sư (Summoner)";
                } else if (mainType.name().endsWith("_HOE")) {
                    isWeapon = true;
                    if (!classId.equals("reaper")) requiredClass = "Tử Thần (Reaper)";
                } else if (mainType.name().endsWith("_SWORD") || mainType.name().endsWith("_AXE")) {
                    isWeapon = true;
                    if (!classId.equals("warrior") && !classId.equals("guardian") && !classId.equals("commoner")) {
                        requiredClass = "Chiến Binh / Vệ Binh";
                    }
                }

                // Nếu cầm vũ khí (không phải tay không/block) mà sai phái -> chặn
                if (isWeapon && requiredClass != null) {
                    event.setCancelled(true);
                    attacker.sendMessage("§c§l[!] §cVũ khí này chỉ dành cho hệ phái " + requiredClass + "!");
                    return;
                }

                // Gọi EquipmentListener kiểm tra Lore (Level)
                EquipmentListener eqListener = new EquipmentListener(playerManager);
                if (!eqListener.canPlayerUse(attacker, mainItem)) {
                    event.setCancelled(true);
                    return;
                }

                rpgAttacker.setInCombat();

                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {

                    boolean isMagicWeapon = rpgAttacker.getStats().maxMagicalDamage > 0;
                    double extraCritRate = 0.0;
                    double extraCritDmg = 0.0;

                    if (attacker.hasMetadata("Assassin_FatalBlow_Level")) {
                        int fbLevel = attacker.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
                        extraCritRate = 50.0 + ((fbLevel - 1) * 10.0);
                        extraCritDmg = 300.0 + ((fbLevel - 1) * 20.0);

                        attacker.removeMetadata("Assassin_FatalBlow_Level", vn.minecraft.rPGCore.RPGCore.getInstance());
                        attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
                        attacker.sendMessage("§c§l[!] §fĐòn đánh thường được Cường hóa!");
                    }

                    double finalDamage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(
                            attacker, rpgAttacker, target, 1.0, 0.0, extraCritRate, extraCritDmg, isMagicWeapon, false
                    );

                    event.setDamage(finalDamage);
                    vn.minecraft.rPGCore.managers.CombatManager.applyLifeSteal(attacker, rpgAttacker, finalDamage, 0.0, isMagicWeapon);
                    vn.minecraft.rPGCore.managers.DurabilityManager.damageItem(attacker, rpgAttacker, mainItem, 1);
                }
            }
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            RPGPlayer rpgVictim = playerManager.getRPGPlayer(victim.getUniqueId());

            if (rpgVictim != null) {
                rpgVictim.setInCombat();
                double incomingDamage = event.getDamage();
                double finalDamage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamageTaken(victim, rpgVictim, incomingDamage);

                if (finalDamage <= 0) event.setCancelled(true);
                else event.setDamage(finalDamage);
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