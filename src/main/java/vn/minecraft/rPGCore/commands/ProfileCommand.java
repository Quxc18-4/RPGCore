package vn.minecraft.rPGCore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ProfileCommand implements CommandExecutor {

    private final PlayerManager playerManager;

    public ProfileCommand(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        if (rpgPlayer == null) return true;

        // Vẽ bảng Profile
        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("           §6§lHỒ SƠ NHÂN VẬT");
        player.sendMessage("");
        player.sendMessage(" §7▶ §fNgười chơi: §e" + player.getName());
        player.sendMessage(" §7▶ §fLớp nhân vật: " + ChatColor.translateAlternateColorCodes('&', rpgPlayer.getCurrentClass().getDisplayName()));
        player.sendMessage(" §7▶ §fCấp độ: §a" + player.getLevel() + " §8(§7" + (int)rpgPlayer.getCurrentExp() + "§8/§7" + (int)rpgPlayer.getRequiredExp() + " XP§8)");

        // Thanh tiến trình EXP mini
        player.sendMessage("    " + getProgressBar(rpgPlayer.getCurrentExp(), rpgPlayer.getRequiredExp()));
        player.sendMessage("");
        player.sendMessage(" §6§lTRANG BỊ ĐANG MẶC:");

        // Kiểm tra trang bị
        player.sendMessage(" §e⛨ §fMũ: " + getItemName(player.getInventory().getHelmet()));
        player.sendMessage(" §e⛨ §fGiáp: " + getItemName(player.getInventory().getChestplate()));
        player.sendMessage(" §e⛨ §fQuần: " + getItemName(player.getInventory().getLeggings()));
        player.sendMessage(" §e⛨ §fGiày: " + getItemName(player.getInventory().getBoots()));
        player.sendMessage("");
        player.sendMessage(" §6§lVŨ KHÍ CHÍNH:");
        player.sendMessage(" §c⚔ §fCầm tay: " + getItemName(player.getInventory().getItemInMainHand()));
        player.sendMessage("");
        player.sendMessage("§8§m---------------------------------------");

        return true;
    }

    // Hàm lấy tên vật phẩm thân thiện
    private String getItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "§8[Trống]";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return "§b[" + item.getItemMeta().getDisplayName() + "§b]";
        }
        // Nếu không có tên tùy chỉnh, lấy tên mặc định của Minecraft
        return "§f[" + item.getType().name().replace("_", " ").toLowerCase() + "]";
    }

    // Hàm vẽ thanh tiến trình EXP
    private String getProgressBar(double current, double max) {
        int barSize = 20;
        int completed = (int) ((current / max) * barSize);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barSize; i++) {
            if (i < completed) bar.append("§a■");
            else bar.append("§7■");
        }
        bar.append("§8]");
        return bar.toString();
    }
}