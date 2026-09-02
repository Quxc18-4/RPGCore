package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    // Gộp 2 map cũ thành 1 map duy nhất quản lý Status Bar
    private final Map<UUID, BossBar> statusBars = new HashMap<>();

    public void createBars(Player player, RPGPlayer rpgPlayer) {
        UUID uuid = player.getUniqueId();

        // Lấy thông tin ResourceType để quyết định màu sắc thanh bar
        ResourceType resType = rpgPlayer.getCurrentClass().getResourceType();

        BossBar statusBar = Bukkit.createBossBar(
                formatStatusTitle(rpgPlayer),
                resType.getColor(),
                BarStyle.SEGMENTED_10
        );
        statusBar.addPlayer(player);
        statusBars.put(uuid, statusBar);

        updateBars(player, rpgPlayer);
    }

    public void updateBars(Player player, RPGPlayer rpgPlayer) {
        UUID uuid = player.getUniqueId();
        if (!statusBars.containsKey(uuid)) return;

        BossBar statusBar = statusBars.get(uuid);

        // 1. Cập nhật Text kẹp 2 thông số
        statusBar.setTitle(formatStatusTitle(rpgPlayer));

        // 2. Giữ nguyên màu sắc theo config của ResourceType
        statusBar.setColor(rpgPlayer.getCurrentClass().getResourceType().getColor());

        // 3. Tụt thanh bar dựa trên tiến độ tiêu hao Năng lượng (Resource)
        double resProgress = rpgPlayer.getCurrentResource() / rpgPlayer.getTotalMaxResource();
        statusBar.setProgress(Math.max(0.0, Math.min(1.0, resProgress)));
    }

    public void removeBars(Player player) {
        UUID uuid = player.getUniqueId();
        if (statusBars.containsKey(uuid)) {
            statusBars.get(uuid).removeAll();
            statusBars.remove(uuid);
        }
    }

    // Hàm gộp Title hiển thị Máu và Năng lượng + Bổ sung Icon
    private String formatStatusTitle(RPGPlayer p) {
        ResourceType type = p.getCurrentClass().getResourceType();
        String colorCode;

        // Phân loại Icon và Mã màu (Color Code) theo từng loại Resource
        switch (type) {
            case MANA:
                colorCode = "§b";
                break;
            case RAGE:
                colorCode = "§5"; // Tím, chuẩn với BarColor.PURPLE
                break;
            case STAMINA:
            default:
                colorCode = "§e";
                break;
        }

        int curHp = (int) p.getCurrentHealth();
        int maxHp = (int) p.getTotalMaxHealth();
        int curRes = (int) p.getCurrentResource();
        int maxRes = (int) p.getTotalMaxResource();

        // Trả về định dạng: HP: xxxx/xxxx | Resource: xxxx/xxxx
        return "§cHP: " + curHp + "/" + maxHp + " §8| " + colorCode + " " + type.getName() + ": " + curRes + "/" + maxRes;
    }
}