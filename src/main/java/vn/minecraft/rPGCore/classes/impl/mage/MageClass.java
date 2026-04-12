package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

        // [HOOK ÁNH SÁNG]: +15% DMG xuyên mục tiêu
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (target.hasMetadata("Mage_Exposure_Debuff")) {
                    target.removeMetadata("Mage_Exposure_Debuff", RPGCore.getInstance());
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
                    target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                    return 0.15; // Nerf xuống 15%
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
    // SIÊU HÀM: ĐÒN ĐÁNH THƯỜNG CỦA PHÁP SƯ V3.0
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

            for (int i = 0; i < 40; i++) {
                currentLoc.add(dir);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) break;

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
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
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        CombatManager.applySkillDamage(player, hitTarget, damage);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (pierceCount >= 3) break;
                    }
                }
                if (pierceCount >= 3) break;
            }
            if (hasHit) DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
            return false; // TRẢ VỀ FALSE ĐỂ CHẶN TRỪ ĐỘ BỀN LẦN 2 TỪ RPGCLASS
        }

        // ==========================================
        // 2. PROJECTILE (LỬA, NƯỚC, GIÓ, THẢO, TỐI)
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

        // KHỞI TẠO "NHÂN" CHO THẢO VÀ TỐI (Thu nhỏ kích thước)
        org.bukkit.entity.ItemDisplay coreDisplay = null;
        if (element == MageElement.NATURE || element == MageElement.DARK) {
            try {
                coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                    ItemStack displayItem = new ItemStack(element == MageElement.NATURE ? Material.MOSS_BLOCK : Material.CRYING_OBSIDIAN);
                    display.setItemStack(displayItem);
                    display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);
                    display.setTeleportDuration(1);

                    // Thu nhỏ 40%
                    org.bukkit.util.Transformation transform = display.getTransformation();
                    transform.getScale().set(0.4f, 0.4f, 0.4f);
                    display.setTransformation(transform);
                });
            } catch(Exception ignored) {}
        }
        final org.bukkit.entity.ItemDisplay finalCore = coreDisplay;

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { this.cancel(); return; }

                currentLoc.add(direction);

                // Cập nhật vị trí "Nhân" đạn xoay vòng
                if (finalCore != null && finalCore.isValid()) {
                    Location displayLoc = currentLoc.clone();
                    displayLoc.setYaw(displayLoc.getYaw() + (ticks * 30));
                    displayLoc.setPitch(displayLoc.getPitch() + (ticks * 30));
                    finalCore.teleport(displayLoc);
                }

                // VẼ ĐẠN & TRAIL NGUYÊN TỐ
                switch (element) {
                    case FIRE:
                        currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 3, 0.1, 0.1, 0.1, 0.02);
                        currentLoc.getWorld().spawnParticle(Particle.SMOKE, currentLoc, 1, 0, 0, 0, 0);
                        break;
                    case WATER:
                        // Chém hình chữ V (Crescent)
                        for (double d = -0.6; d <= 0.6; d += 0.2) {
                            double backOffset = Math.abs(d) * 0.5; // Kéo lùi hạt ở 2 cánh tạo thành chữ V
                            Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(direction.clone().normalize().multiply(backOffset));
                            currentLoc.getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_WATER, pLoc, 1, 0, 0, 0, 0);
                            currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                        }
                        break;
                    case WIND:
                        currentLoc.getWorld().spawnParticle(Particle.GUST, currentLoc, 1, 0, 0, 0, 0);
                        currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 2, 0.1, 0.1, 0.1, 0);
                        break;
                    case NATURE:
                        currentLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                        break;
                    case DARK:
                        currentLoc.getWorld().spawnParticle(Particle.PORTAL, currentLoc, 5, 0.2, 0.2, 0.2, 0);
                        break;
                }

                // VA CHẠM ĐỊA HÌNH
                if (currentLoc.getBlock().getType().isSolid()) {
                    if (element == MageElement.FIRE) {
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                        currentLoc.getWorld().spawnParticle(Particle.EXPLOSION, currentLoc, 1, 0, 0, 0, 0);
                    } else if (element == MageElement.WIND) {
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                    } else if (element == MageElement.NATURE && random.nextDouble() <= 0.33) {
                        spawnNatureVines(currentLoc.clone().add(0, 1, 0), player);
                    }
                    this.cancel();
                    return;
                }

                // VA CHẠM QUÁI
                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        this.cancel();

                        DurabilityManager.damageItem(player, rpgPlayer, weapon, 1); // TRỪ ĐỘ BỀN KHI TRÚNG ĐÍCH

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
                                isAoE = true;
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                                target.getWorld().spawnParticle(Particle.BUBBLE, target.getLocation(), 20, 1, 0.5, 1, 0.1);
                                break;
                            case WIND:
                                dmgMulti = 0.7;
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                                target.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, target.getLocation(), 1, 0, 0, 0, 0);

                                // Ghi đè Knockback sau 1 tick để Override Vanilla Damage Knockback
                                Vector push = currentLoc.getDirection().normalize().multiply(1.2).setY(0.4);
                                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (target.isValid()) target.setVelocity(push);
                                }, 1L);

                                if (random.nextDouble() <= 0.10 && !player.hasMetadata("Wind_Tornado_CD")) {
                                    player.setMetadata("Wind_Tornado_CD", new FixedMetadataValue(plugin, true));
                                    spawnWindTornado(target.getLocation(), player);
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

                        // Gây sát thương & Gắn Ấn
                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        if (isAoE) {
                            for (org.bukkit.entity.Entity aoeEnt : target.getWorld().getNearbyEntities(target.getLocation(), 1.5, 1.5, 1.5)) {
                                if (aoeEnt instanceof LivingEntity && CombatManager.canTarget(player, aoeEnt)) {
                                    double aoeDmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) aoeEnt, dmgMulti, 0.0, true, false);
                                    CombatManager.applySkillDamage(player, (LivingEntity) aoeEnt, aoeDmg);
                                    ReactionEngine.applyMarkOnly((LivingEntity) aoeEnt, element);
                                }
                            }
                        } else {
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            ReactionEngine.applyMarkOnly(target, element);
                        }
                        player.removeMetadata("Ghost_L_Blocker", plugin);
                        return;
                    }
                }
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                if (finalCore != null && finalCore.isValid()) finalCore.remove();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return false; // TRẢ VỀ FALSE CHẶN TRỪ ĐỘ BỀN LÚC VỪA TUNG CHIÊU
    }

    // --- HÀM HỖ TRỢ THIÊN NHIÊN (Bãi Dây Leo Chuẩn Xác Từ Archer) ---
    private void spawnNatureVines(Location loc, Player shooter) {
        Location exactHitLoc = loc.clone();
        while (!exactHitLoc.getBlock().getType().isSolid() && exactHitLoc.getY() > exactHitLoc.getWorld().getMinHeight()) {
            exactHitLoc.subtract(0, 1, 0);
        }
        exactHitLoc.setY(exactHitLoc.getBlockY() + 1.05);

        List<double[]> vines = new java.util.ArrayList<>();
        for (int i = 0; i < 28; i++) {
            double r1 = Math.sqrt(Math.random()) * 3.5, angle1 = Math.random() * Math.PI * 2; // Thu nhỏ bán kính chút so với Skill
            double r2 = Math.sqrt(Math.random()) * 3.5, angle2 = Math.random() * Math.PI * 2;
            vines.add(new double[]{r1 * Math.cos(angle1), r1 * Math.sin(angle1), r2 * Math.cos(angle2), r2 * Math.sin(angle2), 0.2 + (Math.random() * 0.4), Math.random() > 0.5 ? 1.0 : 0.0});
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 30) { this.cancel(); return; } // Tồn tại 1.5s
                Particle.DustOptions rootDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(101, 67, 33), 0.9f);
                Particle.DustOptions groundVineDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 0.8f);
                Particle.DustOptions bindDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.7f);

                for (double[] v : vines) {
                    Particle.DustOptions currentDust = (v[5] == 0.0) ? rootDust : groundVineDust;
                    for (double t = 0; t <= 1; t += 0.2) {
                        exactHitLoc.getWorld().spawnParticle(Particle.DUST, exactHitLoc.clone().add(v[0] + (v[2] - v[0]) * t, 4 * v[4] * t * (1 - t), v[1] + (v[3] - v[1]) * t), 1, currentDust);
                    }
                }

                for (org.bukkit.entity.Entity e : exactHitLoc.getWorld().getNearbyEntities(exactHitLoc, 3.5, 2.0, 3.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                        LivingEntity target = (LivingEntity) e;
                        StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 10);
                        Location entLoc = target.getLocation();
                        double spinSpeed = ticks * 0.4, entRadius = 0.55;
                        for (double y = 0; y <= 0.8; y += 0.2) {
                            entLoc.getWorld().spawnParticle(Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2)), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2))), 1, bindDust);
                            entLoc.getWorld().spawnParticle(Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2) + Math.PI), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2) + Math.PI)), 1, bindDust);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // --- HÀM HỖ TRỢ HIỆU ỨNG GIÓ (ĐÃ FIX BUG x not finite) ---
    private void spawnWindTornado(Location loc, Player shooter) {
        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 40) { this.cancel(); return; }
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 1.5, 0.5, 1.5, 0.05);
                for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) {
                        Vector diff = loc.toVector().subtract(ent.getLocation().toVector());
                        if (diff.lengthSquared() > 0.0001) {
                            Vector pull = diff.normalize().multiply(0.15);
                            ent.setVelocity(ent.getVelocity().add(pull));
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}