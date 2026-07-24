package vn.minecraft.rPGCore.auctionhouse.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.auctionhouse.models.AuctionItem;

import java.util.List;

public class AuctionTaxTask extends BukkitRunnable {

    private final RPGCore plugin;

    public AuctionTaxTask(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        // Lấy tất cả các món đồ ĐANG TRÊN SÀN
        List<AuctionItem> activeItems = plugin.getAuctionManager().getAllActiveAuctions();

        for (AuctionItem item : activeItems) {
            // Nếu đã vượt qua thời hạn đóng thuế (7 ngày đầu hoặc 3 ngày của kỳ trước)
            if (currentTime >= item.getNextTaxTime()) {

                // Tính thuế 5% giá trị niêm yết (Tối thiểu 1 Đồng)
                long taxAmount = (long) Math.max(1, item.getPrice() * 0.05);

                // Tận dụng hàm Offline đã viết ở dự án trước để trừ thẳng vào Ngân hàng
                boolean paymentSuccess = plugin.getCurrencyManager().processOfflinePayment(item.getSellerId(), taxAmount);

                Player seller = Bukkit.getPlayer(item.getSellerId());
                String itemName = (item.getItem().hasItemMeta() && item.getItem().getItemMeta().hasDisplayName())
                        ? item.getItem().getItemMeta().getDisplayName() : item.getItem().getType().name();

                if (paymentSuccess) {
                    // Trừ tiền thành công -> Gia hạn thêm 3 ngày
                    item.setNextTaxTime(currentTime + (3L * 24 * 60 * 60 * 1000));
                    plugin.getAuctionManager().updateActiveItem(item);

                    // Nếu người bán đang Online thì gửi thông báo trừ tiền
                    if (seller != null && seller.isOnline()) {
                        seller.sendMessage("§e§l[Sở Thuế Chợ Đen] §fĐã tự động thu §c" + plugin.getCurrencyManager().formatCurrency(taxAmount) + " §fphí duy trì 3 ngày cho vật phẩm: §a" + itemName);
                    }
                } else {
                    // Không đủ tiền -> Tịch thu & Xóa vĩnh viễn khỏi Database!
                    plugin.getAuctionManager().deleteAuctionItem(item.getId());

                    // Cảnh báo khẩn cấp nếu họ đang Online
                    if (seller != null && seller.isOnline()) {
                        seller.sendMessage("§c§l[!] CẢNH BÁO TỪ SỞ THUẾ CHỢ ĐEN §c§l[!]");
                        seller.sendMessage("§fVật phẩm §e" + itemName + " §fcủa bạn đã bị §cTỊCH THU VĨNH VIỄN §fdo không đủ tài sản trả phí duy trì 5% (" + plugin.getCurrencyManager().formatCurrency(taxAmount) + ")!");
                        seller.playSound(seller.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    } else {
                        // In ra Console để Admin nắm được ai vừa bị khóa đồ
                        plugin.getLogger().info("[Chợ Đen] Đã tịch thu vật phẩm " + itemName + " của người chơi " + item.getSellerName() + " do nợ thuế!");
                    }
                }
            }
        }
    }
}