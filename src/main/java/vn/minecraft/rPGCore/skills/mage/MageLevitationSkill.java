package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class MageLevitationSkill implements ISkill {

    static {
        // 1. Hook chỉ số Né tránh
        CombatManager.registerStatModifier((player, stats) -> {
            if (player.hasMetadata("Mage_Levitating")) {
                int level = player.getMetadata("Mage_Levitating").get(0).asInt();
                double dodge = level * 4.0;
                if (player.hasMetadata("Levitating_Wind")) dodge += 10.0;
                stats.put("Tỉ lệ né tránh", stats.getOrDefault("Tỉ lệ né tránh", 0.0) + dodge);
            }
        });

        // 2. Hook Combat: Nhận thêm 20% sát thương khi bay
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim instanceof Player && victim.hasMetadata("Mage_Levitating")) return -0.20;
                return 0.0;
            }
        });
    }

    @Override public String getId() { return "mage_levitation"; }
    @Override public String getName() { return "§b§lNgự Không Thuật"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.FEATHER; }

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 30; }
    @Override public long getCooldown(int level) { return 1000L; }

    @Override public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("mage_blink", 6);
        req.put("mage_mana_flow", 5);
        return req;
    }

    // Tiêu hao: LV1 = 120 -> LV5 = 180
    @Override public double getManaCost(int level) { return 105.0 + (level * 15.0); }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList("§7Tập trung ma lực xuống đôi chân để thoát ly trọng lực.");
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        MageElement element = ElementalSwapSkill.getActiveElement(player);
        double dodge = level * 4.0;
        String speedDesc = (level <= 2) ? "Chậm" : (level == 3) ? "Vừa" : "Nhanh";

        List<String> lore = new ArrayList<>();
        lore.add("§7Tập trung ma lực xuống đôi chân để thoát ly trọng lực.");
        lore.add("");
        lore.add("§f▪ Nguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Kích hoạt - " + org.bukkit.ChatColor.stripColor(element.getDisplayName()) + " Chi Dực]");
        lore.add("§f- Tốc độ bay: §a" + speedDesc);
        lore.add("§f- Tỉ lệ né tránh: §a+" + (int)dodge + "%");
        lore.add("§f- §cBất lợi: §fNhận thêm §c20% Sát thương§f khi đang bay.");
        lore.add("");

        String buffTitle = "";
        String buffEffect = "";
        switch (element) {
            case FIRE: buffTitle = "Rực Cháy"; buffEffect = "Tăng mạnh tốc độ bay, hao thêm §c20 Mana/s§f."; break;
            case WATER: buffTitle = "Thanh Tẩy"; buffEffect = "Giải trừ mọi hiệu ứng khống chế khi cất cánh."; break;
            case WIND: buffTitle = "Linh Hoạt"; buffEffect = "Tăng thêm §a10% Tỉ lệ né tránh§f."; break;
            case NATURE: buffTitle = "Hài Hòa"; buffEffect = "Giảm §a15 Mana§f tiêu hao mỗi giây."; break;
            case LIGHT: buffTitle = "Thánh Quang"; buffEffect = "Nhìn xuyên đêm, giảm §a5 Mana§f tiêu hao."; break;
            case DARK: buffTitle = "Ám Ảnh"; buffEffect = "Làm mù kẻ địch bên dưới chân (§7Tầm ảnh hưởng <= 5§f)."; break;
        }

        lore.add("§b[Nội tại - Trợ Lực Nguyên Tố - " + buffTitle + "§b]");
        lore.add("§f- " + buffEffect);
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Tắt chủ động -> Rơi tự do (Ép người chơi hạ độ cao trước)
        if (player.hasMetadata("Mage_Levitating")) {
            stopFlight(player, false);
            return true;
        }

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        double drain = getManaCost(level);
        float flySpeed = 0.04f + (level * 0.02f);

        // Xử lý nguyên tố
        if (element == MageElement.FIRE) { drain += 20.0; flySpeed += 0.06f; }
        else if (element == MageElement.WATER) {
            StatusEffectManager.cleanseNegativeEffects(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1.5f);
        }
        else if (element == MageElement.WIND) { player.setMetadata("Levitating_Wind", new FixedMetadataValue(RPGCore.getInstance(), true)); }
        else if (element == MageElement.NATURE) { drain -= 15.0; }
        else if (element == MageElement.LIGHT) { drain -= 5.0; player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0)); }

        if (rpgPlayer.getCurrentResource() < drain) {
            player.sendMessage("§c§l[!] §fKhông đủ Năng lượng để cất cánh!");
            return false;
        }

        final double finalDrain = Math.max(10.0, drain);
        player.setMetadata("Mage_Levitating", new FixedMetadataValue(RPGCore.getInstance(), level));

        // Gọi StatusEffectManager: Cho phép tắt bay, có SafeFall (ban đầu), KHÔNG hủy khi bấm Shift
        StatusEffectManager.applyCustomHover(player, 72000, flySpeed, true, false);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        player.sendMessage("§a§l[!] §fNgự Không Thuật: §aBẬT §7(" + element.getDisplayName() + "§7)");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata("Mage_Levitating")) {
                    this.cancel();
                    return;
                }

                // Nếu chạm đất thành công
                if (!player.isFlying()) {
                    stopFlight(player, true);
                    player.sendMessage("§e§l[!] §fĐã tiếp đất an toàn.");
                    this.cancel();
                    return;
                }

                if (rpgPlayer.getCurrentResource() >= finalDrain) {
                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - finalDrain);

                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 2, 0.2, 0.0, 0.2, 0.02);
                    if (element == MageElement.FIRE) {
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 3, 0.1, -0.2, 0.1, 0.05);
                    } else if (element == MageElement.DARK) {
                        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 5, 0.5, -0.5, 0.5, 0.05);
                        for (org.bukkit.entity.Entity e : player.getNearbyEntities(5, 5, 5)) {
                            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                                double gap = player.getLocation().getY() - e.getLocation().getY();
                                if (gap > 0 && gap <= 5) ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                            }
                        }
                    }
                } else {
                    // Cạn Mana -> Tước quyền Safe Fall, rơi tự do
                    stopFlight(player, false);
                    player.sendMessage("§c§l[!] §fCạn kiệt Năng lượng, rơi tự do!");
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 20L, 20L);

        return true;
    }

    public static void stopFlight(Player player, boolean grantSafeFall) {
        if (player.hasMetadata("Mage_Levitating")) {
            player.removeMetadata("Mage_Levitating", RPGCore.getInstance());
            player.removeMetadata("Levitating_Wind", RPGCore.getInstance());
            StatusEffectManager.removeCustomHover(player, grantSafeFall);
            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1f, 1f);
        }
    }
}