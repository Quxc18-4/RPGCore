package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.managers.VipManager;

public class VipCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final VipManager vipManager;

    public VipCommand(JavaPlugin plugin, VipManager vipManager) {
        this.plugin = plugin;
        this.vipManager = vipManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("buy")) {
            vipManager.purchaseVipWithGold(player);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String summary = vipManager.getVipStatusSummary(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage("§6§l========== HỆ THỐNG ĐẶC QUYỀN VIP ==========");
                player.sendMessage("§eTrạng thái: " + summary);
                player.sendMessage("§fQuyền lợi VIP:");
                player.sendMessage(" §a§fVào máy chủ ngay cả khi đã đầy người chơi.");
                player.sendMessage(" §a§fMở khóa vĩnh viễn §eSlot nhân vật thứ 3 §f(lần mua đầu).");
                player.sendMessage(" §a§fDanh hiệu §6§l[VIP] §ftrên Tablist & Khung Chat.");
                player.sendMessage(" §a§fTra cứu tọa độ NPC & Waystone trên Web Portal.");
                player.sendMessage("§eCách kích hoạt (30 ngày):");
                player.sendMessage(" §7• Dùng §e/vip buy §7để mua bằng §e100 Vàng§7.");
                player.sendMessage(" §7• Hoặc nạp gói §a50.000đ §7trên Website.");
                player.sendMessage("§6§l============================================");
            });
        });

        return true;
    }
}