package vn.minecraft.rPGCore.auctionhouse.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.auctionhouse.models.AuctionItem;

public class AuctionCommand implements CommandExecutor {

    private final RPGCore plugin;

    public AuctionCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;

        // ==========================================
        // 1. LỆNH: /ah (MỞ MENU CHỢ) HOẶC /ah <giá> (BÁN NHANH)
        // ==========================================
        if (label.equalsIgnoreCase("ah")) {
            // Trường hợp 1: Chạy /ah trống -> Mở GUI Chợ
            if (args.length == 0) {
                // TODO: Gọi hàm mở AH_MAIN_GUI ở bài sau
                player.sendMessage("§eĐang mở giao diện Chợ Đen...");
                plugin.getAuctionGUIListener().openMainMenu(player, 1);
                return true;
            }

            // Trường hợp 2: Chạy /ah <giá> -> Bán nhanh luôn không cần hỏi
            if (args.length == 1) {
                handleSell(player, args[0], false);
                return true;
            }
        }

        // ==========================================
        // 2. LỆNH: /ahc <giá> (XÁC NHẬN TRƯỚC KHI BÁN)
        // ==========================================
        if (label.equalsIgnoreCase("ahc")) {
            if (args.length == 1) {
                handleSell(player, args[0], true);
                return true;
            } else {
                player.sendMessage("§c§l[!] §cSử dụng: /ahc <giá tiền>");
                return true;
            }
        }

        return false;
    }

    // ==========================================
    // HÀM XỬ LÝ LOGIC ĐĂNG BÁN CHUNG
    // ==========================================
    private void handleSell(Player player, String priceStr, boolean requireConfirm) {
        long price;
        try {
            price = Long.parseLong(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[!] §cGiá tiền không hợp lệ! Vui lòng nhập số nguyên dương.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c§l[!] §cBạn phải cầm vật phẩm trên tay để đăng bán!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Kiểm tra giới hạn 5 món tối đa trên sàn
        int currentListings = plugin.getAuctionManager().getPlayerActiveListingCount(player.getUniqueId());
        if (currentListings >= 5) {
            player.sendMessage("§c§l[!] §cBạn đã đạt giới hạn tối đa §e5 món đồ §cđăng bán trên chợ!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (requireConfirm) {
            // TODO: Mở GUI Xác nhận Bán (Sẽ làm ở bài tiếp theo)
            player.sendMessage("§eĐang mở Menu Xác nhận đưa vật phẩm lên sàn với giá " + plugin.getCurrencyManager().formatCurrency(price) + "...");

            plugin.getAuctionGUIListener().openConfirmSellGUI(player, item, price);
        } else {
            // BÁN NHANH TRỰC TIẾP
            // 1. Clone item lại để lưu, sau đó trừ item trên tay người chơi
            ItemStack itemToSell = item.clone();
            player.getInventory().setItemInMainHand(null);

            // 2. Tạo phiên đấu giá và lưu xuống MongoDB
            AuctionItem auctionItem = new AuctionItem(player.getUniqueId(), player.getName(), itemToSell, price);
            plugin.getAuctionManager().listNewItem(auctionItem);

            String itemName = (itemToSell.hasItemMeta() && itemToSell.getItemMeta().hasDisplayName())
                    ? itemToSell.getItemMeta().getDisplayName()
                    : itemToSell.getType().name();

            // 3. Thông báo
            player.sendMessage("§a§l[Chợ Đen] §fĐã đăng bán §e" + itemName + " §fvới giá §a" + plugin.getCurrencyManager().formatCurrency(price) + "§f!");
            player.sendMessage("§7Vật phẩm sẽ được bảo vệ trong §c15 phút đầu tiên §7(Không ai có thể mua).");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
        }
    }
}