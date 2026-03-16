package vn.minecraft.rPGCore.skills;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class AbstractEnhancementSkill implements ISkill {

    protected final JavaPlugin plugin;

    public AbstractEnhancementSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Hàm dùng chung để áp dụng các Cường hóa Vũ khí (Weapon Buffs).
     * Tự động quản lý việc Ghi đè (Overwrite) và Hẹn giờ hủy (Expiration).
     *
     * @param player        Người chơi nhận Buff
     * @param buffKey       Mã Metadata riêng của chiêu (VD: "Assassin_FatalBlow_Level")
     * @param buffValue     Giá trị của Buff (Cấp độ, lượng hút máu, v.v...)
     * @param durationTicks Thời gian tồn tại (bằng Ticks)
     * @param castMessage   Câu thoại khi bật chiêu
     * @param expireMessage Câu thoại khi hết hạn chiêu
     */
    protected void applyWeaponEnhancement(Player player, String buffKey, Object buffValue, long durationTicks, String castMessage, String expireMessage) {

        // ==========================================
        // 1. KIỂM TRA VÀ GHI ĐÈ BUFF CŨ (TOÀN CỤC)
        // ==========================================
        if (player.hasMetadata("Active_Weapon_Enhancement")) {
            String oldBuffKey = player.getMetadata("Active_Weapon_Enhancement").get(0).asString();

            // Nếu phát hiện đang có 1 Buff khác với Buff chuẩn bị dùng
            if (!oldBuffKey.equals(buffKey)) {
                // Kiểm tra xem Buff cũ đó chưa bị xài mất thì mới xóa và báo Ghi đè
                if (player.hasMetadata(oldBuffKey)) {
                    player.removeMetadata(oldBuffKey, plugin);
                    player.sendMessage("§8§o(Cường hóa vũ khí trước đó đã bị ghi đè...)");
                }
            }
        }

        // ==========================================
        // 2. ÁP DỤNG BUFF MỚI
        // ==========================================
        player.setMetadata(buffKey, new FixedMetadataValue(plugin, buffValue));
        player.setMetadata("Active_Weapon_Enhancement", new FixedMetadataValue(plugin, buffKey));

        // Đánh dấu mốc thời gian (Timestamp) để chống lỗi BukkitRunnable xóa nhầm nếu spam chiêu liên tục
        long applyTime = System.currentTimeMillis();
        player.setMetadata(buffKey + "_Time", new FixedMetadataValue(plugin, applyTime));

        if (castMessage != null && !castMessage.isEmpty()) {
            player.sendMessage(castMessage);
        }

        // ==========================================
        // 3. HẸN GIỜ HỦY BUFF THÔNG MINH
        // ==========================================
        new BukkitRunnable() {
            @Override
            public void run() {
                // Nếu người chơi online và Buff vẫn còn tồn tại (chưa bị đánh mất)
                if (player.isOnline() && player.hasMetadata(buffKey)) {

                    // Chỉ hủy nếu đây đúng là cái Buff do chính Task này sinh ra
                    if (player.hasMetadata(buffKey + "_Time") && player.getMetadata(buffKey + "_Time").get(0).asLong() == applyTime) {

                        player.removeMetadata(buffKey, plugin);
                        player.removeMetadata(buffKey + "_Time", plugin);

                        // Xóa luôn con trỏ Active để làm sạch dữ liệu
                        if (player.hasMetadata("Active_Weapon_Enhancement") && player.getMetadata("Active_Weapon_Enhancement").get(0).asString().equals(buffKey)) {
                            player.removeMetadata("Active_Weapon_Enhancement", plugin);
                        }

                        if (expireMessage != null && !expireMessage.isEmpty()) {
                            player.sendMessage(expireMessage);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }
}