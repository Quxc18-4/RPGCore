package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
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

                // [FIX LỖI XUYÊN THẤU]: Nếu là đạn xuyên, KHÔNG ĐƯỢC CANCEL vì sẽ làm đạn bị dội ngược (Bounce)
                if (arrow.hasMetadata("Is_Piercing_Arrow")) {
                    event.setDamage(0.0); // Ép sát thương Vanilla về 0, nhưng vẫn cho phép sự kiện tiếp tục để đạn xuyên qua
                } else {
                    event.setCancelled(true);
                }
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
        // XỬ LÝ SÁT THƯƠNG TỪ THÚ TRIỆU HỒI GÂY RA
        // ==========================================
        if (event.getDamager().hasMetadata("Summoner_Pet")) {
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = event.getDamager().getMetadata("Summoner_Pet");
            if (metaList == null || metaList.isEmpty()) return;

            event.setCancelled(true); // Hủy đòn vật lý gốc
            String ownerUuidStr = metaList.get(0).asString();

            try {
                java.util.UUID ownerUuid = java.util.UUID.fromString(ownerUuidStr);
                RPGPlayer rpgOwner = playerManager.getRPGPlayer(ownerUuid);

                if (rpgOwner != null && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    Player owner = rpgOwner.getBukkitPlayer();

                    double finalPetDamage = vn.minecraft.rPGCore.managers.CombatManager.calculatePetDamage(
                            owner, rpgOwner, event.getDamager(), target, event.getDamage()
                    );

                    // [CHỐNG ĐÁNH ĐÔI]: Chỉ gây sát thương nếu > 0
                    if (finalPetDamage > 0) {
                        owner.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(owner, target, finalPetDamage);
                        owner.removeMetadata("Ghost_L_Blocker", vn.minecraft.rPGCore.RPGCore.getInstance());
                    }
                }
            } catch (IllegalArgumentException e) { return; }
            return;
        }

        // Chỉ kích hoạt khi Damager trực tiếp là Player (Đánh cận chiến, quất roi...)
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player attacker = (Player) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();

            // ==========================================
            // [ĐÃ SỬA]: DÙNG BỘ LỌC CHUẨN (Thay thế đoạn chặn đánh đệ tử cũ)
            // Lọc chính xác địch/ta/Guild
            // ==========================================
            if (!CombatManager.canTarget(attacker, target)) {
                event.setCancelled(true); // Hủy sát thương nếu đánh vào Pet phe mình hoặc người cùng Guild
                return;
            }

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
                    return;
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

                    // [CHUẨN SOLID]: BÁO CÁO ĐÒN ĐÁNH TAY TRÚNG ĐÍCH CHO CLASS XỬ LÝ
                    rpgAttacker.getCurrentClass().onAttackLanded(attacker, target);
                }
            }
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI BỊ ĐÁNH TRÚNG ---
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            RPGPlayer rpgVictim = playerManager.getRPGPlayer(victim.getUniqueId());

            if (rpgVictim != null) {
                rpgVictim.setInCombat();

                LivingEntity attackerEntity = event.getDamager() instanceof LivingEntity ? (LivingEntity) event.getDamager() : null;

                // Mặc định lấy sát thương từ Vanilla (Dành cho quái thường, ngã, lửa...)
                double initialDamage = event.getDamage();

                // ==========================================
                // [FIX BUG]: ÉP LẤY SÁT THƯƠNG ĐÃ SCALE TỪ MYTHICMOBS
                // ==========================================
                if (attackerEntity != null) {
                    try {
                        io.lumine.mythic.core.mobs.ActiveMob am = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getActiveMob(attackerEntity.getUniqueId()).orElse(null);
                        if (am != null) {
                            // Hàm am.getDamage() sẽ trả về Sát thương gốc + (Level * LevelModifiers)
                            // Sau đó chia 2 theo đúng công thức chịu sát thương của Server
                            initialDamage = am.getDamage() / 2.0;
                        }
                    } catch (Exception ignored) {}
                }

                // Chuyển mức sát thương (đã sửa) vào cho Class xử lý (Khiên, Buff...)
                double modifiedDamage = rpgVictim.getCurrentClass().onTakeDamage(victim, rpgVictim, attackerEntity, initialDamage);

                // Đi qua CombatManager để tính Né Tránh / Đỡ đòn
                double finalDamage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamageTaken(victim, rpgVictim, modifiedDamage);

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
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        // [MỚI]: Thu hồi toàn bộ đệ tử trước khi thoát để tránh kẹt Entity và lỗi Mana
        if (rpgPlayer != null && rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
            ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgPlayer.getCurrentClass()).clearAllPets(player);
        }

        bossBarManager.removeBars(player);
        playerManager.savePlayer(player.getUniqueId());
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
    // CƠ CHẾ: HOÀN TRẢ MANA & KÍCH HOẠT HIỆU ỨNG CHẾT
    // ==========================================
    @EventHandler
    public void onPetDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("Summoner_Pet")) {
            LivingEntity pet = event.getEntity();

            // 1. GỌI PROFILE ĐỂ XỬ LÝ HIỆU ỨNG NỔ/CHẾT (CHUẨN SOLID)
            if (pet.hasMetadata("Pet_Type")) {
                String petType = pet.getMetadata("Pet_Type").get(0).asString();
                vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile(petType);

                if (profile != null) {
                    try {
                        java.util.UUID ownerUuid = java.util.UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString());
                        Player owner = org.bukkit.Bukkit.getPlayer(ownerUuid);
                        if (owner != null) profile.onDeath(owner, pet); // Gọi quái tự nổ!
                    } catch (Exception ignored) {}
                }
            }

            // 2. HOÀN TRẢ MANA THẾ CHẤP CHO CHỦ NHÂN
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = pet.getMetadata("Summoner_Pet");
            if (metaList == null || metaList.isEmpty()) return;

            String ownerUuidStr = metaList.get(0).asString();
            try {
                java.util.UUID ownerUuid = java.util.UUID.fromString(ownerUuidStr);
                RPGPlayer rpgOwner = playerManager.getRPGPlayer(ownerUuid);

                if (rpgOwner != null && rpgOwner.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgOwner.getCurrentClass())
                            .unregisterPet(pet.getUniqueId(), rpgOwner);
                }
            } catch (Exception ignored) {}
        }
    }

    @org.bukkit.event.EventHandler
    public void onPetInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        org.bukkit.entity.Entity ent = event.getRightClicked();
        if (ent.hasMetadata("Summoner_Pet") && ent.hasMetadata("Pet_Type")) {
            String ownerId = ent.getMetadata("Summoner_Pet").get(0).asString();

            // Chỉ chủ nhân mới được tương tác
            if (event.getPlayer().getUniqueId().toString().equals(ownerId)) {
                String petType = ent.getMetadata("Pet_Type").get(0).asString();
                vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile(petType);

                if (profile != null && ent instanceof org.bukkit.entity.LivingEntity) {
                    profile.onInteract(event.getPlayer(), (org.bukkit.entity.LivingEntity) ent, event.getPlayer().getInventory().getItemInMainHand());
                    event.setCancelled(true); // Chặn hành động mặc định của Vanilla
                }
            }
        }
    }
}