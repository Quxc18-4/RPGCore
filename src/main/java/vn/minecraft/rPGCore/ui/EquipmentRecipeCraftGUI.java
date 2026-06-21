package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.FixedEquipmentManager;
import vn.minecraft.rPGCore.managers.FixedEquipmentManager.FixedRecipe;

import java.util.*;

public class EquipmentRecipeCraftGUI implements Listener {

    public static final String GUI_NAME = "§6§lBản Vẽ: ";

    private final int[] INGREDIENT_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
    private final int[] RECIPE_SLOTS = {46, 47, 48, 49, 50, 51, 52};
    private final int CONFIRM_SLOT = 16;
    private final int RESULT_SLOT = 34;
    private final int PREV_PAGE_SLOT = 45;
    private final int NEXT_PAGE_SLOT = 53;
    private final int BACK_SLOT = 8; // Nút quay về

    public void openGUI(Player player, String category) {
        player.setMetadata("eq_recipe_session", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), new PlayerSession(category)));
        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME + category);
        renderGUI(player, inv);
        player.openInventory(inv);
    }

    private PlayerSession getSession(Player player) {
        if (player.hasMetadata("eq_recipe_session")) {
            return (PlayerSession) player.getMetadata("eq_recipe_session").get(0).value();
        }
        return null;
    }

    private void renderGUI(Player player, Inventory inv) {
        PlayerSession session = getSession(player);
        if (session == null) return;

        inv.clear();
        List<FixedRecipe> recipes = FixedEquipmentManager.getInstance().getRecipes(session.category);

        ItemStack bgBlack = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack bgRed = createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c[ Ô trống ]");
        ItemStack bgGray = createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7[ Chưa mở khóa ]");

        for (int i = 0; i < 54; i++) inv.setItem(i, bgBlack);

        // NÚT TRỞ VỀ (BACK)
        inv.setItem(BACK_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c§l◀ QUAY LẠI DANH MỤC", "§7Nhấp vào để chọn loại vũ khí/giáp khác."));

        int totalPages = (int) Math.ceil(recipes.size() / 7.0);
        if (totalPages == 0) totalPages = 1;

        if (session.page > 0) inv.setItem(PREV_PAGE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c§l◀ TRANG TRƯỚC", "§7Trang " + session.page));
        if (session.page < totalPages - 1) inv.setItem(NEXT_PAGE_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§lTRANG TIẾP ▶", "§7Trang " + (session.page + 2)));

        int startIndex = session.page * 7;
        for (int i = 0; i < 7; i++) {
            int slot = RECIPE_SLOTS[i];
            int recipeIndex = startIndex + i;

            if (recipeIndex < recipes.size()) {
                ItemStack displayItem = recipes.get(recipeIndex).result.clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(session.selectedRecipe == recipeIndex ? "§a§l▶ ĐANG CHỌN" : "§e▶ Click để xem Nguyên liệu");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inv.setItem(slot, displayItem);
            } else {
                inv.setItem(slot, bgGray);
            }
        }

        if (session.selectedRecipe != -1 && session.selectedRecipe < recipes.size()) {
            FixedRecipe recipe = recipes.get(session.selectedRecipe);
            inv.setItem(RESULT_SLOT, recipe.result);
            inv.setItem(CONFIRM_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§l[✔] TIẾN HÀNH RÈN", "§7Nhấp vào để rèn trang bị này."));

            // [FIX 1]: Tách ảo hiển thị UI để giải quyết giới hạn 64 items/stack
            List<ItemStack> displayIngredients = new ArrayList<>();
            for (ItemStack req : recipe.ingredients) {
                int amountLeft = req.getAmount();
                ItemStack baseClone = req.clone();
                while (amountLeft > 0) {
                    int stackSize = Math.min(amountLeft, 64);
                    ItemStack splitStack = baseClone.clone();
                    splitStack.setAmount(stackSize);
                    displayIngredients.add(splitStack);
                    amountLeft -= stackSize;
                }
            }

            for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
                if (i < displayIngredients.size()) inv.setItem(INGREDIENT_SLOTS[i], displayIngredients.get(i));
                else inv.setItem(INGREDIENT_SLOTS[i], bgRed);
            }
        } else {
            inv.setItem(RESULT_SLOT, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "§9Thành Phẩm", "§7Hãy chọn một bản vẽ ở dưới."));
            inv.setItem(CONFIRM_SLOT, createGuiItem(Material.BARRIER, "§c§lCHƯA CHỌN BẢN VẼ"));
            for (int slot : INGREDIENT_SLOTS) inv.setItem(slot, bgRed);
        }
        player.updateInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_NAME)) return;
        event.setCancelled(true);

        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        PlayerSession session = getSession(player);

        if (session == null || slot >= 54 || slot < 0) return;

        List<FixedRecipe> recipes = FixedEquipmentManager.getInstance().getRecipes(session.category);
        int totalPages = (int) Math.ceil(recipes.size() / 7.0);
        Inventory inv = event.getInventory();

        if (slot == BACK_SLOT) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 1f);
            new EquipmentCategoryGUI().openGUI(player);
        }
        else if (slot == PREV_PAGE_SLOT && session.page > 0) {
            session.page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            renderGUI(player, inv);
        }
        else if (slot == NEXT_PAGE_SLOT && session.page < totalPages - 1) {
            session.page++;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            renderGUI(player, inv);
        }
        else if (isRecipeSlot(slot)) {
            int indexInPage = getRecipeIndexFromSlot(slot);
            int absoluteIndex = (session.page * 7) + indexInPage;
            if (absoluteIndex < recipes.size()) {
                session.selectedRecipe = absoluteIndex;
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                renderGUI(player, inv);
            }
        }
        else if (slot == CONFIRM_SLOT) {
            if (session.selectedRecipe == -1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c§l[!] §cVui lòng chọn một bản vẽ ở thanh bên dưới!");
                return;
            }

            FixedRecipe recipe = recipes.get(session.selectedRecipe);
            boolean canCraft = true;
            List<String> missingList = new ArrayList<>();

            for (ItemStack req : recipe.ingredients) {
                int currentAmount = RPGCore.getInstance().getItemManager().countItem(player, req);
                if (currentAmount < req.getAmount()) {
                    canCraft = false;
                    String itemName = req.hasItemMeta() && req.getItemMeta().hasDisplayName() ? req.getItemMeta().getDisplayName() : req.getType().name();
                    missingList.add(itemName + " §c(" + currentAmount + "/" + req.getAmount() + ")");
                }
            }

            if (canCraft) {
                for (ItemStack req : recipe.ingredients) {
                    RPGCore.getInstance().getItemManager().takeItem(player, req, req.getAmount());
                }
                player.getInventory().addItem(recipe.result.clone());
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage("§6§l[Rèn Trang Bị] §aĐã chế tạo thành công " + recipe.result.getItemMeta().getDisplayName() + "§a!");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c§l[!] §cChế tạo thất bại! Bạn thiếu: §e" + String.join("§c, §e", missingList));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(GUI_NAME)) {
            event.getPlayer().removeMetadata("eq_recipe_session", RPGCore.getInstance());
        }
    }

    private boolean isRecipeSlot(int slot) {
        for (int s : RECIPE_SLOTS) if (s == slot) return true;
        return false;
    }

    private int getRecipeIndexFromSlot(int slot) {
        for (int i = 0; i < RECIPE_SLOTS.length; i++) if (RECIPE_SLOTS[i] == slot) return i;
        return -1;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static class PlayerSession {
        String category;
        int page = 0;
        int selectedRecipe = -1;
        public PlayerSession(String category) { this.category = category; }
    }
}