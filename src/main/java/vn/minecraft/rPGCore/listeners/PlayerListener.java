package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
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

    // ==========================================
    // 1. HÀM KIỂM TRA VŨ KHÍ DÙNG CHUNG (Cực kỳ quan trọng cho Phase 3)
    // ==========================================
    public static boolean isValidWeaponForClass(ItemStack weapon, String classId) {
        if (weapon == null || weapon.getType() == Material.AIR) return false;
        Material mainType = weapon.getType();
        String cId = classId.toLowerCase();

        if (mainType == Material.BOW || mainType == Material.CROSSBOW) return cId.equals("archer");
        if (mainType == Material.SHEARS) return cId.equals("assassin");
        if (mainType == Material.STICK || mainType == Material.BLAZE_ROD) return cId.equals("mage");
        if (mainType == Material.LEAD) return cId.equals("summoner");
        if (mainType.name().endsWith("_HOE")) return cId.equals("reaper");
        if (mainType.name().endsWith("_SWORD") || mainType.name().endsWith("_AXE")) {
            return cId.equals("warrior") || cId.equals("guardian") || cId.equals("commoner");
        }
        return false; // Cầm đất, đá, lúa mì... không tính là vũ khí
    }

    // ==========================================
    // 2. CỔNG ĐIỀU PHỐI CLICK CHUỘT (Delegation)
    // ==========================================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;

        if (player.hasMetadata("Is_Casting")) return;

        // [MỚI THÊM]: Chặn tuyệt đối việc gọi đòn đánh cơ bản (như Bắn cung, Quất roi) khi đang múa Combo
        // [ĐÃ SỬA]: Chặn tuyệt đối đòn đánh cơ bản (LEFT CLICK) khi đang múa Combo.
        // Nhưng CHO PHÉP Chuột Phải đi qua để dùng các kỹ năng đặc biệt của Class (như Unsummon).
        if (player.hasMetadata("Is_Combo_Casting")) {
            if (event.getAction().name().contains("LEFT")) {
                return;
            }
        }

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();

        // Chỉ chuyển giao cho Class nếu đang cầm đúng vũ khí phái mình
        if (isValidWeaponForClass(weapon, rpgPlayer.getCurrentClass().getId())) {
            // Đã dọn dẹp hàm check Equipment ở đây vì Phễu lọc đã lo!

            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
            if (event.getAction().name().contains("LEFT")) {
                rpgPlayer.getCurrentClass().executeLeftClick(player, rpgPlayer, weapon); // Gọi qua execute
            } else if (event.getAction().name().contains("RIGHT")) {
                rpgPlayer.getCurrentClass().executeRightClick(player, rpgPlayer, weapon); // Gọi qua execute
            }
        }
    }

    // ==========================================
    // CỔNG ĐIỀU PHỐI ĐÒN ĐÁNH XA (PROJECTILES)
    // ==========================================
    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            RPGPlayer rpgShooter = playerManager.getRPGPlayer(shooter.getUniqueId());

            if (rpgShooter != null && rpgShooter.getCurrentClass() != null) {
                // Gọi Trưởng phòng tự xử lý mũi tên của mình
                rpgShooter.getCurrentClass().onProjectileHit(shooter, rpgShooter, event.getEntity(), event);
            }
        }
    }

    // KHÓA SÁT THƯƠNG VẬT LÝ GỐC CỦA MŨI TÊN (Chống lỗi dame ảo)
    @EventHandler
    public void onVanillaArrowDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Arrow) {
            org.bukkit.entity.Arrow arrow = (org.bukkit.entity.Arrow) event.getDamager();
            if (arrow.hasMetadata("WeaponType") || arrow.hasMetadata("Is_Custom_Skill") || arrow.hasMetadata("CustomArrowType")) {
                event.setCancelled(true);
            }
        }
    }

    // CHẶN KÉO CUNG VANILLA (Bắt buộc click chuột theo cơ chế RPG)
    @EventHandler
    public void onVanillaShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // 3. CỔNG ĐIỀU PHỐI SÁT THƯƠNG
    // ==========================================
    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent event) {
        // ==========================================
        // XỬ LÝ SÁT THƯƠNG TỪ THÚ TRIỆU HỒI (SUMMONER)
        // ==========================================
        if (event.getDamager().hasMetadata("Summoner_Pet")) {

            // [FIX LỖI]: Lấy danh sách Metadata ra và kiểm tra an toàn
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = event.getDamager().getMetadata("Summoner_Pet");

            // Nếu danh sách rỗng (Do lỗi reload server / chunk) -> Bỏ qua ngay lập tức
            if (metaList == null || metaList.isEmpty()) {
                return;
            }

            // [QUAN TRỌNG]: Hủy đòn đánh vật lý gốc
            event.setCancelled(true);

            // Bây giờ gọi .get(0) mới an toàn 100%
            String ownerUuidStr = metaList.get(0).asString();

            try {
                java.util.UUID ownerUuid = java.util.UUID.fromString(ownerUuidStr);
                RPGPlayer rpgOwner = playerManager.getRPGPlayer(ownerUuid);

                if (rpgOwner != null && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    Player owner = rpgOwner.getBukkitPlayer();

                    double basePetDamage = event.getDamage();

                    // ==========================================
                    // [FIX]: LẤY SÁT THƯƠNG THỰC TẾ TỪ MYTHICMOBS
                    // Bỏ qua giới hạn của Bukkit để lấy đúng Sát thương đã cộng LevelModifiers
                    // ==========================================
                    try {
                        io.lumine.mythic.core.mobs.ActiveMob am = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getActiveMob(event.getDamager().getUniqueId()).orElse(null);
                        if (am != null) {
                            basePetDamage = am.getDamage();
                        }
                    } catch (Exception ignored) {}

                    // Tính toán qua CombatManager như bình thường
                    double finalPetDamage = vn.minecraft.rPGCore.managers.CombatManager.calculatePetDamage(
                            owner, rpgOwner, event.getDamager(), target, basePetDamage
                    );

                    // [BỊT MẮT COMBO LISTENER]: Treo biển báo đang Auto đánh
                    owner.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

                    CombatManager.applySkillDamage(owner, target, finalPetDamage);

                    // Đánh xong gỡ biển báo ngay lập tức
                    owner.removeMetadata("Ghost_L_Blocker", vn.minecraft.rPGCore.RPGCore.getInstance());
                }
            } catch (IllegalArgumentException e) {
                // Đề phòng UUID bị lỗi định dạng
                return;
            }

            return; // Đã xử lý xong
        }
        // Chỉ kích hoạt khi Damager trực tiếp là Player (Đánh cận chiến)
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player attacker = (Player) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();

            // Ngoại lệ: Cho phép sát thương từ Kỹ năng (Skill) được đi qua
            if (attacker.hasMetadata("Skill_Damage_Lock") || attacker.hasMetadata("Skill_Just_Cast") || attacker.hasMetadata("Is_Casting")) {
                if (!attacker.hasMetadata("Skill_Damage_Lock")) event.setCancelled(true);
                return;
            }

            RPGPlayer rpgAttacker = playerManager.getRPGPlayer(attacker.getUniqueId());
            if (rpgAttacker != null) {
                String classId = rpgAttacker.getCurrentClass().getId();
                ItemStack mainItem = attacker.getInventory().getItemInMainHand();

                // ==========================================
                // [MỚI]: KHÓA ĐÁNH THƯỜNG KHI ĐANG BẤM COMBO
                // ==========================================
                if (attacker.hasMetadata("Is_Combo_Casting")) {
                    event.setCancelled(true);
                    return; // Im lặng hủy sát thương để không làm hỏng chuỗi phím
                }

                // Lọc rác vũ khí (Không cho Chiến binh dùng Cung đập quái)
                if (!isValidWeaponForClass(mainItem, classId)) {
                    event.setCancelled(true);
                    return;
                }

                // ==========================================
                // [CHUẨN SOLID]: GIAO QUYỀN QUYẾT ĐỊNH CHO TỪNG CLASS
                // ==========================================
                if (!rpgAttacker.getCurrentClass().performMeleeAttack(attacker, rpgAttacker, target, mainItem)) {
                    event.setCancelled(true);
                    return; // Nếu Class (như Archer/Summoner) trả về false -> Hủy ngay sát thương tay
                }

                EquipmentListener eqListener = new EquipmentListener(playerManager);
                if (!eqListener.canPlayerUse(attacker, mainItem)) {
                    event.setCancelled(true);
                    return;
                }

                rpgAttacker.setInCombat();

                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {

                    // ==========================================
                    // TRƯỞNG PHÒNG NHÚNG TAY: Lấy hệ số nhân sát thương từ Class (Đa hình)
                    // ==========================================
                    double classDmgMultiplier = rpgAttacker.getCurrentClass().onMeleeHit(attacker, rpgAttacker, target, 1.0);

                    boolean isMagicWeapon = rpgAttacker.getStats().maxMagicalDamage > 0;
                    double extraCritRate = 0.0;
                    double extraCritDmg = 0.0;

                    // Fatal Blow (Cường hóa)
                    if (attacker.hasMetadata("Assassin_FatalBlow_Level")) {
                        int fbLevel = attacker.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
                        extraCritRate = 50.0 + ((fbLevel - 1) * 10.0);
                        extraCritDmg = 300.0 + ((fbLevel - 1) * 20.0);

                        attacker.removeMetadata("Assassin_FatalBlow_Level", RPGCore.getInstance());
                        attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
                        attacker.sendMessage("§c§l[!] §fĐòn đánh thường được Cường hóa!");
                    }

                    // TÍNH SÁT THƯƠNG TỔNG
                    double finalDamage = CombatManager.calculateDamage(
                            attacker, rpgAttacker, target, classDmgMultiplier, 0.0, extraCritRate, extraCritDmg, isMagicWeapon, false
                    );

                    event.setDamage(finalDamage);
                    CombatManager.applyLifeSteal(attacker, rpgAttacker, finalDamage, 0.0, isMagicWeapon);
                    DurabilityManager.damageItem(attacker, rpgAttacker, mainItem, 1);
                }
            }
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI BỊ ĐÁNH TRÚNG ---
        // (Phần này giữ nguyên không đổi)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            RPGPlayer rpgVictim = playerManager.getRPGPlayer(victim.getUniqueId());

            if (rpgVictim != null) {
                rpgVictim.setInCombat();

                LivingEntity attackerEntity = event.getDamager() instanceof LivingEntity ? (LivingEntity) event.getDamager() : null;

                double initialDamage = event.getDamage();
                double modifiedDamage = rpgVictim.getCurrentClass().onTakeDamage(victim, rpgVictim, attackerEntity, initialDamage);

                double finalDamage = CombatManager.calculateDamageTaken(victim, rpgVictim, modifiedDamage);

                if (finalDamage <= 0) event.setCancelled(true);
                else event.setDamage(finalDamage);
            }
        }
    }

    // ==========================================
    // 4. QUÁI MẤT MỤC TIÊU KHI TÀNG HÌNH (Dời từ AssassinListener)
    // ==========================================
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (player.hasMetadata("Assassin_Stealth")) {
                event.setCancelled(true);
            }
        }
    }

    // --- CÁC SỰ KIỆN CŨ GIỮ NGUYÊN (Không đổi) ---
    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.loadPlayer(player);
        bossBarManager.createBars(player, rpgPlayer);
    }
    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarManager.removeBars(event.getPlayer());
        playerManager.savePlayer(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onVanillaRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN)) {
            event.setCancelled(true);
        }
    }
    @EventHandler public void onExpGain(org.bukkit.event.player.PlayerExpChangeEvent event) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(event.getPlayer().getUniqueId());
        if (rpgPlayer != null) { rpgPlayer.addExp(event.getAmount()); event.setAmount(0); }
    }
    @EventHandler public void onLevelChange(org.bukkit.event.player.PlayerLevelChangeEvent event) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(event.getPlayer().getUniqueId());
        if (rpgPlayer != null) { rpgPlayer.updateMaxStats(); bossBarManager.updateBars(event.getPlayer(), rpgPlayer); }
    }
    @EventHandler public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(event.getEntity().getUniqueId());
        if (rpgPlayer != null && event.getEntity().getLevel() > 20) rpgPlayer.loseDeathExp(0.3);
    }
    @EventHandler public void onEnvironmentDamage(EntityDamageEvent event) {
        // [Toàn bộ logic môi trường lửa/độc/rơi của bạn giữ nguyên, tôi không sửa gì thêm ở đây]
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
                org.bukkit.potion.PotionEffectType type = (cause == EntityDamageEvent.DamageCause.POISON) ? org.bukkit.potion.PotionEffectType.POISON : org.bukkit.potion.PotionEffectType.WITHER;
                org.bukkit.potion.PotionEffect effect = entity.getPotionEffect(type);
                if (effect != null) event.setDamage(50.0 * Math.pow(2, effect.getAmplifier()));
                else event.setDamage(50.0);
                isCustomDoT = true;
            }
            if (isCustomDoT) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override public void run() { if (entity.isValid() && !entity.isDead()) entity.setNoDamageTicks(0); }
                }.runTaskLater(RPGCore.getInstance(), 1L);
            }
        }
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer == null) return;
            rpgPlayer.setInCombat();
            EntityDamageEvent.DamageCause cause = event.getCause();
            double multiplier = rpgPlayer.getTotalMaxHealth() / 20.0;
            if (cause == EntityDamageEvent.DamageCause.FALL || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.DROWNING || cause == EntityDamageEvent.DamageCause.STARVATION || cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.FREEZE) {
                event.setDamage(event.getDamage() * multiplier);
            }
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() { bossBarManager.updateBars(player, rpgPlayer); }
            }.runTaskLater(RPGCore.getInstance(), 1L);
        }
    }

    // ==========================================
    // CỔNG ĐIỀU PHỐI TƯƠNG TÁC ENTITY (CHẶN DÂY DẮT, MỞ KHO ĐỒ LÀNG...)
    // ==========================================
    @EventHandler
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        String classId = rpgPlayer.getCurrentClass().getId();

        if (isValidWeaponForClass(weapon, classId)) {
            event.setCancelled(true);
            // Báo cáo qua phễu lọc
            rpgPlayer.getCurrentClass().executeRightClick(player, rpgPlayer, weapon);
        }
    }

    // ==========================================
    // CƠ CHẾ: HOÀN TRẢ MANA TỨC THÌ KHI THÚ TRIỆU HỒI TỬ TRẬN
    // ==========================================
    @EventHandler
    public void onPetDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("Summoner_Pet")) {
            // Kiểm tra an toàn
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = event.getEntity().getMetadata("Summoner_Pet");
            if (metaList == null || metaList.isEmpty()) return;

            String ownerUuidStr = metaList.get(0).asString();
            try {
                java.util.UUID ownerUuid = java.util.UUID.fromString(ownerUuidStr);
                RPGPlayer rpgOwner = playerManager.getRPGPlayer(ownerUuid);

                // Trả Mana ngay lập tức (0 giây delay)
                if (rpgOwner != null && rpgOwner.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgOwner.getCurrentClass())
                            .unregisterPet(event.getEntity().getUniqueId(), rpgOwner);
                }
            } catch (Exception ignored) {}
        }
    }
}