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
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        List<CoreData> availableCores = new ArrayList<>();
        for (CoreData core : CORES) {
            if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + core.id) == 0) {
                availableCores.add(core);
            }
        }

        if (availableCores.isEmpty()) {
            player.sendMessage("§cBạn đã học toàn bộ Lõi Nâng Cấp!");
            return;
        }

        Collections.shuffle(availableCores);
        List<CoreData> selectedCores = availableCores.subList(0, Math.min(3, availableCores.size()));

        int[] slots = {11, 13, 15};
        for (int i = 0; i < selectedCores.size(); i++) {
            CoreData core = selectedCores.get(i);
            ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§d§l[Lõi Nâng Cấp [" + core.id + "] ] §f" + core.name);
            meta.setLore(Arrays.asList("§7" + core.description, "", "§eNhấp chuột để chọn Lõi này!"));
            item.setItemMeta(meta);
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

        String name = clickedItem.getItemMeta().getDisplayName();
        int coreId = -1;
        for (CoreData core : CORES) {
            if (name.contains("Lõi Cấp " + core.id)) { coreId = core.id; break; }
        }

        if (coreId != -1) {
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");

            if (unspent > 0) {
                rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent - 1);
                rpgPlayer.getSkillData().setSkillLevel("BARD_CORE_" + coreId, 1);
                player.sendMessage("§a§lĐỘT PHÁ THÀNH CÔNG! §fBạn đã thức tỉnh: §d" + name);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
                player.closeInventory();
            }
        }
    }

    private static class CoreData {
        int id; String name; String description;
        CoreData(int id, String name, String description) { this.id = id; this.name = name; this.description = description; }
    }
}