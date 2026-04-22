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
    // [SIÊU HÀM 1]: KIỂM TRA COOLDOWN, MANA & TRỪ MANA
    // Trả về true nếu đủ điều kiện thi triển
    // ==========================================
    default boolean handleCostAndCooldown(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();

        // 1. Kiểm tra Cooldown [FIX LỖI INDEX OUT OF BOUNDS]
        if (player.hasMetadata(cdKey)) {
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata(cdKey);

            // Kiểm tra chắc chắn rằng danh sách không bị rỗng (Chống lỗi rác bộ nhớ)
            if (!metaList.isEmpty()) {
                long expire = metaList.get(0).asLong();
                if (System.currentTimeMillis() < expire) {
                    double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                    player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                    return false;
                }
            }
            // Nếu đã hết hạn HOẶC danh sách bị rỗng do lỗi máy chủ -> Xóa sạch rác
            player.removeMetadata(cdKey, vn.minecraft.rPGCore.RPGCore.getInstance());
        }

        // 2. Kiểm tra Năng lượng
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        // 3. Trừ Năng lượng
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        return true;
    }

    // ==========================================
    // [SIÊU HÀM 2]: KÍCH HOẠT HỒI CHIÊU
    // Gọi hàm này khi chiêu thức đã hoàn tất để khóa Cooldown
    // ==========================================
    default void applyGlobalCooldown(Player player, RPGPlayer rpgPlayer, int level) {
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("CD_" + getId(), new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);
    }
}