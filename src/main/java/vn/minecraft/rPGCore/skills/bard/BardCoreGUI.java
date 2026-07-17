package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BardCoreGUI implements Listener {

    public static final String GUI_TITLE = "§8§lĐỘT PHÁ: LÕI ÂM NHẠC";

    private static final CoreData[] CORES = {
            new CoreData(1, "Dư Âm Năng Lượng", "Khi Giai Điệu nổ, lập tức hồi cố định 50 Mana cho mọi đồng minh."),
            new CoreData(2, "Hơi Thở Của Gió", "Nếu HP > 90% khi kích nổ Giai điệu: Kỹ năng đó tốn 0 Mana và giảm thêm 20% Hồi chiêu cuối."),
            new CoreData(3, "Vang Vọng Tột Cùng", "Tăng vĩnh viễn mức mở rộng Bán kính của Âm Vang từ 50% lên 80% mỗi Âm."),
            new CoreData(4, "Hợp Âm Dội Lại", "Khi Giai điệu nổ, tự động hoàn trả 1 Âm ngẫu nhiên vừa bị tiêu thụ vào bộ nhớ."),
            new CoreData(5, "Bội Âm", "Mỗi Âm đang giữ +20% Tốc chạy & +15% Kháng Hiệu Ứng. (Mất đi khi tiêu thụ)."),
            new CoreData(6, "Nghịch Âm", "Kẻ địch dính Giai điệu bị giảm 15% Kháng Phép và Giáp Vật lý."),
            new CoreData(7, "Dấu Lặng Ngân", "Giai điệu Sự Sống thanh tẩy 1 Hiệu ứng xấu ngẫu nhiên cho đồng minh."),
            new CoreData(8, "Thiên Tài Âm Nhạc", "Tăng giới hạn lưu trữ từ 3 Âm lên tối đa 4 Âm."),
            new CoreData(9, "Giai Điệu Vĩnh Cửu", "Giai điệu trúng từ 4 đồng minh trở lên lập tức giảm 50% Hồi chiêu của kỹ năng đó.")
    };

    public static void openSelectionMenu(Player player, RPGPlayer rpgPlayer) {
        // Nếu không có điểm, chặn không cho mở GUI để tránh sinh lỗi
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        if (unspent <= 0) {
            player.sendMessage("§cBạn không có điểm Đột Phá Lõi nào!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        List<CoreData> selectedCores = new ArrayList<>();

        // 1. KIỂM TRA CACHE XEM ĐÃ ROLL TRƯỚC ĐÓ CHƯA
        if (player.hasMetadata("BARD_ROLLED_CORES")) {
            String savedRolls = player.getMetadata("BARD_ROLLED_CORES").get(0).asString();
            String[] ids = savedRolls.split(",");
            for (String idStr : ids) {
                try {
                    int id = Integer.parseInt(idStr);
                    for (CoreData core : CORES) {
                        if (core.id == id) {
                            selectedCores.add(core);
                            break;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. NẾU CHƯA CÓ CACHE (Lần đầu mở) -> QUAY RANDOM VÀ LƯU LẠI
        if (selectedCores.isEmpty()) {
            List<CoreData> availableCores = new ArrayList<>();
            for (CoreData core : CORES) {
                if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + core.id) == 0) {
                    availableCores.add(core);
                }
            }

            if (availableCores.isEmpty()) {
                player.sendMessage("§aBạn đã mở khóa toàn bộ Lõi Kỹ Năng của Nhạc Sư!");
                return;
            }

            Collections.shuffle(availableCores);
            selectedCores = availableCores.subList(0, Math.min(3, availableCores.size()));

            // Lưu kết quả lại vào Metadata (Ví dụ: "1,5,8")
            StringBuilder savedMeta = new StringBuilder();
            for (CoreData core : selectedCores) {
                savedMeta.append(core.id).append(",");
            }
            player.setMetadata("BARD_ROLLED_CORES", new FixedMetadataValue(RPGCore.getInstance(), savedMeta.toString()));
        }

        // 3. VẼ GUI VỚI KẾT QUẢ ĐÃ CHỐT
        int[] slots = {11, 13, 15}; // Vị trí cân xứng giữa GUI
        for (int i = 0; i < selectedCores.size(); i++) {
            CoreData core = selectedCores.get(i);
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.AMETHYST_CLUSTER);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d§l[Lõi Kỹ Năng] §e" + core.name);
                meta.setLore(Arrays.asList(
                        "§7" + core.description,
                        "",
                        "§a► Nhấp chuột để Thức Tỉnh Lõi này!"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.AMETHYST_CLUSTER) return;
        if (!clickedItem.hasItemMeta()) return;

        String name = clickedItem.getItemMeta().getDisplayName();
        int coreId = -1;

        // FIX LỖI UI: So sánh trực tiếp với tên của Core thay vì chuỗi hardcode bị sai
        for (CoreData core : CORES) {
            if (name.contains(core.name)) {
                coreId = core.id;
                break;
            }
        }

        if (coreId != -1) {
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");

            if (unspent > 0) {
                // Trừ điểm và nâng cấp lõi
                rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent - 1);
                rpgPlayer.getSkillData().setSkillLevel("BARD_CORE_" + coreId, 1);

                // XÓA CACHE ĐỂ LẦN MUA SAU ĐƯỢC ROLL LẠI TỪ ĐẦU
                player.removeMetadata("BARD_ROLLED_CORES", RPGCore.getInstance());

                player.sendMessage("§a§lĐỘT PHÁ THÀNH CÔNG! §fBạn đã thức tỉnh Lõi: §d" + name);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
                player.closeInventory();
            } else {
                player.sendMessage("§cBạn không đủ điểm Đột Phá Lõi!");
                player.closeInventory();
            }
        }
    }

    private static class CoreData {
        int id; String name; String description;
        CoreData(int id, String name, String description) { this.id = id; this.name = name; this.description = description; }
    }
}