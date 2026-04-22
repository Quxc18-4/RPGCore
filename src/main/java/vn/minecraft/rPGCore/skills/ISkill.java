package vn.minecraft.rPGCore.skills;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;
import java.util.Map;

public interface ISkill {
    String getId();               // VD: "archer_spread"
    String getName();             // VD: "Mưa Tên Kép"
    String getRequiredClass();    // VD: "archer"
    Material getIcon();           // Icon hiển thị trên Menu

    int getMaxLevel();
    double getManaCost(int level);
    long getCooldown(int level);  // Trả về Milliseconds

    // ==========================================
    // [MỚI THÊM]: HÀM TỰ ĐỘNG TÍNH TOÁN COOLDOWN THỰC TẾ
    // ==========================================
    default long getFinalCooldown(RPGPlayer rpgPlayer, long baseCd) {
        double cdrPct = rpgPlayer.getTotalStatsMap().getOrDefault("Giảm hồi chiêu", 0.0);
        return (long) (baseCd * (1.0 - (cdrPct / 100.0)));
    }

    // ==========================================
    // [MỚI THÊM]: TASK BÁO HỒI CHIÊU XONG (DÙNG CHUNG)
    // ==========================================
    default void startCooldownNotifier(Player player, long finalCdMillis) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Tự động gọi getName() của class hiện tại để thông báo tên chiêu
                    player.sendMessage("§a§l[!] §fKỹ năng §e" + getName() + " §fđã sẵn sàng!");
                }
            }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), finalCdMillis / 50L);
    }

    // Tự động sinh mô tả dựa trên cấp độ hiện tại
    List<String> getDescription(int level);

    // ==========================================
    // [MỚI]: HÀM SINH LORE ĐỘNG THEO NGƯỜI CHƠI (DYNAMIC LORE)
    // ==========================================
    default List<String> getDescription(Player player, int level) {
        return getDescription(level);
    }

    // Hàm thực thi chính khi bấm đúng Combo
    boolean execute(Player player, RPGPlayer rpgPlayer, int level);
    // Thêm hàm này để mỗi kỹ năng tự quyết định giá SP khi nâng cấp
    int getUpgradeCost(int currentLevel);
    // Trả về danh sách Kỹ năng yêu cầu. Key = ID kỹ năng, Value = Level tối thiểu
    Map<String, Integer> getRequiredSkills();
    // Thêm hàm để quy định cấp độ tối thiểu của người chơi (Mặc định là 1 nếu không cài đặt)
    default int getRequiredPlayerLevel() { return 1; }
    // [MỚI]: Phân biệt Kỹ năng Nội tại (Passive) và Chủ động (Active)
    default boolean isPassiveSkill() { return false; }

    // ==========================================
    // [MỚI]: YÊU CẦU NGUYÊN TỐ (Dành cho Kỹ năng chuyên biệt của Mage)
    // ==========================================
    default List<String> getRequiredElements() {
        return java.util.Collections.emptyList(); // Mặc định không yêu cầu nguyên tố nào
    }

    // ==========================================
    // [HỆ THỐNG CHARGE TỔNG QUÁT]: SỐ LƯỢT TÍCH TRỮ
    // ==========================================
    // Trả về số lượt tích trữ tối đa. (Truyền Player để lỡ có check theo Buff/Nguyên tố)
    default int getMaxCharges(Player player, int level) { return 1; }

    // ==========================================
    // [SIÊU HÀM]: KIỂM TRA & TRỪ MANA / COOLDOWN / CHARGE
    // Dùng cái này ở đầu hàm execute() của MỌI KỸ NĂNG để tiết kiệm hàng chục dòng code.
    // ==========================================
    default boolean handleCostAndCooldown(Player player, RPGPlayer rpgPlayer, int level) {
        int maxCharges = getMaxCharges(player, level);
        String cdKey = "CD_" + getId();
        String chargeKey = "Charges_" + getId();

        int currentCharges = maxCharges;
        if (maxCharges > 1 && player.hasMetadata(chargeKey)) {
            currentCharges = player.getMetadata(chargeKey).get(0).asInt();
        }

        // 1. Kiểm tra Cooldown (Dành cho Skill thường, hoặc Skill Charge nhưng đã xài hết lượt)
        if ((maxCharges <= 1 || currentCharges <= 0) && player.hasMetadata(cdKey)) {
            long expire = player.getMetadata(cdKey).get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        // 2. Kiểm tra Mana
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        // 3. Thực thi (Trừ Mana)
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        // 4. Xử lý Trừ Charge & Kích hoạt Hồi chiêu
        if (maxCharges > 1) {
            currentCharges--;
            player.setMetadata(chargeKey, new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), currentCharges));

            if (currentCharges == maxCharges - 1) {
                // Vừa xài 1 charge từ mốc đầy -> Bắt đầu đếm ngược hồi Charge
                startRechargeTask(player, rpgPlayer, level);
            }
            if (currentCharges == 0) {
                // Xài cạn sạch -> Đánh dấu Cooldown cứng lên UI
                player.setMetadata(cdKey, new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
            }
        } else {
            // Kỹ năng bình thường (Max Charge = 1)
            player.setMetadata(cdKey, new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
            startCooldownNotifier(player, finalCd);
        }

        return true;
    }

    // Task tự động hồi Charge ngầm
    default void startRechargeTask(Player player, RPGPlayer rpgPlayer, int level) {
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                String chargeKey = "Charges_" + getId();
                int maxCharges = getMaxCharges(player, level);
                int current = player.hasMetadata(chargeKey) ? player.getMetadata(chargeKey).get(0).asInt() : 0;

                if (current < maxCharges) {
                    current++;
                    player.setMetadata(chargeKey, new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), current));

                    if (current == maxCharges) {
                        player.sendMessage("§a§l[!] §fKỹ năng §e" + getName() + " §fđã hồi đủ §e" + maxCharges + " Lượt§f!");
                    } else {
                        // Vẫn chưa đầy, tiếp tục hồi lượt tiếp theo
                        startRechargeTask(player, rpgPlayer, level);
                    }
                }
            }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), finalCd / 50L);
    }
}