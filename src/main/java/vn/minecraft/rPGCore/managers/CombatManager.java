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

        // ==========================================
        // 1. TÍCH LŨY STACK SÁT Ý LĨNH VỰC (Cơ chế 1/3 Stack)
        // ==========================================
        if (attacker.hasMetadata("Assassin_KillingIntent_Active")) {
            long expire = attacker.getMetadata("Assassin_KillingIntent_Active").get(0).asLong();
            if (System.currentTimeMillis() <= expire) {
                // SỬ DỤNG SUBSTACKS (1/3)
                int subStacks = attacker.hasMetadata("Assassin_KillingIntent_SubStacks") ? attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() : 0;

                if (subStacks < 30) { // 30 Sub = 10 Stack thật
                    subStacks++;
                    attacker.setMetadata("Assassin_KillingIntent_SubStacks", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), subStacks));

                    int realStacks = subStacks / 3;
                    if (subStacks % 3 == 0) { // Cứ đủ 3 hit thì kêu một tiếng "Ping"
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.0f + (realStacks * 0.1f));
                    }

                    if (subStacks < 30) {
                        // Làm mới lại 7s
                        attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + 7000));
                    } else {
                        // Max 10 Stack -> Đặt thời gian (Scaling theo level) và Khóa làm mới
                        int kiLevel = attacker.hasMetadata("Assassin_KillingIntent_Level") ? attacker.getMetadata("Assassin_KillingIntent_Level").get(0).asInt() : 1;
                        long maxDur = (10 + kiLevel) * 1000L;
                        attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + maxDur));
                        attacker.sendMessage("§4§l[!] TỤ SÁT Ý TỐI ĐA! (Cuồng nộ trong " + (10+kiLevel) + "s)");
                    }
                }
            }
        }

        Map<String, Double> combatStats = new java.util.HashMap<>(rpgPlayer.getTotalStatsMap());

        // CHUẨN BỊ BÙ CRIT TỪ ĐÁNH LÉN TÀNG HÌNH (Bảo đảm 100% không tính dư)
        if (attacker.hasMetadata("Basic_Stealth_Attack")) {
            attacker.removeMetadata("Basic_Stealth_Attack", vn.minecraft.rPGCore.RPGCore.getInstance());
            double currentBaseCrit = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0);
            if (currentBaseCrit < 100.0) {
                extraCritRate += (100.0 - currentBaseCrit);
            }
        }

        double dynamicCritRate = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0) + extraCritRate;
        double dynamicCritDamage = combatStats.getOrDefault("Sát thương chí mạng", 50.0) + extraCritDamage;
        double dynamicArmorPen = combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + extraArmorPen;

        if (attacker.hasMetadata("Assassin_KillingIntent_SubStacks")) {
            int realStacks = attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() / 3;
            int level = attacker.hasMetadata("Assassin_KillingIntent_Level") ? attacker.getMetadata("Assassin_KillingIntent_Level").get(0).asInt() : 1;

            // [GIẢM SỨC MẠNH SÁT Ý]: 2.5% Crit và 6% DMG ở max level
            dynamicCritRate += (realStacks * (0.5 + (level * 0.4)));
            dynamicCritDamage += (realStacks * (1.0 + (level * 1.0)));
        }

        combatStats.put("Tỉ lệ chí mạng", dynamicCritRate);
        combatStats.put("Sát thương chí mạng", dynamicCritDamage);
        combatStats.put("Tỉ lệ xuyên giáp", dynamicArmorPen);

        // NỘI TẠI CLASS CHUYỂN HÓA CRIT
        if (rpgPlayer.getCurrentClass() != null) {
            rpgPlayer.getCurrentClass().applyOffensivePassives(combatStats);
        }

        // BÙ CRIT CHO CHIÊU CHÉM ĐÔI (Backstab 30% - Không bị tính dư)
        if (attacker.hasMetadata("DoubleSlash_Backstab")) {
            double finalCrit = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0) + 30.0;
            combatStats.put("Tỉ lệ chí mạng", Math.min(finalCrit, 100.0));
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

        // Xuyên giáp giờ lấy thẳng từ finalArmorPen (Đã được mở Gap 80% nếu là Archer)
        double penToUse = isMagic ? finalMagicPen : finalArmorPen;
        double finalDefenseMulti = 1.0 - (penToUse / 100.0);
        targetDefense *= finalDefenseMulti;

        double finalDamage = Math.max(1.0, (rawDamage - targetDefense) * dmgMultiplier);

        // Code Dummy Test giữ nguyên
        if (target.getCustomName() != null && ChatColor.stripColor(target.getCustomName()).contains("Dummy")) {
            int currentStacks = attacker.hasMetadata("Assassin_KillingIntent_Stacks") ? attacker.getMetadata("Assassin_KillingIntent_Stacks").get(0).asInt() : 0;
            attacker.sendMessage("§e§l[DUMMY] §8[Stack: " + currentStacks + "] §fSát thương: §c" + String.format("%.1f", finalDamage) + " ⚔");

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

        return finalDamage;
    }

    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct, boolean isMagic) {
        if (attacker.hasMetadata("Assassin_AntiHeal")) {
            long expire = attacker.getMetadata("Assassin_AntiHeal").get(0).asLong();
            if (System.currentTimeMillis() < expire) return;
        }

        double finalExtraLifesteal = extraLifeStealPct;

        // ==========================================
        // [MỚI CẬP NHẬT]: KÍCH HOẠT ĐÒN KHÁT MÁU (CHỈ DÙNG 1 LẦN)
        // ==========================================
        if (attacker.hasMetadata("Assassin_Bloodlust_Value")) {
            finalExtraLifesteal += attacker.getMetadata("Assassin_Bloodlust_Value").get(0).asDouble();

            // Xóa ngay lập tức để không bị lặp lại ở đòn thứ 2
            attacker.removeMetadata("Assassin_Bloodlust_Value", vn.minecraft.rPGCore.RPGCore.getInstance());

            attacker.sendMessage("§c§l[!] §fĐòn Khát Máu chạm đích! Hồi phục sinh lực!");
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 0.8f);
            attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 7, 0.5, 0.5, 0.5, 0.1);
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
}