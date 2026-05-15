package vn.minecraft.rPGCore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;

    public ConsumableListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        boolean consumed = false;

        // 1. Logic Bình Máu
        if (itemId.equals("potion_hp_1")) {
            double healAmount = rpgPlayer.getTotalMaxHealth() * 0.10; // Hồi 10%
            rpgPlayer.heal(healAmount);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            consumed = true;
        }

        // 2. Logic Tinh Thể Nguyên Tố
        else if (itemId.startsWith("crystal_")) {
            if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("mage")) {
                player.sendMessage("§cChỉ Pháp Sư mới có thể hấp thụ Tinh Thể Nguyên Tố!");
                return;
            }

            String elementToUnlock = itemId.replace("crystal_", "").toUpperCase();

            if (rpgPlayer.hasElement(elementToUnlock)) {
                player.sendMessage("§eBạn đã sở hữu Nguyên Tố này rồi!");
                return;
            }

            rpgPlayer.addElement(elementToUnlock);
            player.sendMessage("§a§l[!] §fBạn đã hấp thụ thành công §dNguyên Tố " + elementToUnlock + "§f!");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            consumed = true;
        }

        // 3. Logic Mở Túi Vũ Khí & Phòng Cụ (8 Tier)
        else if (itemId.startsWith("bag_weapon_") || itemId.startsWith("bag_armor_")) {
            // Nhận diện Loại túi
            String type = itemId.startsWith("bag_weapon_") ? "weapon" : "armor";
            String typeName = type.equals("weapon") ? "Vũ Khí" : "Phòng Cụ";

            // Nhận diện Tier (0 -> 3)
            int tierIndex = 0;
            if (itemId.endsWith("uncommon")) tierIndex = 1;
            else if (itemId.endsWith("rare")) tierIndex = 2;
            else if (itemId.endsWith("epic")) tierIndex = 3;

            // Lấy cấp độ hiện tại của người chơi để đúc đồ
            int playerLevel = player.getLevel();
            if (playerLevel <= 0) playerLevel = 1; // Đảm bảo tối thiểu cấp 1

            player.sendMessage("§a§l[!] §fĐang mở Túi " + typeName + "...");

            // Gọi API từ EquipmentCommand: Truyền tierIndex cố định, mọi chỉ số phụ khác random (-1 / null)
            ItemStack gachaItem = vn.minecraft.rPGCore.commands.EquipmentCommand.createGachaItem(
                    type, playerLevel, null, tierIndex, null, -1, -1
            );
            player.getInventory().addItem(gachaItem);

            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            consumed = true;
        }

        // 3. Trừ Item sau khi dùng thành công
        if (consumed) {
            event.setCancelled(true); // Cản sự kiện đặt block (nếu item là block)
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}