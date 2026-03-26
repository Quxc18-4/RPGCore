package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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

        double penToUse = isMagic ? finalMagicPen : finalArmorPen;
        double finalDefenseMulti = 1.0 - (penToUse / 100.0);
        targetDefense *= finalDefenseMulti;

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

        double dodgeRate = totalStats.getOrDefault("Tỉ lệ né tránh", 0.0);
        if (dodgeRate > 0 && (Math.random() * 100) <= dodgeRate) {
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l[NÉ THÀNH CÔNG]"));
            return 0.0;
        }

        double finalDamage = incomingDamage;

        double blockRate = totalStats.getOrDefault("Tỉ lệ đỡ đòn", 0.0);
        if (blockRate > 0 && (Math.random() * 100) <= blockRate) {
            victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.SHIELD));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            finalDamage *= 0.7;
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§9§l[ĐỠ ĐÒN: -30% SÁT THƯƠNG]"));
        }

        // ==========================================
        // [MỚI]: KÍCH HOẠT GIẢM SÁT THƯƠNG TỪ CỘNG HƯỞNG (VANGUARD)
        // ==========================================
        if (victim.hasMetadata("Resonance_Stats")) {
            Object val = victim.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Double> resonanceBuffs = (Map<String, Double>) val;
                double damageReduction = resonanceBuffs.getOrDefault("DamageReduction", 0.0);

                if (damageReduction > 0) {
                    finalDamage *= (1.0 - damageReduction); // Trừ đi % ST tương ứng

                    // [DEBUG] In ra ST đã được giảm nhờ Vanguard
                    victim.sendMessage("§e[Debug Vanguard] Đã giảm §c" + (damageReduction * 100) + "% §eST nhận vào (Còn lại: " + String.format("%.1f", finalDamage) + ")");
                }
            }
        }

        // ==========================================
        // [MỚI]: HẤP THỤ SÁT THƯƠNG VÀO KHIÊN NGẦM
        // ==========================================
        if (victim.hasMetadata("Custom_Earth_Shield")) {
            double currentShield = victim.getMetadata("Custom_Earth_Shield").get(0).asDouble();
            double maxShield = victim.getMetadata("Custom_Earth_Shield_Max").get(0).asDouble();

            if (currentShield > 0) {
                if (finalDamage <= currentShield) {
                    // Khiên đủ sức đỡ toàn bộ sát thương
                    currentShield -= finalDamage;
                    finalDamage = 0; // Sát thương truyền vào người = 0
                    victim.setMetadata("Custom_Earth_Shield", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), currentShield));
                } else {
                    // Sát thương làm vỡ khiên
                    finalDamage -= currentShield; // Sát thương tràn vào máu đỏ
                    currentShield = 0;
                    victim.removeMetadata("Custom_Earth_Shield", vn.minecraft.rPGCore.RPGCore.getInstance());
                    victim.removeMetadata("Custom_Earth_Shield_Max", vn.minecraft.rPGCore.RPGCore.getInstance());
                    victim.setAbsorptionAmount(0);
                    victim.playSound(victim.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BREAK, 1.0f, 1.2f);
                }

                // Cập nhật lại UI hiển thị tim vàng (Chia tỷ lệ)
                if (currentShield > 0) {
                    double displayAbsorption = (currentShield / maxShield) * 20.0;
                    // Đảm bảo dù khiên còn 1 giọt cũng hiển thị ít nhất nửa tim vàng cho người chơi dễ nhìn
                    victim.setAbsorptionAmount(Math.max(1.0, displayAbsorption));
                }
            }
        }

        return finalDamage;
    }

    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct, boolean isMagic) {
        if (attacker.hasMetadata("Status_AntiHeal")) {
            long expire = attacker.getMetadata("Status_AntiHeal").get(0).asLong();
            if (System.currentTimeMillis() < expire) return;
        }

        double finalExtraLifesteal = extraLifeStealPct;

        // ==========================================
        // [CHUẨN SOLID]: LẤY LIFESTEAL TỪ CLASS
        // ==========================================
        if (rpgPlayer.getCurrentClass() != null) {
            finalExtraLifesteal += rpgPlayer.getCurrentClass().getDynamicLifeSteal(attacker, rpgPlayer);
        }

        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();
        String stealType = isMagic ? "Tỉ lệ hút máu phép" : "Tỉ lệ hút máu";
        double totalLifeSteal = totalStats.getOrDefault(stealType, 0.0) + finalExtraLifesteal;

        if (totalLifeSteal <= 0) return;

        double healAmount = damageDealt * (totalLifeSteal / 100.0);
        if (healAmount <= 0) return;

        double maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHp, attacker.getHealth() + healAmount));

        Particle pType = isMagic ? Particle.SOUL : Particle.HEART;
        attacker.getWorld().spawnParticle(pType, attacker.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
    }

    // ==========================================
    // HỆ THỐNG GÂY SÁT THƯƠNG KỸ NĂNG (DÙNG CHUNG CHO MỌI CLASS)
    // ==========================================
    public static void applySkillDamage(Player attacker, LivingEntity target, double finalDamage) {
        // 1. Xóa I-Frame (Giây bất tử của Vanilla) để chiêu thức chạm là ăn sát thương
        target.setNoDamageTicks(0);

        // 2. Cắm cờ Khóa Sát Thương để PlayerListener không tính lặp thành đòn đánh tay
        attacker.setMetadata("Skill_Damage_Lock", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));

        // 3. Gây sát thương (Ghi nhận người ra đòn để quái dí theo và tính Kill)
        target.damage(finalDamage, attacker);

        // 4. Rút cờ Khóa Sát Thương ra
        attacker.removeMetadata("Skill_Damage_Lock", vn.minecraft.rPGCore.RPGCore.getInstance());
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

        // 6. TRỪ PHÒNG THỦ
        double targetDefense = 0;
        if (isMagicPet && target instanceof Player) {
            RPGPlayer targetRpg = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (targetRpg != null) targetDefense = targetRpg.getTotalStatsMap().getOrDefault("Kháng phép", 0.0);
            targetDefense *= (1.0 - (petMagicPen / 100.0));
        } else if (!isMagicPet) {
            org.bukkit.attribute.AttributeInstance armorAttr = target.getAttribute(org.bukkit.attribute.Attribute.ARMOR);
            if (armorAttr != null) targetDefense = armorAttr.getValue();
            targetDefense *= (1.0 - (petArmorPen / 100.0));
        }

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
}