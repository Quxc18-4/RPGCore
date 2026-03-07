package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.managers.ArrowRecipeManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdminArrowCraftingUI implements Listener {

    private final ArrowRecipeManager recipeManager;
    public static final String ADMIN_UI_TITLE = "§c§l[ADMIN] TẠO CÔNG THỨC";

    // Vị trí các ô (Giống hệt UI của người chơi)
    private final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private final int RESULT_SLOT = 23;
    private final int SAVE_BTN_SLOT = 41;

    public AdminArrowCraftingUI(ArrowRecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public void openAdminGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ADMIN_UI_TITLE);

        // 1. Phủ kính đen Background (Ngoại trừ ô Input và Result)
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 54; i++) {
            if (!isInputSlot(i) && i != RESULT_SLOT) {
                inv.setItem(i, bg);
            }
        }

        // 2. Nút Lưu Công thức
        inv.setItem(SAVE_BTN_SLOT, createItem(Material.LIME_DYE, "§a§lLƯU CÔNG THỨC",
                "§7Hãy bỏ nguyên liệu vào lưới 3x3",
                "§7Bỏ kết quả (Mũi tên) vào ô trống bên phải",
                "§7Sau đó click vào đây để lưu!"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ADMIN_UI_TITLE)) return;

        int slot = event.getSlot();

        // NẾU CLICK VÀO BACKGROUND -> Hủy sự kiện (Không cho lấy kính)
        if (!isInputSlot(slot) && slot != RESULT_SLOT && event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        }

        // XỬ LÝ KHI BẤM NÚT LƯU
        if (slot == SAVE_BTN_SLOT && event.getClickedInventory() == event.getView().getTopInventory()) {
            Player player = (Player) event.getWhoClicked();
            Inventory inv = event.getInventory();

            ItemStack resultItem = inv.getItem(RESULT_SLOT);
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                player.sendMessage("§c(!) Lỗi: Bạn chưa để vật phẩm Kết quả vào ô!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            Map<Integer, ItemStack> ingredients = new HashMap<>();
            boolean hasIngredient = false;

            // Quét 9 ô Input
            for (int i = 0; i < INPUT_SLOTS.length; i++) {
                ItemStack inSlot = inv.getItem(INPUT_SLOTS[i]);
                if (inSlot != null && inSlot.getType() != Material.AIR) {
                    ingredients.put(i, inSlot.clone());
                    hasIngredient = true;
                }
            }

            if (!hasIngredient) {
                player.sendMessage("§c(!) Lỗi: Công thức phải có ít nhất 1 nguyên liệu!");
                return;
            }

            // Sinh ID ngẫu nhiên hoặc nối tiếp (Ví dụ: recipe_1, recipe_2)
            String recipeId = "recipe_" + (recipeManager.getRecipes().size() + 1);
            recipeManager.saveRecipe(recipeId, resultItem, ingredients);

            player.sendMessage("§a§l[!] LƯU THÀNH CÔNG: §fCông thức " + recipeId);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();
        }
    }

    // Đề phòng Admin đóng nhầm UI mà mất đồ (Tránh nuốt đồ của Admin)
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(ADMIN_UI_TITLE)) return;
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Trả lại đồ trong 9 ô Input
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) player.getInventory().addItem(item);
        }
        // Trả lại đồ ở ô Result
        ItemStack resultItem = inv.getItem(RESULT_SLOT);
        if (resultItem != null && resultItem.getType() != Material.AIR) player.getInventory().addItem(resultItem);
    }

    private boolean isInputSlot(int slot) {
        for (int s : INPUT_SLOTS) if (s == slot) return true;
        return false;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}