package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
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
import vn.minecraft.rPGCore.skills.mage.ElementalSwapSkill;

import java.util.*;

public class MageLevitationSkill implements ISkill {

    // ==========================================
    // KHỐI TĨNH: HOOK CHỈ SỐ VÀ SÁT THƯƠNG
    // ==========================================
    static {
        // 1. Hook chỉ số Né tránh & Khóa hồi Mana
        CombatManager.registerStatModifier((player, stats) -> {
            if (player.hasMetadata("Mage_Levitating")) {
                int level = player.getMetadata("Mage_Levitating").get(0).asInt();
                double dodge = level * 4.0;
                if (player.hasMetadata("Levitating_Wind")) dodge += 10.0;
                stats.put("Tỉ lệ né tránh", stats.getOrDefault("Tỉ lệ né tránh", 0.0) + dodge);
            }
        });

        // 2. Hook Combat: Nhận thêm 20% sát thương
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim instanceof Player && victim.hasMetadata("Mage_Levitating")) return -0.20;
                return 0.0;
            }
        });
    }

    @Override public String getId() { return "mage_levitation"; }
    @Override public String getName() { return "§b§lNguyên Tố Chi Dực"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.FEATHER; }

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 30; }
    @Override public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("mage_blink", 6);
        req.put("mage_mana_flow", 5);
        return req;
    }

    @Override public boolean isPassiveSkill() { return false; }

    // TIÊU HAO MANA: Cấp 1 = 50 -> Cấp 5 = 90
    // LV1 = 120, LV5 = 180 -> Công thức: 105 + (level * 15)
    @Override public double getManaCost(int level) { return 105.0 + (level * 15.0); }
    @Override public long getCooldown(int level) { return 1000L; }

    // TỐC ĐỘ BAY
    private float getFlySpeed(int level) {
        return 0.04f + (level * 0.02f); // Cấp 1: 0.06f -> Cấp 5: 0.14f
    }

    // 1. HÀM GỐC BẮT BUỘC CỦA ISKILL (Dùng khi không có thông tin người chơi)
    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList("§7Sử dụng ma lực để thoát ly trọng lực.");
    }

    // 2. HÀM LORE ĐỘNG (Ghi đè từ ISkill.java - Tự động được gọi khi mở GUI)
    @Override
    public List<String> getDescription(Player player, int level) {
        MageElement element = ElementalSwapSkill.getActiveElement(player);
        double dodge = level * 4.0;

        // Trả lại hiển thị Chậm / Vừa / Nhanh
        String speedDesc = (level <= 2) ? "Chậm" : (level == 3) ? "Vừa" : "Nhanh";

        List<String> lore = new ArrayList<>();
        lore.add("§7Sử dụng ma lực để thoát ly trọng lực.");
        lore.add("");
        lore.add("§fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Kích hoạt - Bài Ca Của " + org.bukkit.ChatColor.stripColor(element.getDisplayName()) + "§e]");
        lore.add("§f- Tốc độ bay: §a" + speedDesc);
        lore.add("§f- Tỉ lệ né tránh: §a+" + (int)dodge + "%");
        lore.add("§f- Nhận thêm §c20% Sát thương§f khi bị tấn công.");
        lore.add("");

        // Tách riêng Tên Buff và Mô tả để nhét vào Tiêu đề
        String buffName = "";
        String buffDesc = "";
        switch (element) {
            case FIRE:
                buffName = "§cRực Cháy";
                buffDesc = "Tăng mạnh tốc độ bay, tiêu hao thêm §c20 Mana/s§f.";
                break;
            case WATER:
                buffName = "§bThanh Tẩy";
                buffDesc = "Giải trừ mọi hiệu ứng khống chế khi cất cánh.";
                break;
            case WIND:
                buffName = "§aLinh Hoạt";
                buffDesc = "Tăng thêm §a10% Tỉ lệ né tránh§f.";
                break;
            case NATURE:
                buffName = "§2Hài Hòa";
                buffDesc = "Giảm §a15 Mana§f tiêu hao mỗi giây.";
                break;
            case LIGHT:
                buffName = "§eThánh Quang";
                buffDesc = "Cung cấp tầm nhìn trong bóng tối, giảm §a5 Mana§f tiêu hao.";
                break;
            case DARK:
                buffName = "§5Ám Ảnh";
                buffDesc = "Làm mù kẻ địch bên dưới (§7Tối đa 5 block§f).";
                break;
        }

        // Ép Tên Buff vào Header của Nội tại
        lore.add("§b[Nội tại - Trợ Lực Nguyên Tố - " + buffName + "§b]");
        lore.add("§f- " + buffDesc);

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (player.hasMetadata("Mage_Levitating")) {
            stopFlight(player, false); // [SỬA]: Tắt chủ động -> Rơi tự do (Ép người chơi hạ độ cao trước)
            return true;
        }

        // ==========================================
        // 2. BẬT CHIÊU VÀ LẤY NGUYÊN TỐ
        // ==========================================
        double baseDrain = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < baseDrain) {
            player.sendMessage("§c§l[!] §fKhông đủ Năng lượng để cất cánh!");
            return false;
        }

        MageElement activeElement = ElementalSwapSkill.getActiveElement(player);
        float flySpeed = getFlySpeed(level);

        // --- XỬ LÝ CỘNG HƯỞNG NGUYÊN TỐ ---
        if (activeElement == MageElement.FIRE) {
            baseDrain += 20.0;
            flySpeed += 0.05f;
        }
        else if (activeElement == MageElement.WATER) {
            // Tái sử dụng hàm thanh tẩy từ Core Manager
            StatusEffectManager.cleanseNegativeEffects(player);

            // Chỉ giữ lại âm thanh đặc trưng của hệ Nước khi giải vây
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1.5f);
        }
        else if (activeElement == MageElement.WIND) {
            player.setMetadata("Levitating_Wind", new FixedMetadataValue(RPGCore.getInstance(), true));
        }
        else if (activeElement == MageElement.NATURE) {
            baseDrain -= 15.0;
        }
        else if (activeElement == MageElement.LIGHT) {
            baseDrain -= 5.0; // Bổ sung dòng giảm Mana này
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0, false, false, false));
        }

        final double finalDrain = Math.max(5.0, baseDrain);

        // --- GẮN TRẠNG THÁI BAY THÔNG QUA STATUSEFFECTMANAGER ---
        player.setMetadata(metaKey, new FixedMetadataValue(RPGCore.getInstance(), level));

        // Gọi hàm của sếp: Tự động cho phép bay + Safe Fall chống chết vì ngã
        // Set duration siêu dài (72000 ticks = 1 tiếng) vì chúng ta sẽ tự tắt nó khi cạn mana
        StatusEffectManager.applyCustomHover(player, 72000, flySpeed, true);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        player.sendMessage("§a§l[!] §fNgự Không Thuật: §aBẬT §7(" + activeElement.getDisplayName() + "§7)");

        // ==========================================
        // 3. VÒNG LẶP DUY TRÌ & TRỪ MANA
        // ==========================================
        new BukkitRunnable() {
            @Override
            public void run() {
                // Nếu người chơi out game hoặc bị code khác xóa meta
                if (!player.isOnline() || player.isDead() || !player.hasMetadata(metaKey)) {
                    stopFlight(player);
                    this.cancel();
                    return;
                }

                // [SIÊU TƯƠNG THÍCH]: Nếu người chơi nhấn SHIFT, PlayerListener của sếp sẽ tự gọi removeCustomHover
                // làm cho isFlying() == false. Lập tức vòng lặp này bắt được và dọn dẹp sạch sẽ!
                if (!player.isFlying()) {
                    stopFlight(player);
                    player.sendMessage("§e§l[!] §fĐã chạm đất, tắt Ngự Không Thuật.");
                    this.cancel();
                    return;
                }

                // TRỪ MANA MỖI GIÂY
                if (rpgPlayer.getCurrentResource() >= finalDrain) {
                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - finalDrain);

                    // Particle Cơ bản
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 2, 0.2, 0.0, 0.2, 0.02);

                    // Particle & Hiệu ứng Nguyên tố duy trì
                    if (activeElement == MageElement.FIRE) {
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 3, 0.1, -0.2, 0.1, 0.05);
                    }
                    else if (activeElement == MageElement.DARK) {
                        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 5, 0.5, -0.5, 0.5, 0.05);
                        for (org.bukkit.entity.Entity e : player.getNearbyEntities(5, 5, 5)) {
                            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                                // Tính khoảng cách chiều cao từ người chơi xuống mục tiêu
                                double gap = player.getLocation().getY() - e.getLocation().getY();

                                // Chỉ làm mù kẻ địch ĐỨNG BÊN DƯỚI (gap > 0) và TỐI ĐA 5 BLOCK (gap <= 5)
                                if (gap > 0 && gap <= 5) {
                                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));
                                }
                            }
                        }
                    }
                } else {
                    stopFlight(player);
                    player.sendMessage("§c§l[!] §fCạn kiệt Năng lượng, hạ cánh khẩn cấp!");
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 20L, 20L);

        return true;
    }

    /**
     * Dọn dẹp cờ chuyên dụng của Mage và gọi StatusEffectManager để tắt bay
     */
    public static void stopFlight(Player player) {
        if (player.hasMetadata("Mage_Levitating")) {
            player.removeMetadata("Mage_Levitating", RPGCore.getInstance());
            player.removeMetadata("Levitating_Wind", RPGCore.getInstance());

            // Trả lại việc tắt Bay + Reset tốc độ cho StatusEffectManager
            StatusEffectManager.removeCustomHover(player);

            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 1.0f);
        }
    }
}