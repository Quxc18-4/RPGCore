package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;
import java.util.Map;

public class MageClass extends RPGClass {

    // Khai báo quản lý Tốc độ đánh và Tỉ lệ Random cho đòn đánh
    private final java.util.Map<java.util.UUID, Long> nextAttackTime = new java.util.HashMap<>();
    private final java.util.Random random = new java.util.Random();

    public MageClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);

        this.magicPenCap = 80.0;   // Nâng giới hạn Xuyên kháng phép lên 80%

        // [HOOK ÁNH SÁNG]: Đăng ký cơ chế Phơi Nhiễm (+30% DMG cho đòn đánh tiếp theo)
        vn.minecraft.rPGCore.managers.CombatManager.registerCombatModifier(new vn.minecraft.rPGCore.managers.CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (target.hasMetadata("Mage_Exposure_Debuff")) {
                    target.removeMetadata("Mage_Exposure_Debuff", vn.minecraft.rPGCore.RPGCore.getInstance());
                    target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
                    target.getWorld().spawnParticle(org.bukkit.Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                    return 0.3; // +30% Sát thương
                }
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        // [NỘI TẠI]: Trí Tuệ Vượt Trội - Khởi đầu với +25% Xuyên kháng phép
        double currentMagicPen = stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        stats.put("Tỉ lệ xuyên kháng phép", currentMagicPen + 25.0);

        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // Khóa hoàn toàn khả năng chém tay cận chiến của Pháp Sư
        attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✖ Pháp Sư không thể đánh cận chiến!"));
        return false;
    }

    @Override
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, Map<String, Double> petStats) {
        // Đệ tử Bóng Tối hưởng ké 30% xuyên phép
        double ownerMagicPen = rpgOwner.getTotalStatsMap().getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        petStats.put("ArmorPen", petStats.getOrDefault("ArmorPen", 0.0) + (ownerMagicPen * 0.3));
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Hook với kỹ năng Luân chuyển Nguyên tố (Phím F)
        vn.minecraft.rPGCore.skills.ISkill swapSkill = vn.minecraft.rPGCore.RPGCore.getInstance().getSkillManager().getSkill("mage_elemental_swap");
        if (swapSkill != null) {
            return swapSkill.execute(player, rpgPlayer, 1);
        }
        return false;
    }

    // ==========================================
    // SIÊU HÀM: ĐÒN ĐÁNH THƯỜNG CỦA PHÁP SƯ
    // ==========================================
    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // 1. CHẶN SPAM BẰNG TỐC ĐỘ ĐÁNH
        long currentTime = System.currentTimeMillis();
        long allowedTime = nextAttackTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long attackCooldown = (long) (1000 / (1.0 + (totalAtkSpeedBonus / 100.0))); // Mặc định 1 hit/s
        attackCooldown = Math.max(300, attackCooldown); // Cap ở mức 3.33 hit/s
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        // 2. LẤY NGUYÊN TỐ VÀ CHUẨN BỊ BẮN
        MageElement element = vn.minecraft.rPGCore.skills.mage.ElementalSwapSkill.getActiveElement(player);
        vn.minecraft.rPGCore.RPGCore plugin = vn.minecraft.rPGCore.RPGCore.getInstance();
        org.bukkit.Location startLoc = player.getEyeLocation();

        // 3. XỬ LÝ HỆ ÁNH SÁNG (HITSCAN / RAYCAST)
        if (element == MageElement.LIGHT) {
            player.getWorld().playSound(startLoc, org.bukkit.Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
            org.bukkit.util.Vector dir = startLoc.getDirection().normalize().multiply(0.5);
            org.bukkit.Location currentLoc = startLoc.clone();
            LivingEntity hitTarget = null;

            // Bắn tia raycast dài 20 blocks
            for (int i = 0; i < 40; i++) {
                currentLoc.add(dir);
                currentLoc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) break;

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                    if (ent instanceof LivingEntity && ent != player && vn.minecraft.rPGCore.managers.CombatManager.canTarget(player, ent)) {
                        hitTarget = (LivingEntity) ent;
                        break;
                    }
                }
                if (hitTarget != null) break;
            }

            if (hitTarget != null) {
                // 60% DMG + Cắm cờ Phơi Nhiễm
                double damage = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(player, rpgPlayer, hitTarget, 0.6, 0.0, true, false);
                hitTarget.setMetadata("Mage_Exposure_Debuff", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                // Chỉ gắn Ấn (Mark), KHÔNG kích nổ Phản ứng
                vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine.applyMarkOnly(hitTarget, element);

                player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                vn.minecraft.rPGCore.managers.CombatManager.applySkillDamage(player, hitTarget, damage);
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
            return true;
        }

        // 4. XỬ LÝ CÁC HỆ CÒN LẠI (PROJECTILE DẠNG HẠT)
        org.bukkit.util.Vector direction = startLoc.getDirection().normalize().multiply(1.0); // Tốc độ đạn

        // Âm thanh khi vẩy đũa
        player.getWorld().playSound(startLoc, org.bukkit.Sound.ENTITY_ENDER_PEARL_THROW, 0.5f, 1.5f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            org.bukkit.Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 25) { this.cancel(); return; } // Tầm bay ~25 blocks

                currentLoc.add(direction);
                currentLoc.getWorld().spawnParticle(element.getParticle(), currentLoc, 2, 0.1, 0.1, 0.1, 0);

                // Va chạm địa hình (Đặc biệt cho hệ Thiên Nhiên nổ bãi Slowness)
                if (currentLoc.getBlock().getType().isSolid()) {
                    if (element == MageElement.NATURE && random.nextDouble() <= 0.33) {
                        spawnNatureSludge(currentLoc.clone().add(0, 1, 0), player);
                    }
                    this.cancel();
                    return;
                }

                // Va chạm quái
                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && ent != player && vn.minecraft.rPGCore.managers.CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        this.cancel(); // Nổ đạn

                        double dmgMulti = 1.0;
                        boolean isAoE = false;

                        // ==========================================
                        // LOGIC RIÊNG CHO TỪNG NGUYÊN TỐ
                        // ==========================================
                        switch (element) {
                            case FIRE: // 90% DMG, 25% Cháy
                                dmgMulti = 0.9;
                                if (random.nextDouble() <= 0.25) target.setFireTicks(60); // Cháy 3s
                                break;
                            case WATER: // 40% DMG, AoE nhỏ
                                dmgMulti = 0.4;
                                isAoE = true;
                                target.getWorld().spawnParticle(Particle.BUBBLE_POP, target.getLocation(), 20, 1, 0.5, 1, 0.1);
                                break;
                            case WIND: // 70% DMG, Đẩy lùi, 10% Lốc
                                dmgMulti = 0.7;
                                org.bukkit.util.Vector push = currentLoc.getDirection().normalize().multiply(0.5).setY(0.2);
                                target.setVelocity(target.getVelocity().add(push));

                                // Cooldown ẩn 3s cho Lốc Xoáy
                                if (random.nextDouble() <= 0.10 && !player.hasMetadata("Wind_Tornado_CD")) {
                                    player.setMetadata("Wind_Tornado_CD", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                    spawnWindTornado(target.getLocation(), player);
                                    new org.bukkit.scheduler.BukkitRunnable() {
                                        @Override public void run() { player.removeMetadata("Wind_Tornado_CD", plugin); }
                                    }.runTaskLater(plugin, 60L);
                                }
                                break;
                            case NATURE: // 80% DMG, 15% Trói
                                dmgMulti = 0.8;
                                if (random.nextDouble() <= 0.15) {
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 10);
                                    target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
                                }
                                break;
                            case DARK: // 80% DMG, 25% Mù lòa
                                dmgMulti = 0.8;
                                if (random.nextDouble() <= 0.25) {
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0);
                                }
                                break;
                        }

                        // ==========================================
                        // GÂY SÁT THƯƠNG VÀ GẮN ẤN
                        // ==========================================
                        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                        if (isAoE) {
                            for (org.bukkit.entity.Entity aoeEnt : target.getWorld().getNearbyEntities(target.getLocation(), 1.5, 1.5, 1.5)) {
                                if (aoeEnt instanceof LivingEntity && aoeEnt != player && vn.minecraft.rPGCore.managers.CombatManager.canTarget(player, aoeEnt)) {
                                    double aoeDmg = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) aoeEnt, dmgMulti, 0.0, true, false);
                                    vn.minecraft.rPGCore.managers.CombatManager.applySkillDamage(player, (LivingEntity) aoeEnt, aoeDmg);
                                    vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine.applyMarkOnly((LivingEntity) aoeEnt, element);
                                }
                            }
                        } else {
                            double finalDmg = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, true, false);
                            vn.minecraft.rPGCore.managers.CombatManager.applySkillDamage(player, target, finalDmg);
                            vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine.applyMarkOnly(target, element); // GẮN ẤN
                        }

                        player.removeMetadata("Ghost_L_Blocker", plugin);
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true; // Báo cáo tung chiêu thành công để hệ thống trừ độ bền vũ khí
    }

    // --- HÀM HỖ TRỢ HIỆU ỨNG THIÊN NHIÊN (Bãi Lầy) ---
    private void spawnNatureSludge(org.bukkit.Location loc, Player shooter) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 30) { this.cancel(); return; } // Tồn tại 1.5s
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 3, 1.0, 0.1, 1.0, 0);
                if (time % 5 == 0) {
                    for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 1.0, 1.5)) {
                        if (ent instanceof LivingEntity && ent != shooter && vn.minecraft.rPGCore.managers.CombatManager.canTarget(shooter, ent)) {
                            vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect((LivingEntity) ent, org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 1);
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);
    }

    // --- HÀM HỖ TRỢ HIỆU ỨNG GIÓ (Lốc Xoáy) ---
    private void spawnWindTornado(org.bukkit.Location loc, Player shooter) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 40) { this.cancel(); return; } // Tồn tại 2s
                loc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc, 5, 1.5, 0.5, 1.5, 0.05);
                for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                    if (ent instanceof LivingEntity && ent != shooter && vn.minecraft.rPGCore.managers.CombatManager.canTarget(shooter, ent)) {
                        org.bukkit.util.Vector pull = loc.toVector().subtract(ent.getLocation().toVector()).normalize().multiply(0.15);
                        ent.setVelocity(ent.getVelocity().add(pull));
                    }
                }
                time++;
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);
    }
}