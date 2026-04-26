package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;

public class CombatManager {

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, boolean isMagic, boolean isCrossbow) {
        return calculateDamage(attacker, rpgPlayer, target, dmgMultiplier, extraArmorPen, 0.0, 0.0, isMagic, isCrossbow);
    }

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, double extraCritRate, boolean isMagic, boolean isCrossbow) {
        return calculateDamage(attacker, rpgPlayer, target, dmgMultiplier, extraArmorPen, extraCritRate, 0.0, isMagic, isCrossbow);
    }

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, double extraCritRate, double extraCritDamage, boolean isMagic, boolean isCrossbow) {

        Map<String, Double> combatStats = new java.util.HashMap<>(rpgPlayer.getTotalStatsMap());

        // Đưa các chỉ số bổ sung cơ bản từ Kỹ năng vào Map
        combatStats.put("Tỉ lệ chí mạng", combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0) + extraCritRate);
        combatStats.put("Sát thương chí mạng", combatStats.getOrDefault("Sát thương chí mạng", 50.0) + extraCritDamage);
        combatStats.put("Tỉ lệ xuyên giáp", combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + extraArmorPen);

        // ==========================================
        // 2. [CHUẨN SOLID]: GỌI CLASS TỰ SỬA ĐỔI CHỈ SỐ LINH HOẠT
        // ==========================================
        if (rpgPlayer.getCurrentClass() != null) {
            rpgPlayer.getCurrentClass().applyDynamicCombatStats(attacker, target, combatStats);
        }

        // NỘI TẠI CLASS CHUYỂN HÓA CRIT & ÉP CAP VỀ MỨC GIỚI HẠN
        if (rpgPlayer.getCurrentClass() != null) {
            rpgPlayer.getCurrentClass().applyOffensivePassives(combatStats);
        }

        // Lấy lại dữ liệu "Thành phẩm" sau khi Class đã nhào nặn xong
        double finalCritRate = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0);
        double finalCritDamage = combatStats.getOrDefault("Sát thương chí mạng", 50.0);
        double finalArmorPen = combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0);
        double finalMagicPen = combatStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);

        // ==========================================
        // 5. TÍNH TOÁN SÁT THƯƠNG THỰC TẾ
        // ==========================================
        double minDmg = combatStats.getOrDefault(isMagic ? "Sát thương phép thuật (Nhỏ nhất)" : "Sát thương vật lý (Nhỏ nhất)", 5.0);
        double maxDmg = combatStats.getOrDefault(isMagic ? "Sát thương phép thuật (Lớn nhất)" : "Sát thương vật lý (Lớn nhất)", 10.0);

        double rawDamage = minDmg + (Math.random() * (maxDmg - minDmg));
        if (isCrossbow) rawDamage *= 1.55;

        if ((Math.random() * 100) <= finalCritRate) {
            rawDamage *= (1.0 + (finalCritDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }

        double targetDefense = 0;
        if (isMagic && target instanceof Player) {
            RPGPlayer targetRpg = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (targetRpg != null) targetDefense = targetRpg.getTotalStatsMap().getOrDefault("Kháng phép", 0.0);
        } else if (!isMagic) {
            AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
            if (armorAttr != null) targetDefense = armorAttr.getValue();
        }

        // [LỚP 1 - CHUẨN SOLID]: Áp dụng Phá Giáp Toàn Cục TRƯỚC
        double totalReductionPct = 0.0;
        for (ICombatModifier modifier : combatModifiers) {
            if (isMagic) {
                totalReductionPct += modifier.getTargetMagicResistReductionPercentage(target);
            } else {
                totalReductionPct += modifier.getTargetArmorReductionPercentage(target);
            }
        }
        if (totalReductionPct > 0) {
            totalReductionPct = Math.min(1.0, totalReductionPct);
            targetDefense *= (1.0 - totalReductionPct);
        }

        // [LỚP 2]: Xuyên Giáp Cá Nhân Của Người Chơi Áp dụng SAU
        double penToUse = isMagic ? finalMagicPen : finalArmorPen;
        targetDefense *= (1.0 - (penToUse / 100.0));

        // Tính sát thương cuối cùng
        double finalDamage = Math.max(1.0, (rawDamage - targetDefense) * dmgMultiplier);

        // Code Dummy Test
        if (target.getCustomName() != null && ChatColor.stripColor(target.getCustomName()).contains("Dummy")) {
            int currentStacks = attacker.hasMetadata("Assassin_KillingIntent_SubStacks") ? attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() / 3 : 0;
            attacker.sendMessage("§e§l[DUMMY] §8[Stack Sát Ý: " + currentStacks + "] §fSát thương: §c" + String.format("%.1f", finalDamage) + " ⚔");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isValid() && !target.isDead()) {
                        AttributeInstance maxHp = target.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHp != null) target.setHealth(maxHp.getValue());
                        target.setFireTicks(0);
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
        }

        return finalDamage;
    }

    public static double calculateDamageTaken(Player victim, RPGPlayer rpgVictim, double incomingDamage) {
        Map<String, Double> totalStats = rpgVictim.getTotalStatsMap();

        // ==========================================
        // LỚP 0: NÉ TRÁNH (Dodge)
        // Ưu tiên cao nhất: Nếu né thành công, hủy toàn bộ sát thương, không cần tính tiếp
        // ==========================================
        double dodgeRate = totalStats.getOrDefault("Tỉ lệ né tránh", 0.0);
        if (dodgeRate > 0 && (Math.random() * 100) <= dodgeRate) {
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l[NÉ THÀNH CÔNG]"));
            return 0.0;
        }

        // ==========================================
        // LỚP 1: CLASS HOOK (Đặc quyền riêng của Class)
        // [CHUẨN SOLID]: Gọi về class tương ứng (Ví dụ: WarriorClass sẽ tự kiểm tra Parry và /2 sát thương ở đây)
        // ==========================================
        double damageAfterClassHook = incomingDamage;
        if (rpgVictim.getCurrentClass() != null) {
            // Class sẽ nhận sát thương gốc, xử lý đặc quyền, và trả về sát thương sau khi giảm
            damageAfterClassHook = rpgVictim.getCurrentClass().onTakeDamage(victim, rpgVictim, incomingDamage);
        }

        // ==========================================
        // LỚP 2: HỆ THỐNG MODIFIER CHUNG (Thiết quân luật Cap 40%)
        // Gom tất cả các buff từ đồng minh, pet, lãnh vực...
        // ==========================================
        double totalReduction = 0.0;
        for (ICombatModifier modifier : combatModifiers) {
            totalReduction += modifier.getIncomingDamageReduction(victim, null);
        }

        // Khóa giới hạn: Tổng giảm sát thương không bao giờ vượt quá 40% (0.40)
        totalReduction = Math.min(totalReduction, 0.40);

        // Áp dụng giảm trừ vào lượng sát thương đã đi qua Lớp 1
        double finalDamage = damageAfterClassHook * (1.0 - totalReduction);

        // ==========================================
        // LỚP 3: ĐỠ ĐÒN (Chỉ số tĩnh từ Trang bị/Vũ khí)
        // ==========================================
        double blockRate = totalStats.getOrDefault("Tỉ lệ đỡ đòn", 0.0);
        if (blockRate > 0 && (Math.random() * 100) <= blockRate) {
            victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.SHIELD));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            finalDamage *= 0.7; // Giảm 30% sát thương
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§9§l[ĐỠ ĐÒN: -30% SÁT THƯƠNG]"));
        }

        // ==========================================
        // LỚP 4: [CHUẨN SOLID] HỆ THỐNG KHIÊN ẢO DÙNG CHUNG (Absorption)
        // ==========================================
        if (victim.hasMetadata("RPG_Absorption_Shield")) {
            double currentShield = victim.getMetadata("RPG_Absorption_Shield").get(0).asDouble();
            double maxShield = victim.getMetadata("RPG_Absorption_Shield_Max").get(0).asDouble();

            if (currentShield > 0) {
                if (finalDamage <= currentShield) {
                    currentShield -= finalDamage;
                    finalDamage = 0;
                    victim.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), currentShield));
                } else {
                    finalDamage -= currentShield;
                    currentShield = 0;
                    victim.removeMetadata("RPG_Absorption_Shield", vn.minecraft.rPGCore.RPGCore.getInstance());
                    victim.removeMetadata("RPG_Absorption_Shield_Max", vn.minecraft.rPGCore.RPGCore.getInstance());
                    victim.setAbsorptionAmount(0);
                    victim.playSound(victim.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BREAK, 1.0f, 1.2f);
                }

                if (currentShield > 0) {
                    double displayAbsorption = (currentShield / maxShield) * 20.0;
                    victim.setAbsorptionAmount(Math.max(1.0, displayAbsorption));
                }
            }
        }

        return finalDamage;
    }

    // ==========================================
    // MÁY ÉP HÚT MÁU 12 TẦNG (12-TIER LIFESTEAL COMPRESSOR)
    // Thêm tham số boolean isSkill vào cuối
    // ==========================================
    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct, boolean isMagic, boolean isSkill) {
        if (attacker.hasMetadata("Status_AntiHeal")) {
            long expire = attacker.getMetadata("Status_AntiHeal").get(0).asLong();
            if (System.currentTimeMillis() < expire) return;
        }

        double finalExtraLifesteal = extraLifeStealPct;

        if (rpgPlayer.getCurrentClass() != null) {
            finalExtraLifesteal += rpgPlayer.getCurrentClass().getDynamicLifeSteal(attacker, rpgPlayer);
        }

        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();
        String stealType = isMagic ? "Tỉ lệ hút máu phép" : "Tỉ lệ hút máu";
        double totalLifeSteal = totalStats.getOrDefault(stealType, 0.0) + finalExtraLifesteal;

        if (totalLifeSteal <= 0) return;

        double effectiveDamage = 0;
        double remaining = damageDealt;

        // Mảng 11 mốc đầu (Đạt đỉnh 1 Triệu sát thương)
        double[][] tiers = {
                {2000, 1.00},   // Mốc 1: 0 - 2k
                {3000, 0.85},   // Mốc 2: 2k - 5k (Size: 3k)
                {5000, 0.70},   // Mốc 3: 5k - 10k (Size: 5k)
                {10000, 0.55},  // Mốc 4: 10k - 20k (Size: 10k)
                {20000, 0.40},  // Mốc 5: 20k - 40k (Size: 20k)
                {30000, 0.25},  // Mốc 6: 40k - 70k (Size: 30k)
                {50000, 0.15},  // Mốc 7: 70k - 120k (Size: 50k)
                {80000, 0.10},  // Mốc 8: 120k - 200k (Size: 80k)
                {150000, 0.06}, // Mốc 9: 200k - 350k (Size: 150k)
                {250000, 0.03}, // Mốc 10: 350k - 600k (Size: 250k)
                {400000, 0.01}  // Mốc 11: 600k - 1M (Size: 400k)
        };

        for (double[] tier : tiers) {
            if (remaining <= 0) break;
            double chunk = Math.min(remaining, tier[0]);
            effectiveDamage += chunk * tier[1];
            remaining -= chunk;
        }

        // Mốc 12: Trên 1 Triệu sát thương
        if (remaining > 0) {
            effectiveDamage += remaining * 0.002; // Chỉ còn 0.2%
        }

        double theoreticalHeal = effectiveDamage * (totalLifeSteal / 100.0);

        // Kỹ năng bị bóp nghẹt chỉ còn 10% hiệu suất
        if (isSkill) theoreticalHeal *= 0.10;

        if (theoreticalHeal <= 0) return;

        // CỘNG DỒN VÀO HỒ CHỨA MÁU (POOL) THAY VÌ HỒI TRỰC TIẾP
        double currentPool = 0.0;
        if (attacker.hasMetadata("Lifesteal_Pool")) {
            currentPool = attacker.getMetadata("Lifesteal_Pool").get(0).asDouble();
        }
        attacker.setMetadata("Lifesteal_Pool", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), currentPool + theoreticalHeal));
    }

    // ==========================================
    // HỆ THỐNG GÂY SÁT THƯƠNG KỸ NĂNG (DÙNG CHUNG CHO MỌI CLASS)
    // ==========================================
    public static void applySkillDamage(Player attacker, LivingEntity target, double finalDamage) {
        target.setNoDamageTicks(0);
        attacker.setMetadata("Skill_Damage_Lock", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
        target.damage(finalDamage, attacker);
        attacker.removeMetadata("Skill_Damage_Lock", RPGCore.getInstance());

        // ==========================================
        // [CẢM BIẾN GIAO TRANH SỐ 1]: KỸ NĂNG & THÚ TRIỆU HỒI
        // ==========================================
        RPGPlayer rpgAttacker = RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
        if (rpgAttacker != null) {
            // Chỉ cần Kỹ năng hoặc Đệ tử gây sát thương -> Bật đồng hồ cho Chủ nhân!
            rpgAttacker.setInCombat();
            applyLifeSteal(attacker, rpgAttacker, finalDamage, 0.0, false, true);
        }

        // Bật đồng hồ cho Nạn nhân (Nếu nạn nhân là Player)
        if (target instanceof Player) {
            RPGPlayer rpgVictim = RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (rpgVictim != null) {
                rpgVictim.setInCombat();
            }
        }
    }

    // ==========================================
    // HỆ THỐNG TÍNH TOÁN SÁT THƯƠNG CHO THÚ TRIỆU HỒI
    // ==========================================
    public static double calculatePetDamage(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, LivingEntity target, double eventDamage) {
        // 1. LẤY THÔNG TIN PROFILE
        String petType = pet.hasMetadata("Pet_Type") ? pet.getMetadata("Pet_Type").get(0).asString() : "Unknown";
        vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile(petType);
        int skillLevel = pet.hasMetadata("Summon_Level") ? pet.getMetadata("Summon_Level").get(0).asInt() : 1;

        if (profile == null) return 1.0;

        Map<String, Double> playerStats = rpgOwner.getTotalStatsMap();

        // 2. TÍNH TOÁN SÁT THƯƠNG GỐC THEO PROFILE
        double minMagic = playerStats.getOrDefault("Sát thương phép thuật (Nhỏ nhất)", 5.0);
        double maxMagic = playerStats.getOrDefault("Sát thương phép thuật (Lớn nhất)", 10.0);
        double avgMasterDamage = (minMagic + maxMagic) / 2.0;

        double flatBase = profile.getBaseFlatDamage(skillLevel);
        double scaling = profile.getWeaponScaling(skillLevel);
        double finalBasePetDamage = flatBase + (avgMasterDamage * 0.55 * scaling);

        // Áp dụng Bội số Dung hợp (Nếu có)
        if (pet.hasMetadata("Fusion_Multiplier")) {
            finalBasePetDamage *= pet.getMetadata("Fusion_Multiplier").get(0).asDouble();
        }

        // Lưu Snapshot lại cho hàm onDeath nổ
        pet.setMetadata("Base_Damage_Snapshot", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), finalBasePetDamage));

        // 3. XỬ LÝ KỸ NĂNG VÀ PHÂN LOẠI
        boolean isMagicPet = profile.isMagicDamage();
        double skillMultiplier = profile.onAttack(owner, (LivingEntity) pet, target, finalBasePetDamage);

        // 4. ĐẾM THÚ VÀ CHIA SẺ SUB-STATS
        int petCount = 0;
        for (org.bukkit.entity.Entity e : owner.getNearbyEntities(30, 20, 30)) {
            if (e.hasMetadata("Summoner_Pet") && e.getMetadata("Summoner_Pet").get(0).asString().equals(owner.getUniqueId().toString())) {
                petCount++;
            }
        }
        petCount = Math.max(1, petCount);

        // ==========================================
        // [CHUẨN SOLID] 4.5. LẤY CHỈ SỐ ĐỘNG CHO ĐỆ TỬ TỪ CLASS
        // ==========================================
        Map<String, Double> petDynamicStats = new java.util.HashMap<>();
        if (rpgOwner.getCurrentClass() != null) {
            rpgOwner.getCurrentClass().applyPetDynamicStats(owner, rpgOwner, pet, petDynamicStats);
        }

        double resonanceDmgBonus = petDynamicStats.getOrDefault("DamageMultiplier", 0.0);
        double resonancePen = petDynamicStats.getOrDefault("ArmorPen", 0.0);
        double resonanceLifesteal = petDynamicStats.getOrDefault("Lifesteal", 0.0);
        double resonanceHealOnHit = petDynamicStats.getOrDefault("HealOnHit", 0.0);
        double resonanceBurnChance = petDynamicStats.getOrDefault("BurnChance", 0.0);
        double resonanceSlowChance = petDynamicStats.getOrDefault("SlowChance", 0.0);

        double petArmorPen = 0.0;
        double petMagicPen = 0.0;
        double petLifeSteal = 0.0;

        if (isMagicPet) {
            double shareFactor = 0.5 / petCount;
            petMagicPen = (playerStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) * shareFactor) + resonancePen;
            petLifeSteal = (playerStats.getOrDefault("Tỉ lệ hút máu phép", 0.0) * shareFactor) + resonanceLifesteal;
        } else {
            double shareFactor = 1.0 / petCount;
            petArmorPen = (playerStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) * shareFactor) + resonancePen;
            petLifeSteal = (playerStats.getOrDefault("Tỉ lệ hút máu", 0.0) * shareFactor) + resonanceLifesteal;
        }

        // 5. NỘI TẠI VÀ CHÍ MẠNG
        double summonDmgBonusPct = playerStats.getOrDefault("Sát thương Triệu hồi", 0.0);

        // [CỘNG HƯỞNG]: Nhồi Buff Sát Thương (Attacker) vào Hệ số tổng
        double rpgMultiplier = 1.0 + (summonDmgBonusPct / 100.0) + resonanceDmgBonus;

        String markKey = "Summoner_Mark_" + owner.getUniqueId().toString();
        boolean hasMark = target.hasMetadata(markKey);
        if (hasMark) rpgMultiplier *= 1.3;

        double rawDamage = finalBasePetDamage * skillMultiplier * rpgMultiplier;

        double critRate = playerStats.getOrDefault("Tỉ lệ chí mạng", 5.0) * (1.0 / petCount);
        double critDamage = playerStats.getOrDefault("Sát thương chí mạng", 50.0);
        if ((Math.random() * 100) <= critRate) {
            rawDamage *= (1.0 + (critDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            owner.playSound(owner.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5f, 1.5f);
        }

        // 6. TRỪ PHÒNG THỦ VÀ PHÁ GIÁP
        double targetDefense = 0;
        if (isMagicPet && target instanceof Player) {
            RPGPlayer targetRpg = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (targetRpg != null) targetDefense = targetRpg.getTotalStatsMap().getOrDefault("Kháng phép", 0.0);
        } else if (!isMagicPet) {
            org.bukkit.attribute.AttributeInstance armorAttr = target.getAttribute(org.bukkit.attribute.Attribute.ARMOR);
            if (armorAttr != null) targetDefense = armorAttr.getValue();
        }

        // [LỚP 1 - CHUẨN SOLID]: Áp dụng Phá Giáp Toàn Cục TRƯỚC
        double totalReductionPct = 0.0;
        for (ICombatModifier modifier : combatModifiers) {
            if (isMagicPet) {
                totalReductionPct += modifier.getTargetMagicResistReductionPercentage(target);
            } else {
                totalReductionPct += modifier.getTargetArmorReductionPercentage(target);
            }
        }
        if (totalReductionPct > 0) {
            totalReductionPct = Math.min(1.0, totalReductionPct);
            targetDefense *= (1.0 - totalReductionPct);
        }

        // [LỚP 2]: Xuyên Giáp Cá Nhân Của Pet Áp dụng SAU
        double penToUse = isMagicPet ? petMagicPen : petArmorPen;
        targetDefense *= (1.0 - (penToUse / 100.0));

        // Tính sát thương cuối cùng
        double finalDamage = Math.max(1.0, rawDamage - targetDefense);

        // ==========================================
        // 7. HÚT MÁU CHÉO & HIỆU ỨNG ĐÒN ĐÁNH (CỘNG HƯỞNG)
        // ==========================================
        double totalHeal = 0.0;
        if (petLifeSteal > 0) {
            totalHeal += finalDamage * (petLifeSteal / 100.0);
        }
        if (resonanceHealOnHit > 0) {
            totalHeal += resonanceHealOnHit; // Hồi máu chuẩn từ hệ Light
        }

        if (totalHeal > 0) {
            double maxHp = owner.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            owner.setHealth(Math.min(maxHp, owner.getHealth() + totalHeal));
            owner.getWorld().spawnParticle(Particle.HEART, owner.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
        }

        // [CỘNG HƯỞNG]: Hiệu ứng Nguyên Tố (Thiêu Đốt, Làm Chậm)
        if (resonanceBurnChance > 0 && (Math.random() * 100) <= resonanceBurnChance) {
            target.setFireTicks(60); // Đốt 3s
        }
        if (resonanceSlowChance > 0 && (Math.random() * 100) <= resonanceSlowChance) {
            vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1);
        }

        // 8. KIỂM TRA & XỬ LÝ DUMMY (Báo DMG + Bất tử Dummy)
        if (target.getCustomName() != null && org.bukkit.ChatColor.stripColor(target.getCustomName()).contains("Dummy")) {
            String petNameRaw = pet.getCustomName() != null ? org.bukkit.ChatColor.stripColor(pet.getCustomName()).replaceAll("\\[.*?\\]", "").trim() : "Linh Lang";
            String prefix = hasMark ? "§d[" + petNameRaw + " - Cuồng Nộ]" : "§b[" + petNameRaw + "]";

            String skillName = "";
            if (skillMultiplier > 1.0) {
                if (skillMultiplier >= 2.0) skillName = " §e[Sét]";
                else skillName = " §6[Đuôi]";
            }

            owner.sendMessage("§e§l[DUMMY] " + prefix + skillName + " §fSát thương: §c" + String.format("%.1f", finalDamage) + " ⚔");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isValid() && !target.isDead()) {
                        org.bukkit.attribute.AttributeInstance maxHp = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (maxHp != null) target.setHealth(maxHp.getValue());
                        target.setFireTicks(0);
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
        }

        return finalDamage;
    }
    // Thêm hàm này vào CombatManager.java
    public static boolean canTarget(Player attacker, org.bukkit.entity.Entity victim) {
        if (attacker == null || victim == null) return false;
        if (attacker.equals(victim)) return false; // Không tự đánh mình

        Player victimOwner = null;

        // 1. Nếu mục tiêu là Người chơi
        if (victim instanceof Player) {
            victimOwner = (Player) victim;
        }
        // 2. Nếu mục tiêu là Thú triệu hồi
        else if (victim.hasMetadata("Summoner_Pet")) {
            try {
                // [FIX LỖI CONSOLE]: Kiểm tra list rỗng trước khi get(0)
                if (victim.getMetadata("Summoner_Pet").isEmpty()) return true;

                String ownerId = victim.getMetadata("Summoner_Pet").get(0).asString();

                // KHÔNG đánh thú của chính mình
                if (ownerId.equals(attacker.getUniqueId().toString())) {
                    return false;
                }
                victimOwner = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(ownerId));
            } catch (Exception ignored) {}
        }

        // 3. Nếu mục tiêu CÓ CHỦ (Là người chơi khác hoặc Thú của họ) -> Check Guild
        if (victimOwner != null && victimOwner.isOnline()) {

            // ==========================================
            // [TÍCH HỢP API RPGGUILD CỦA BẠN VÀO ĐÂY]
            // boolean isSameGuild = RPGGuildAPI.isSameGuild(attacker, victimOwner);
            // if (isSameGuild) return false;
            // ==========================================
            for (IAllyChecker checker : allyCheckers) {
                if (checker.isAlly(attacker, victimOwner)) {
                    return false; // Nếu 1 trong các plugin (Guild/Party) bảo là Đồng Minh -> Cấm đánh!
                }
            }

            return true; // Khác Guild -> Cho phép tàn sát
        }

        // 4. Nếu là quái vật hoang dã bình thường
        return true;
    }

    // ==========================================
    // [HỆ THỐNG HOOK ĐỒNG MINH - DECOUPLED API]
    // ==========================================
    public interface IAllyChecker {
        boolean isAlly(Player p1, Player p2);
    }

    public static final java.util.List<IAllyChecker> allyCheckers = new java.util.ArrayList<>();

    // Các Plugin con (Party, Guild) sẽ gọi hàm này để đăng ký logic của họ vào đây
    public static void registerAllyChecker(IAllyChecker checker) {
        allyCheckers.add(checker);
    }

    // ==========================================
    // [HỆ THỐNG HOOK BUFF/DEBUFF SÁT THƯƠNG]
    // ==========================================
    public interface ICombatModifier {
        // Trả về lượng % sát thương GÂY RA cộng thêm (VD: +0.2 là tăng 20%)
        default double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) { return 0.0; }

        // Trả về lượng % sát thương NHẬN VÀO được giảm (VD: 0.15 là giảm 15%)
        default double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) { return 0.0; }

        // [CHUẨN SOLID]: Tách bạch Phá Giáp (Vật lý) và Phân Rã (Phép thuật)
        default double getTargetArmorReductionPercentage(LivingEntity target) { return 0.0; }
        default double getTargetMagicResistReductionPercentage(LivingEntity target) { return 0.0; }
    }

    public static final java.util.List<ICombatModifier> combatModifiers = new java.util.ArrayList<>();

    // Các Kỹ năng (Skill) sẽ tự gọi hàm này để đăng ký Buff của nó vào Core
    public static void registerCombatModifier(ICombatModifier modifier) {
        combatModifiers.add(modifier);
    }

    // ==========================================
    // 2. [HỆ THỐNG HOOK CHỈ SỐ TĨNH] (BẠN THÊM CÁI MỚI NÀY VÀO BÊN DƯỚI)
    // ==========================================
    public interface IStatModifier {
        void applyBonusStats(Player player, java.util.Map<String, Double> stats);
    }
    public static final java.util.List<IStatModifier> statModifiers = new java.util.ArrayList<>();
    public static void registerStatModifier(IStatModifier modifier) {
        statModifiers.add(modifier);
    }
}