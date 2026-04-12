package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.ElementalSwapSkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ElementRotationGUI implements Listener {

    private static final String GUI_TITLE = "§8§lSẮP XẾP NGUYÊN TỐ";
    private static final int[] ORDER_SLOTS = {10, 11, 12, 13, 14, 15}; // Hàng 2
    private static final int[] ELEMENT_SLOTS = {19, 20, 21, 22, 23, 24}; // Hàng 3

    public static void openGUI(Player player, RPGPlayer rpgPlayer) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);

        // 1. Fill Background (Hàng 1, 4 và các viền hàng 2,3)
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 36; i++) inv.setItem(i, bg);

        // Xóa viền đen ở các ô tác vụ
        for (int slot : ORDER_SLOTS) inv.setItem(slot, new ItemStack(Material.AIR));
        for (int slot : ELEMENT_SLOTS) inv.setItem(slot, new ItemStack(Material.AIR));

        // 2. Lấy danh sách Rotation hiện tại
        List<String> unlocked = rpgPlayer.getUnlockedElements();
        String rotationStr = player.getPersistentDataContainer().getOrDefault(ElementalSwapSkill.ROTATION_KEY, PersistentDataType.STRING, String.join(",", unlocked));
        List<String> rotationList = new ArrayList<>(Arrays.asList(rotationStr.split(",")));
        rotationList.removeIf(String::isEmpty);

        // 3. Render Hàng 2: Chuỗi xoay vòng
        for (int i = 0; i < 6; i++) {
            if (i < rotationList.size()) {
                MageElement el = MageElement.valueOf(rotationList.get(i));
                ItemStack icon = getElementIcon(el);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName("§e§lThứ tự " + (i + 1) + ": " + el.getDisplayName());
                meta.setLore(Arrays.asList("§7Nhấp để đổi hoặc xóa", "§f[Chuột Phải] §cĐể gỡ bỏ"));
                icon.setAmount(i + 1); // Hiển thị số lượng 1,2,3...
                icon.setItemMeta(meta);
                inv.setItem(ORDER_SLOTS[i], icon);
            } else {
                ItemStack empty = createItem(Material.WHITE_STAINED_GLASS_PANE, "§f§lThứ tự " + (i + 1), "§7Ô trống. Hãy nhấp vào 1 nguyên tố", "§7ở dưới rồi nhấp lên đây để gắn.");
                empty.setAmount(i + 1);
                inv.setItem(ORDER_SLOTS[i], empty);
            }
        }

        // 4. Render Hàng 3: Danh sách Nguyên Tố người chơi sở hữu
        for (int i = 0; i < 6; i++) {
            if (i < unlocked.size()) {
                MageElement el = MageElement.valueOf(unlocked.get(i));

                // Nếu đang có metadata "chọn", làm item nhấp nháy/Enchant
                boolean isSelected = player.hasMetadata("MageUI_Selected_Element") && player.getMetadata("MageUI_Selected_Element").get(0).asString().equals(el.name());

                ItemStack icon = getElementIcon(el);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(el.getDisplayName());
                if (isSelected) {
                    meta.setLore(Arrays.asList("§a§lĐANG CHỌN!", "§7Hãy nhấp vào Hàng trên để gắn."));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.setLore(Arrays.asList("§f[Chuột Trái] §aChọn nguyên tố này"));
                }
                icon.setItemMeta(meta);
                inv.setItem(ELEMENT_SLOTS[i], icon);
            } else {
                inv.setItem(ELEMENT_SLOTS[i], createItem(Material.BARRIER, "§cChưa mở khóa"));
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        int slot = event.getSlot();

        // 1. NHẤP VÀO HÀNG 3 (Chọn Nguyên Tố)
        for (int i = 0; i < ELEMENT_SLOTS.length; i++) {
            if (slot == ELEMENT_SLOTS[i]) {
                List<String> unlocked = rpgPlayer.getUnlockedElements();
                if (i < unlocked.size()) {
                    String selectedElement = unlocked.get(i);
                    player.setMetadata("MageUI_Selected_Element", new FixedMetadataValue(RPGCore.getInstance(), selectedElement));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                    openGUI(player, rpgPlayer); // Reload GUI để hiển thị enchant nhấp nháy
                }
                return;
            }
        }

        // 2. NHẤP VÀO HÀNG 2 (Gắn / Gỡ Nguyên Tố)
        for (int i = 0; i < ORDER_SLOTS.length; i++) {
            if (slot == ORDER_SLOTS[i]) {
                String rotationStr = player.getPersistentDataContainer().getOrDefault(ElementalSwapSkill.ROTATION_KEY, PersistentDataType.STRING, "");
                List<String> rotationList = new ArrayList<>(Arrays.asList(rotationStr.split(",")));
                rotationList.removeIf(String::isEmpty);

                // Nếu click phải -> Gỡ nguyên tố khỏi slot
                if (event.getClick().isRightClick()) {
                    if (i < rotationList.size()) {
                        rotationList.remove(i);
                        saveAndReload(player, rpgPlayer, rotationList);
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1.5f);
                    }
                    return;
                }

                // Nếu click trái -> Ráp nguyên tố đang chọn vào slot
                if (player.hasMetadata("MageUI_Selected_Element")) {
                    String selected = player.getMetadata("MageUI_Selected_Element").get(0).asString();

                    if (i < rotationList.size()) {
                        rotationList.set(i, selected); // Đổi chỗ
                    } else {
                        // Thêm vào cuối cùng (nếu bấm ô xa quá thì nó tự dồn vào đuôi)
                        rotationList.add(selected);
                    }

                    saveAndReload(player, rpgPlayer, rotationList);
                    player.removeMetadata("MageUI_Selected_Element", RPGCore.getInstance());
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                } else {
                    player.sendMessage("§cHãy chọn 1 Nguyên tố ở hàng dưới trước!");
                }
                return;
            }
        }
    }

    private void saveAndReload(Player player, RPGPlayer rpgPlayer, List<String> rotationList) {
        // Ràng buộc: Ít nhất phải có 1 nguyên tố trong danh sách
        if (rotationList.isEmpty() && !rpgPlayer.getUnlockedElements().isEmpty()) {
            rotationList.add(rpgPlayer.getUnlockedElements().get(0));
        }

        String newStr = String.join(",", rotationList);
        player.getPersistentDataContainer().set(ElementalSwapSkill.ROTATION_KEY, PersistentDataType.STRING, newStr);
        openGUI(player, rpgPlayer);
    }

    private static ItemStack getElementIcon(MageElement element) {
        Material mat;
        switch (element) {
            case FIRE: mat = Material.BLAZE_POWDER; break;
            case WATER: mat = Material.WATER_BUCKET; break;
            case WIND: mat = Material.FEATHER; break;
            case NATURE: mat = Material.OAK_SAPLING; break;
            case LIGHT: mat = Material.GLOWSTONE_DUST; break;
            case DARK: mat = Material.COAL; break;
            default: mat = Material.PAPER; break;
        }
        return new ItemStack(mat);
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}