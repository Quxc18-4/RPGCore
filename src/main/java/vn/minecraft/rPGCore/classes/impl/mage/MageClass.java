package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.ElementalSwapSkill;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class MageClass extends RPGClass {

    private final Map<java.util.UUID, Long> nextAttackTime = new java.util.HashMap<>();
    private final Random random = new Random();

    public MageClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
        this.magicPenCap = 80.0;

        // [HOOK ÁNH SÁNG]: +15% DMG xuyên mục tiêu [cite: 35]
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (target.hasMetadata("Mage_Exposure_Debuff")) {
                    target.removeMetadata("Mage_Exposure_Debuff", RPGCore.getInstance());
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
                    target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                    return 0.15; // +15% DMG
                }
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        double currentMagicPen = stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        stats.put("Tỉ lệ xuyên kháng phép", currentMagicPen + 25.0);
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✖ Pháp Sư không thể đánh cận chiến!"));
        return false;
    }

    @Override
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, Map<String, Double> petStats) {
        double ownerMagicPen = rpgOwner.getTotalStatsMap().getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        petStats.put("ArmorPen", petStats.getOrDefault("ArmorPen", 0.0) + (ownerMagicPen * 0.3));
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        vn.minecraft.rPGCore.skills.ISkill swapSkill = RPGCore.getInstance().getSkillManager().getSkill("mage_elemental_swap");
        if (swapSkill != null) {
            return swapSkill.execute(player, rpgPlayer, 1);
        }
        return false;
    }

    // ==========================================
    // SIÊU HÀM: ĐÒN ĐÁNH THƯỜNG CỦA PHÁP SƯ V6.0
    // ==========================================
    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        long currentTime = System.currentTimeMillis();
        long allowedTime = nextAttackTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long attackCooldown = (long) (1000 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        attackCooldown = Math.max(300, attackCooldown);
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();

        // ==========================================
        // 1. ÁNH SÁNG (RAYCAST XUYÊN 3)
        // ==========================================
        if (element == MageElement.LIGHT) {
            player.getWorld().playSound(startLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
            Vector dir = startLoc.getDirection().normalize().multiply(0.5);
            Location currentLoc = startLoc.clone();

            int pierceCount = 0;
            boolean firstHit = true;
            boolean hasHit = false;
            java.util.Set<java.util.UUID> hitSet = new java.util.HashSet<>();

            // [FIX TẦM BAY]: Tăng từ 40 lên 80 (Tầm bắn ~40 blocks)
            for (int i = 0; i < 80; i++) {
                currentLoc.add(dir);

                // Chỉ vẽ tia sáng sau khi đã bay xa mặt 4 blocks (i > 8)
                if (i > 8) {
                    currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);
                }

                if (currentLoc.getBlock().getType().isSolid()) break;

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) { // [cite: 38]
                        LivingEntity hitTarget = (LivingEntity) ent;
                        hitSet.add(hitTarget.getUniqueId());
                        pierceCount++;
                        hasHit = true;

                        hitTarget.getWorld().playSound(hitTarget.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f);
                        double damage = CombatManager.calculateDamage(player, rpgPlayer, hitTarget, 0.6, 0.0, true, false);

                        if (firstHit) {
                            hitTarget.setMetadata("Mage_Exposure_Debuff", new FixedMetadataValue(plugin, true));
                            firstHit = false;
                        }

                        ReactionEngine.applyMarkOnly(hitTarget, element);
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true)); // [cite: 39]
                        CombatManager.applySkillDamage(player, hitTarget, damage); // [cite: 35]
                        player.removeMetadata("Ghost_L_Blocker", plugin); // [cite: 39]

                        if (pierceCount >= 3) break;
                    }
                }
                if (pierceCount >= 3) break;
            }
            if (hasHit) DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
            return false;
        }

        // ==========================================
        // 2. PROJECTILE CÁC HỆ CÒN LẠI
        // ==========================================
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);
        Vector rightVec = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightVec.lengthSquared() < 0.001) rightVec.setX(1);

        // PLAY CAST SOUNDS
        switch (element) {
            case FIRE: player.getWorld().playSound(startLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.5f); break;
            case WATER: player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.2f); break;
            case WIND: player.getWorld().playSound(startLoc, Sound.ENTITY_WIND_CHARGE_THROW, 0.8f, 1.5f); break;
            case NATURE: player.getWorld().playSound(startLoc, Sound.ITEM_CROP_PLANT, 0.8f, 0.8f); break;
            case DARK: player.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 2.0f); break;
        }

        // KHỞI TẠO ITEM DISPLAY CHO HỆ THẢO, TỐI
        org.bukkit.entity.ItemDisplay coreDisplay = null;
        if (element == MageElement.NATURE || element == MageElement.DARK) {
            try {
                coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                    ItemStack displayItem = new ItemStack(element == MageElement.NATURE ? Material.MOSS_BLOCK : Material.CRYING_OBSIDIAN);
                    display.setItemStack(displayItem);
                    display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);
                    display.setTeleportDuration(1);

                    org.bukkit.util.Transformation transform = display.getTransformation();
                    transform.getScale().set(0.4f, 0.4f, 0.4f);
                    display.setTransformation(transform);
                });
            } catch(Exception ignored) {}
        }
        final org.bukkit.entity.ItemDisplay finalCore = coreDisplay;

        // [SIÊU PHẨM]: KHỞI TẠO THỰC THỂ WIND CHARGE 3D GỐC CỦA VANILLA 1.21
        WindCharge realWindCharge = null;
        if (element == MageElement.WIND) {
            try {
                realWindCharge = player.launchProjectile(WindCharge.class, direction);
                realWindCharge.setGravity(false);
                // Khóa sát thương nổ mặc định của Vanilla bằng Metadata
                realWindCharge.setMetadata("Fake_Mage_Wind", new FixedMetadataValue(plugin, true));
            } catch(Exception ignored) {}
        }
        final WindCharge finalWindCore = realWindCharge;

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            Location lastLoc = startLoc.clone();

            java.util.Set<java.util.UUID> hitSet = new java.util.HashSet<>();
            boolean hasDamagedWeapon = false;

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { this.cancel(); return; }

                lastLoc = currentLoc.clone();
                currentLoc.add(direction);

                // Cập nhật Item Display xoay vòng (Thảo, Tối)
                if (finalCore != null && finalCore.isValid()) {
                    Location displayLoc = currentLoc.clone();
                    displayLoc.setYaw(displayLoc.getYaw() + (ticks * 30));
                    displayLoc.setPitch(displayLoc.getPitch() + (ticks * 30));
                    finalCore.teleport(displayLoc);
                }

                // Cập nhật Vị trí Wind Charge
                if (finalWindCore != null && finalWindCore.isValid()) {
                    finalWindCore.teleport(currentLoc);
                }

                // CHỈ VẼ PARTICLE KHI BAY RA KHỎI TẦM MẮT (Sau 3 ticks)
                if (ticks > 3) {
                    switch (element) {
                        case FIRE:
                            // [FIX LỬA]: Làm "mập" quả cầu lửa bằng cách tăng hạt và thêm LAVA/SMOKE
                            currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 6, 0.2, 0.2, 0.2, 0.02);
                            currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                            currentLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                            break;
                        case WATER:
                            // [FIX NƯỚC]: Dùng BUBBLE (Bong bóng) chống trọng lực, vẽ lưỡi liềm mượt
                            for (double d = -0.8; d <= 0.8; d += 0.2) {
                                double backOffset = Math.abs(d) * 0.8;
                                Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(direction.clone().normalize().multiply(backOffset));
                                currentLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 1, 0, 0, 0, 0);
                                currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                            }
                            break;
                        case WIND:
                            // Model 3D đã gánh, chỉ thêm vài hạt mây nhỏ lác đác
                            currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                            break;
                        case NATURE:
                            currentLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                            break;
                        case DARK:
                            // [FIX BÓNG TỐI]: Tăng Step nội suy từ 0.25 lên 0.5 để thanh thoát, bớt đặc sệt
                            double dist = lastLoc.distance(currentLoc);
                            Vector step = direction.clone().normalize().multiply(0.5);
                            Location tempLoc = lastLoc.clone();
                            for (double d = 0; d < dist; d += 0.5) {
                                tempLoc.getWorld().spawnParticle(Particle.PORTAL, tempLoc, 1, 0.05, 0.05, 0.05, 0);
                                tempLoc.getWorld().spawnParticle(Particle.SQUID_INK, tempLoc, 1, 0, 0, 0, 0);
                                tempLoc.add(step);
                            }
                            break;
                    }
                }

                // VA CHẠM ĐỊA HÌNH
                if (currentLoc.getBlock().getType().isSolid()) {
                    if (element == MageElement.FIRE) {
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                        currentLoc.getWorld().spawnParticle(Particle.EXPLOSION, currentLoc, 1, 0, 0, 0, 0);
                    } else if (element == MageElement.WIND) {
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                    } else if (element == MageElement.NATURE && random.nextDouble() <= 0.33) {
                        spawnSimpleNatureSludge(currentLoc.clone().add(0, 1, 0), player);
                    }
                    this.cancel();
                    return;
                }

                // VA CHẠM QUÁI VẬT
                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) { // [cite: 38]
                        LivingEntity target = (LivingEntity) ent;

                        hitSet.add(target.getUniqueId());

                        if (element != MageElement.WATER) {
                            this.cancel();
                        }

                        if (!hasDamagedWeapon) {
                            DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
                            hasDamagedWeapon = true;
                        }

                        double dmgMulti = 1.0;
                        boolean isAoE = false;

                        // HIT SOUNDS & EFFECTS
                        switch (element) {
                            case FIRE:
                                dmgMulti = 0.9;
                                if (random.nextDouble() <= 0.25) target.setFireTicks(60);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                                target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1, 0, 0, 0, 0);
                                break;
                            case WATER:
                                dmgMulti = 0.4;
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                                target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 20, 1, 0.5, 1, 0.1);
                                break;
                            case WIND:
                                dmgMulti = 0.7;
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                                target.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, target.getLocation(), 1, 0, 0, 0, 0);

                                Vector push = currentLoc.getDirection().normalize().multiply(1.2).setY(0.4);
                                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (target.isValid()) target.setVelocity(push);
                                }, 1L);

                                if (random.nextDouble() <= 0.10 && !player.hasMetadata("Wind_Tornado_CD")) {
                                    player.setMetadata("Wind_Tornado_CD", new FixedMetadataValue(plugin, true));
                                    spawnSimpleWindTornado(target.getLocation(), player);
                                    new BukkitRunnable() {
                                        @Override public void run() { player.removeMetadata("Wind_Tornado_CD", plugin); }
                                    }.runTaskLater(plugin, 60L);
                                }
                                break;
                            case NATURE:
                                dmgMulti = 0.8;
                                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
                                if (random.nextDouble() <= 0.15) {
                                    StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 10);
                                    target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0, org.bukkit.Bukkit.createBlockData(Material.OAK_LEAVES));
                                }
                                break;
                            case DARK:
                                dmgMulti = 0.8;
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 0.5f);
                                if (random.nextDouble() <= 0.25) {
                                    StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0);
                                }
                                break;
                        }

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true)); // [cite: 39]
                        if (isAoE) {
                            for (org.bukkit.entity.Entity aoeEnt : target.getWorld().getNearbyEntities(target.getLocation(), 1.5, 1.5, 1.5)) {
                                if (aoeEnt instanceof LivingEntity && CombatManager.canTarget(player, aoeEnt)) { // [cite: 38]
                                    double aoeDmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) aoeEnt, dmgMulti, 0.0, true, false); // [cite: 48]
                                    CombatManager.applySkillDamage(player, (LivingEntity) aoeEnt, aoeDmg); // [cite: 35]
                                    ReactionEngine.applyMarkOnly((LivingEntity) aoeEnt, element);
                                }
                            }
                        } else {
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, true, false); // [cite: 48]
                            CombatManager.applySkillDamage(player, target, finalDmg); // [cite: 35]
                            ReactionEngine.applyMarkOnly(target, element);
                        }
                        player.removeMetadata("Ghost_L_Blocker", plugin); // [cite: 39]

                        if (element != MageElement.WATER) return;
                    }
                }
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                if (finalCore != null && finalCore.isValid()) finalCore.remove();
                if (finalWindCore != null && finalWindCore.isValid()) finalWindCore.remove();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return false;
    }

    // --- HÀM HỖ TRỢ THIÊN NHIÊN (ĐƠN GIẢN HÓA BÃI LÁ LÀM CHẬM) ---
    private void spawnSimpleNatureSludge(Location loc, Player shooter) {
        Location ground = loc.clone();
        while (!ground.getBlock().getType().isSolid() && ground.getY() > ground.getWorld().getMinHeight()) ground.subtract(0, 1, 0);
        ground.add(0, 1.1, 0);

        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 30) { this.cancel(); return; }

                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    Location pLoc = ground.clone().add(Math.cos(angle)*r, 0, Math.sin(angle)*r);
                    ground.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pLoc, 1, org.bukkit.Bukkit.createBlockData(Material.OAK_LEAVES));
                }

                if (time % 5 == 0) {
                    for (org.bukkit.entity.Entity ent : ground.getWorld().getNearbyEntities(ground, 1.5, 1.0, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) { // [cite: 38]
                            StatusEffectManager.applyEffect((LivingEntity) ent, org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 1);
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // --- HÀM HỖ TRỢ HIỆU ỨNG GIÓ (ĐƠN GIẢN HÓA LỐC HÚT) ---
    private void spawnSimpleWindTornado(Location loc, Player shooter) {
        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 20) { this.cancel(); return; }
                loc.getWorld().spawnParticle(Particle.GUST, loc, 3, 1.0, 0.5, 1.0, 0);

                for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 2.0, 2.0, 2.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) { // [cite: 38]
                        Vector diff = loc.toVector().subtract(ent.getLocation().toVector());
                        if (diff.lengthSquared() > 0.0001) {
                            Vector pull = diff.normalize().multiply(0.1); // Kéo nhẹ
                            ent.setVelocity(ent.getVelocity().add(pull));
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}