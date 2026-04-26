package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;

public class AssassinClass extends RPGClass {

    public AssassinClass(String id, String displayName, Material icon, List<String> description,
                         double maxHealth, double maxResource, ResourceType resourceType,
                         double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
        startPassiveStealthTask(RPGCore.getInstance());
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, java.util.Map<String, Double> stats) {
        double currentCrit = stats.getOrDefault("Tỉ lệ chí mạng", 5.0);
        stats.put("Tỉ lệ chí mạng", currentCrit + 15.0);

        // [THÊM MỚI]: Gọi hàm cha để ép Xuyên giáp về 50%
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    @Override
    protected void processCrit(java.util.Map<String, Double> combatStats, double critRate) {
        // LOGIC CHUYỂN HÓA DƯ THỪA CHUẨN MỰC
        if (critRate > 100.0) {
            double excess = critRate - 100.0;
            double currentCritDmg = combatStats.getOrDefault("Sát thương chí mạng", 50.0);

            combatStats.put("Sát thương chí mạng", currentCritDmg + (excess * 2.0));
            combatStats.put("Tỉ lệ chí mạng", 100.0); // Khóa lại ở 100%
        } else {
            combatStats.put("Tỉ lệ chí mạng", critRate);
        }
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        if (attacker.hasMetadata("Assassin_Stealth")) {
            attacker.removeMetadata("Assassin_Stealth", RPGCore.getInstance());
            attacker.removeMetadata("Assassin_Smoke_Bomb_Active", RPGCore.getInstance());
            attacker.sendMessage("§c§l[!] Đòn đánh bất ngờ! Hiện hình!");

            // Gắn cờ để CombatManager tự bù % Crit cho đủ 100%
            attacker.setMetadata("Basic_Stealth_Attack", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(RPGCore.getInstance(), attacker);
            attacker.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
        return rawDamage; // Trả về rawDamage vì Crit đã x2 Dame sẵn rồi
    }

    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        // TÍCH LŨY STACK SÁT Ý LĨNH VỰC (Cơ chế 1/3 Stack)
        if (attacker.hasMetadata("Assassin_KillingIntent_Active")) {
            long expire = attacker.getMetadata("Assassin_KillingIntent_Active").get(0).asLong();
            if (System.currentTimeMillis() <= expire) {
                int subStacks = attacker.hasMetadata("Assassin_KillingIntent_SubStacks") ? attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() : 0;

                if (subStacks < 30) {
                    subStacks++;
                    attacker.setMetadata("Assassin_KillingIntent_SubStacks", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), subStacks));

                    int realStacks = subStacks / 3;
                    if (subStacks % 3 == 0) { // Cứ đủ 3 hit thì kêu một tiếng "Ping"
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.0f + (realStacks * 0.1f));
                    }

                    if (subStacks < 30) {
                        // Làm mới lại 7s
                        attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 7000));
                    } else {
                        // Max 10 Stack -> Đặt thời gian và Khóa làm mới
                        int kiLevel = attacker.hasMetadata("Assassin_KillingIntent_Level") ? attacker.getMetadata("Assassin_KillingIntent_Level").get(0).asInt() : 1;
                        long maxDur = (10 + kiLevel) * 1000L;
                        attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + maxDur));
                        attacker.sendMessage("§4§l[!] TỤ SÁT Ý TỐI ĐA! (Cuồng nộ trong " + (10+kiLevel) + "s)");
                    }
                }
            }
        }
    }

    @Override
    public void applyDynamicCombatStats(Player attacker, LivingEntity target, java.util.Map<String, Double> combatStats) {
        // 1. TÀNG HÌNH ĐỘT KÍCH (Bù Crit cho đủ 100%)
        if (attacker.hasMetadata("Basic_Stealth_Attack")) {
            attacker.removeMetadata("Basic_Stealth_Attack", RPGCore.getInstance());
            double currentBaseCrit = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0);
            if (currentBaseCrit < 100.0) {
                // Nhồi thẳng lên 100%, hàm cha sẽ tự ép Cap và tính dư thừa
                combatStats.put("Tỉ lệ chí mạng", 100.0);
            }
        }

        // 2. SONG ĐAO ĐOẠT MỆNH (Đâm lén +30% Crit)
        if (attacker.hasMetadata("DoubleSlash_Backstab")) {
            combatStats.put("Tỉ lệ chí mạng", combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0) + 30.0);
            // Không xóa Metadata ở đây vì chiêu AoE cần quét qua nhiều mục tiêu
        }

        // 3. SÁT Ý LĨNH VỰC (Cộng dồn Crit Rate & Crit Dmg)
        if (attacker.hasMetadata("Assassin_KillingIntent_SubStacks")) {
            int realStacks = attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() / 3;
            int level = attacker.hasMetadata("Assassin_KillingIntent_Level") ? attacker.getMetadata("Assassin_KillingIntent_Level").get(0).asInt() : 1;

            double currentCrit = combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0);
            double currentCritDmg = combatStats.getOrDefault("Sát thương chí mạng", 50.0);

            combatStats.put("Tỉ lệ chí mạng", currentCrit + (realStacks * (0.5 + (level * 0.4))));
            combatStats.put("Sát thương chí mạng", currentCritDmg + (realStacks * (1.0 + (level * 1.0))));
        }
    }

    @Override
    public double getDynamicLifeSteal(Player attacker, RPGPlayer rpgPlayer) {
        // ĐÒN KHÁT MÁU
        if (attacker.hasMetadata("Assassin_Bloodlust_Value")) {
            double lifestealBonus = attacker.getMetadata("Assassin_Bloodlust_Value").get(0).asDouble();

            // Xóa ngay lập tức để chỉ dùng 1 lần
            attacker.removeMetadata("Assassin_Bloodlust_Value", RPGCore.getInstance());

            attacker.sendMessage("§c§l[!] §fĐòn Khát Máu chạm đích! Hồi phục sinh lực!");
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 0.8f);
            attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 7, 0.5, 0.5, 0.5, 0.1);

            return lifestealBonus;
        }
        return 0.0;
    }

    // Thêm hàm này để các Kỹ năng tự gọi khi cast thành công
    public void addSkillStack(Player attacker) {
        if (attacker.hasMetadata("Assassin_KillingIntent_Active")) {
            long expire = attacker.getMetadata("Assassin_KillingIntent_Active").get(0).asLong();
            if (System.currentTimeMillis() <= expire) {
                int subStacks = attacker.hasMetadata("Assassin_KillingIntent_SubStacks") ? attacker.getMetadata("Assassin_KillingIntent_SubStacks").get(0).asInt() : 0;

                // 1 Kỹ năng = 1 Stack = 3 SubStacks
                subStacks += 3;
                if (subStacks > 30) subStacks = 30;

                attacker.setMetadata("Assassin_KillingIntent_SubStacks", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), subStacks));

                int realStacks = subStacks / 3;
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f + (realStacks * 0.1f));

                if (subStacks < 30) {
                    attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 7000));
                } else {
                    int kiLevel = attacker.hasMetadata("Assassin_KillingIntent_Level") ? attacker.getMetadata("Assassin_KillingIntent_Level").get(0).asInt() : 1;
                    long maxDur = (10 + kiLevel) * 1000L;
                    attacker.setMetadata("Assassin_KillingIntent_Active", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + maxDur));
                    attacker.sendMessage("§4§l[!] TỤ SÁT Ý TỐI ĐA! (Cuồng nộ trong " + (10+kiLevel) + "s)");
                }
            }
        }
    }

    // ==========================================
    // [BƯỚC 2.1]: ÁO CHOÁNG BÓNG ĐÊM (LỚP 1)
    // ==========================================
    @Override
    public double onTakeDamage(Player victim, vn.minecraft.rPGCore.player.RPGPlayer rpgVictim, LivingEntity attacker, double damage) {
        if (victim.hasMetadata("Assassin_Stealth")) {
            damage *= 0.5; // Giảm 50% ngay từ gốc
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§8§l[ÁO CHOÁNG BÓNG ĐÊM KÍCH HOẠT: -50% SÁT THƯƠNG]"));
        }
        return super.onTakeDamage(victim, rpgVictim, attacker, damage);
    }

    // ==========================================
    // [BƯỚC 2.2]: NỘI TẠI LẨN KHUẤT (10s AUTO-STEALTH)
    // ==========================================
    public void startPassiveStealthTask(vn.minecraft.rPGCore.RPGCore core) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    vn.minecraft.rPGCore.player.RPGPlayer rpg = core.getPlayerManager().getRPGPlayer(player.getUniqueId());

                    if (rpg != null && rpg.getCurrentClass() instanceof AssassinClass) {
                        if (!player.hasMetadata("Assassin_Stealth") && !rpg.isInCombat()) {

                            vn.minecraft.rPGCore.skills.ISkill skill = core.getSkillManager().getSkill("assassin_smoke_bomb");
                            if (skill instanceof vn.minecraft.rPGCore.skills.assassin.SmokeBombSkill) {
                                ((vn.minecraft.rPGCore.skills.assassin.SmokeBombSkill) skill).applyStealth(player, 999999);
                                player.sendMessage("§8§o(Nhịp tim ổn định... Bạn đã lẩn khuất vào bóng đêm)");
                                player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.02);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(core, 0L, 20L);
    }
}