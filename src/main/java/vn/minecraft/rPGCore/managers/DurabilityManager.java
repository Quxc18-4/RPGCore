package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;

public class DurabilityManager {

    // Trả về true nếu item bị biến mất (phá hủy)
    public static boolean damageItem(Player player, RPGPlayer rpgPlayer, ItemStack item, int amount) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        if (item.getItemMeta().getDisplayName().startsWith("§8§m")) return false; // Đã hỏng rồi thì không trừ độ bền nữa

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        int duraLineIndex = -1;
        int currentDura = -1;
        int maxDura = -1;
        int livesLineIndex = -1;
        int currentLives = -1;

        for (int i = 0; i < lore.size(); i++) {
            String clean = ChatColor.stripColor(lore.get(i)).trim();
            if (clean.startsWith("- Độ bền:")) {
                duraLineIndex = i;
                String[] parts = clean.replace("- Độ bền:", "").trim().split("/");
                try {
                    currentDura = Integer.parseInt(parts[0]);
                    maxDura = Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
            } else if (clean.startsWith("- Cơ hội bảo vệ:")) {
                livesLineIndex = i;
                String[] parts = clean.replace("- Cơ hội bảo vệ:", "").trim().split("/");
                try { currentLives = Integer.parseInt(parts[0]); } catch (Exception ignored) {}
            }
        }

        if (duraLineIndex == -1 || currentDura <= 0) return false;

        currentDura -= amount;

        if (currentDura <= 0) {
            currentDura = 0;
            if (currentLives > 0) {
                // Cơ chế phá hủy: Mạng 3 = 30%, Mạng 2 = 60%, Mạng 1 = 100% vỡ
                double breakChance = (currentLives == 3) ? 0.30 : (currentLives == 2 ? 0.60 : 1.0);

                if (Math.random() <= breakChance) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    player.sendMessage("§c§l[!] Món trang bị của bạn đã VỠ VỤN do vượt quá giới hạn chịu đựng!");
                    item.setAmount(0);
                    rpgPlayer.updateEquipmentStats();
                    return true;
                } else {
                    currentLives--;
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 0.5f);
                    player.sendMessage("§e§l[!] Trang bị đã hỏng! Mất 1 cơ hội bảo vệ và Tạm thời vô hiệu hóa.");

                    lore.set(duraLineIndex, "§7 - Độ bền: §c0§8/§a" + maxDura);
                    if (livesLineIndex != -1) lore.set(livesLineIndex, "§7 - Cơ hội bảo vệ: §e" + currentLives + "§8/§e3");

                    // Chỉ gạch ngang tên màu Xám để đánh dấu Hỏng
                    String oldName = meta.getDisplayName();
                    meta.setDisplayName("§8§m" + ChatColor.stripColor(oldName));

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    rpgPlayer.updateEquipmentStats(); // Re-scan chỉ số (Nó sẽ tự bỏ qua món đồ này)
                    return false;
                }
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                item.setAmount(0);
                rpgPlayer.updateEquipmentStats();
                return true;
            }
        } else {
            lore.set(duraLineIndex, "§7 - Độ bền: §a" + currentDura + "§8/§a" + maxDura);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return false;
        }
    }
}